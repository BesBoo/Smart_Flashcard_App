using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Caching.Memory;
using SmartFlashcardAPI.Data;
using SmartFlashcardAPI.Models.Entities;

namespace SmartFlashcardAPI.Services;

/// <summary>
/// Analyzes words using Gemini AI to detect polysemy, homonyms, and variants.
/// Caches results in memory and persists to DB for reuse.
/// </summary>
public class WordAnalysisService
{
    private readonly GeminiService _gemini;
    private readonly AppDbContext _db;
    private readonly IMemoryCache _cache;
    private readonly ILogger<WordAnalysisService> _logger;

    public WordAnalysisService(
        GeminiService gemini,
        AppDbContext db,
        IMemoryCache cache,
        ILogger<WordAnalysisService> logger)
    {
        _gemini = gemini;
        _db = db;
        _cache = cache;
        _logger = logger;
    }

    /// <summary>
    /// Main analysis pipeline: lemmatize → detect POS → retrieve/generate senses → rank.
    /// </summary>
    public async Task<WordAnalysisResult> AnalyzeWordAsync(string word, string definition, string? context)
    {
        var cacheKey = $"word_analysis:{word.ToLowerInvariant().Trim()}";

        // 1. Check memory cache
        if (_cache.TryGetValue(cacheKey, out WordAnalysisResult? cached) && cached != null)
        {
            _logger.LogInformation("Cache hit for word: {Word}", word);
            return cached;
        }

        var normalizedWord = word.Trim().ToLowerInvariant();

        // 2. Check DB for existing lemma with full data
        var existingResult = await TryLoadFromDbAsync(normalizedWord, definition);
        if (existingResult != null)
        {
            _cache.Set(cacheKey, existingResult, TimeSpan.FromHours(24));
            return existingResult;
        }

        // 3. Call Gemini AI for full analysis
        var aiResult = await CallGeminiAnalysisAsync(normalizedWord, definition, context);
        if (aiResult == null)
        {
            // Fallback: return minimal result
            return new WordAnalysisResult
            {
                Lemma = normalizedWord,
                DetectedPOS = "unknown",
                MainSense = new SenseResult
                {
                    PartOfSpeech = "unknown",
                    DefinitionEn = word,
                    DefinitionVi = definition,
                    Example = context,
                    SimilarityScore = 100
                }
            };
        }

        // 4. Persist to DB (background-safe)
        await PersistToDbAsync(aiResult);

        // 5. Cache and return
        _cache.Set(cacheKey, aiResult, TimeSpan.FromHours(24));
        return aiResult;
    }

    // ══════════════════════════════════════════════════════════
    //  DB LOOKUP
    // ══════════════════════════════════════════════════════════

    private async Task<WordAnalysisResult?> TryLoadFromDbAsync(string word, string userDefinition)
    {
        try
        {
            var lemma = await _db.Lemmas
                .Include(l => l.WordEntries).ThenInclude(e => e.Senses)
                .Include(l => l.Variants)
                .Include(l => l.HomonymGroups)
                .FirstOrDefaultAsync(l => l.LemmaText == word && l.Language == "en");

            if (lemma == null || !lemma.WordEntries.Any())
                return null;

        _logger.LogInformation("DB hit for lemma: {Lemma}", word);

        var allSenses = lemma.WordEntries
            .SelectMany(e => e.Senses.Select(s => new SenseResult
            {
                PartOfSpeech = e.PartOfSpeech,
                DefinitionEn = s.Definition,
                DefinitionVi = s.DefinitionVi,
                Example = s.Example,
                SimilarityScore = ComputeSimpleSimilarity(userDefinition, s.DefinitionVi ?? s.Definition),
                HomonymCluster = lemma.HomonymGroups
                    .FirstOrDefault(h => h.Entries.Any(he => he.Id == e.Id))?.Label
            }))
            .OrderByDescending(s => s.SimilarityScore)
            .ToList();

        var mainSense = allSenses.FirstOrDefault();
        var related = allSenses.Where(s => s.SimilarityScore >= 40 && s != mainSense).ToList();
        var others = allSenses.Where(s => s.SimilarityScore < 40 && s != mainSense).ToList();

        return new WordAnalysisResult
        {
            Lemma = lemma.LemmaText,
            DetectedPOS = mainSense?.PartOfSpeech ?? "unknown",
            MainSense = mainSense!,
            RelatedVariants = related,
            OtherMeanings = others,
            WordVariants = lemma.Variants.Select(v => new VariantResult
            {
                Variant = v.VariantText,
                Type = v.VariantType
            }).ToList()
        };
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "DB lookup failed for word: {Word} (tables may not exist yet)", word);
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════
    //  GEMINI AI ANALYSIS
    // ══════════════════════════════════════════════════════════

    private async Task<WordAnalysisResult?> CallGeminiAnalysisAsync(string word, string definition, string? context)
    {
        var prompt = $@"You are a lexicography expert. Analyze the English word below and return its distinct meanings.

WORD: ""{word}""
USER_DEFINITION: ""{definition}""
CONTEXT: ""{context ?? ""}""

Instructions:
1. Identify the lemma (base form). E.g. ""running"" → ""run"".
2. Detect the part of speech from the context.
3. List up to 8 most important distinct meanings (prioritize the user's intended meaning first, then common ones).
4. For each meaning: provide partOfSpeech, definitionEn, definitionVi (Vietnamese), example sentence.
5. Score each meaning's similarity to USER_DEFINITION (0-100). The user's intended meaning must score highest.
6. Group unrelated meanings with a homonymCluster label. Related meanings share the same cluster.
7. List morphological variants (past_tense, plural, present_participle, etc.).

IMPORTANT: Keep JSON compact. Maximum 8 senses. Short definitions and examples.

Return ONLY valid JSON (no markdown, no explanation):
{{
  ""lemma"": ""string"",
  ""detectedPOS"": ""string"",
  ""senses"": [
    {{
      ""partOfSpeech"": ""noun|verb|adjective|adverb"",
      ""definitionEn"": ""string"",
      ""definitionVi"": ""string"",
      ""example"": ""string"",
      ""similarityScore"": 0-100,
      ""homonymCluster"": ""string label""
    }}
  ],
  ""variants"": [
    {{ ""text"": ""string"", ""type"": ""past_tense|plural|present_participle|past_participle|third_person|comparative|superlative"" }}
  ]
}}";

        try
        {
            var jsonResponse = await _gemini.SendPromptAsync(prompt);
            if (string.IsNullOrWhiteSpace(jsonResponse)) return null;

            // Clean markdown fences if present
            jsonResponse = jsonResponse
                .Replace("```json", "").Replace("```", "").Trim();

            // Attempt to fix truncated JSON (missing closing brackets)
            jsonResponse = TryFixTruncatedJson(jsonResponse);

            var aiOutput = JsonSerializer.Deserialize<GeminiWordOutput>(jsonResponse, new JsonSerializerOptions
            {
                PropertyNameCaseInsensitive = true
            });

            if (aiOutput == null) return null;

            // Transform to result
            var sortedSenses = (aiOutput.Senses ?? new List<GeminiSense>())
                .OrderByDescending(s => s.SimilarityScore)
                .ToList();

            var mainSense = sortedSenses.FirstOrDefault();
            var related = sortedSenses.Where(s => s.SimilarityScore >= 40 && s != mainSense).ToList();
            var others = sortedSenses.Where(s => s.SimilarityScore < 40 && s != mainSense).ToList();

            return new WordAnalysisResult
            {
                Lemma = aiOutput.Lemma ?? word,
                DetectedPOS = aiOutput.DetectedPOS ?? "unknown",
                MainSense = mainSense != null ? ToSenseResult(mainSense) : new SenseResult
                {
                    PartOfSpeech = "unknown", DefinitionEn = word, DefinitionVi = definition,
                    SimilarityScore = 100
                },
                RelatedVariants = related.Select(ToSenseResult).ToList(),
                OtherMeanings = others.Select(ToSenseResult).ToList(),
                WordVariants = (aiOutput.Variants ?? new List<GeminiVariant>())
                    .Select(v => new VariantResult { Variant = v.Text, Type = v.Type }).ToList()
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Gemini word analysis failed for: {Word}", word);
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════
    //  PERSIST TO DB
    // ══════════════════════════════════════════════════════════

    private async Task PersistToDbAsync(WordAnalysisResult result)
    {
        try
        {
            // Check if lemma already exists
            var existing = await _db.Lemmas.FirstOrDefaultAsync(
                l => l.LemmaText == result.Lemma && l.Language == "en");

            if (existing != null) return; // Already persisted

            var lemma = new Lemma
            {
                LemmaText = result.Lemma,
                Language = "en"
            };
            _db.Lemmas.Add(lemma);

            // Create homonym groups from unique cluster labels
            var clusterLabels = result.AllSenses
                .Where(s => !string.IsNullOrEmpty(s.HomonymCluster))
                .Select(s => s.HomonymCluster!)
                .Distinct()
                .ToList();

            var homonymGroups = new Dictionary<string, HomonymGroup>();
            if (clusterLabels.Count > 1)
            {
                foreach (var label in clusterLabels)
                {
                    var group = new HomonymGroup { LemmaId = lemma.Id, Label = label };
                    _db.HomonymGroups.Add(group);
                    homonymGroups[label] = group;
                }
            }

            // Create word entries and senses
            var entriesByKey = new Dictionary<string, WordEntry>();
            int senseOrder = 1;

            foreach (var sense in result.AllSenses)
            {
                var pos = sense.PartOfSpeech ?? "unknown";
                var cluster = sense.HomonymCluster ?? "";
                var entryKey = $"{pos}|{cluster}";

                if (!entriesByKey.TryGetValue(entryKey, out var entry))
                {
                    entry = new WordEntry
                    {
                        LemmaId = lemma.Id,
                        PartOfSpeech = pos,
                        HomonymGroupId = homonymGroups.TryGetValue(cluster, out var hg) ? hg.Id : null
                    };
                    _db.WordEntries.Add(entry);
                    entriesByKey[entryKey] = entry;
                }

                _db.WordSenses.Add(new WordSense
                {
                    WordEntryId = entry.Id,
                    Definition = sense.DefinitionEn ?? "",
                    DefinitionVi = sense.DefinitionVi,
                    Example = sense.Example,
                    SenseOrder = senseOrder++
                });
            }

            // Create variants
            foreach (var v in result.WordVariants ?? new List<VariantResult>())
            {
                _db.WordVariants.Add(new WordVariant
                {
                    LemmaId = lemma.Id,
                    VariantText = v.Variant ?? "",
                    VariantType = v.Type ?? "other"
                });
            }

            await _db.SaveChangesAsync();
            _logger.LogInformation("Persisted lemma to DB: {Lemma} ({SenseCount} senses)",
                result.Lemma, result.AllSenses.Count);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to persist word analysis for: {Lemma}", result.Lemma);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════

    /// <summary>
    /// Attempts to repair JSON that was truncated mid-stream by balancing brackets.
    /// </summary>
    private static string TryFixTruncatedJson(string json)
    {
        int openBraces = 0, openBrackets = 0;
        bool inString = false;
        char prev = '\0';

        foreach (var ch in json)
        {
            if (ch == '"' && prev != '\\') inString = !inString;
            if (!inString)
            {
                if (ch == '{') openBraces++;
                else if (ch == '}') openBraces--;
                else if (ch == '[') openBrackets++;
                else if (ch == ']') openBrackets--;
            }
            prev = ch;
        }

        // If balanced, no fix needed
        if (openBraces == 0 && openBrackets == 0) return json;

        // If truncated inside a string, close the string first
        if (inString) json += "\"";

        // Remove trailing partial object/value (e.g. `"partOfSpeech": "ver`)
        // Find last complete object by looking for last `}`
        var lastBrace = json.LastIndexOf('}');
        var lastBracket = json.LastIndexOf(']');
        var cutPoint = Math.Max(lastBrace, lastBracket);
        if (cutPoint > 0 && cutPoint < json.Length - 1)
        {
            json = json[..(cutPoint + 1)];
        }

        // Re-count and close
        openBraces = 0; openBrackets = 0; inString = false; prev = '\0';
        foreach (var ch in json)
        {
            if (ch == '"' && prev != '\\') inString = !inString;
            if (!inString)
            {
                if (ch == '{') openBraces++;
                else if (ch == '}') openBraces--;
                else if (ch == '[') openBrackets++;
                else if (ch == ']') openBrackets--;
            }
            prev = ch;
        }

        for (int i = 0; i < openBrackets; i++) json += "]";
        for (int i = 0; i < openBraces; i++) json += "}";

        return json;
    }

    private static int ComputeSimpleSimilarity(string a, string b)
    {
        if (string.IsNullOrEmpty(a) || string.IsNullOrEmpty(b)) return 0;
        a = a.ToLowerInvariant().Trim();
        b = b.ToLowerInvariant().Trim();
        if (a == b) return 100;
        if (b.Contains(a) || a.Contains(b)) return 80;

        // Jaccard word overlap
        var wordsA = a.Split(' ', StringSplitOptions.RemoveEmptyEntries).ToHashSet();
        var wordsB = b.Split(' ', StringSplitOptions.RemoveEmptyEntries).ToHashSet();
        var intersection = wordsA.Intersect(wordsB).Count();
        var union = wordsA.Union(wordsB).Count();
        return union > 0 ? (int)(intersection * 100.0 / union) : 0;
    }

    private static SenseResult ToSenseResult(GeminiSense s) => new()
    {
        PartOfSpeech = s.PartOfSpeech,
        DefinitionEn = s.DefinitionEn,
        DefinitionVi = s.DefinitionVi,
        Example = s.Example,
        SimilarityScore = s.SimilarityScore,
        HomonymCluster = s.HomonymCluster
    };

    // ══════════════════════════════════════════════════════════
    //  DTOs
    // ══════════════════════════════════════════════════════════

    // Gemini response models
    private class GeminiWordOutput
    {
        public string? Lemma { get; set; }
        public string? DetectedPOS { get; set; }
        public List<GeminiSense>? Senses { get; set; }
        public List<GeminiVariant>? Variants { get; set; }
    }

    private class GeminiSense
    {
        public string? PartOfSpeech { get; set; }
        public string? DefinitionEn { get; set; }
        public string? DefinitionVi { get; set; }
        public string? Example { get; set; }
        public int SimilarityScore { get; set; }
        public string? HomonymCluster { get; set; }
    }

    private class GeminiVariant
    {
        public string? Text { get; set; }
        public string? Type { get; set; }
    }
}

// ══════════════════════════════════════════════════════════
//  PUBLIC RESULT MODELS
// ══════════════════════════════════════════════════════════

public class WordAnalysisResult
{
    [JsonPropertyName("lemma")]
    public string Lemma { get; set; } = "";

    [JsonPropertyName("detectedPOS")]
    public string DetectedPOS { get; set; } = "";

    [JsonPropertyName("mainSense")]
    public SenseResult MainSense { get; set; } = new();

    [JsonPropertyName("relatedVariants")]
    public List<SenseResult> RelatedVariants { get; set; } = new();

    [JsonPropertyName("otherMeanings")]
    public List<SenseResult> OtherMeanings { get; set; } = new();

    [JsonPropertyName("wordVariants")]
    public List<VariantResult> WordVariants { get; set; } = new();

    [JsonIgnore]
    public List<SenseResult> AllSenses =>
        new[] { MainSense }.Concat(RelatedVariants).Concat(OtherMeanings).ToList();
}

public class SenseResult
{
    [JsonPropertyName("partOfSpeech")]
    public string? PartOfSpeech { get; set; }

    [JsonPropertyName("definitionEn")]
    public string? DefinitionEn { get; set; }

    [JsonPropertyName("definitionVi")]
    public string? DefinitionVi { get; set; }

    [JsonPropertyName("example")]
    public string? Example { get; set; }

    [JsonPropertyName("similarityScore")]
    public int SimilarityScore { get; set; }

    [JsonPropertyName("homonymCluster")]
    public string? HomonymCluster { get; set; }
}

public class VariantResult
{
    [JsonPropertyName("variant")]
    public string? Variant { get; set; }

    [JsonPropertyName("type")]
    public string? Type { get; set; }
}

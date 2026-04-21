using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using SmartFlashcardAPI.Models.DTOs;

namespace SmartFlashcardAPI.Services;

/// <summary>
/// Service that calls the Gemini REST API for all AI features.
/// Reads API key and model name from appsettings.json → AiProvider:Gemini.
/// </summary>
public class GeminiService
{
    private readonly HttpClient _http;
    private readonly string _model;
    private readonly ILogger<GeminiService> _logger;
    private readonly AiUsageLogger _aiUsageLogger;

    // Context for AI usage logging (flows through async call chain)
    private static readonly AsyncLocal<string> _currentPromptType = new();
    private static readonly AsyncLocal<Guid?> _currentUserId = new();

    // External fallback providers (Groq, DeepSeek) — OpenAI-compatible
    private readonly string? _groqApiKey;
    private readonly string _groqModel;
    private readonly string? _deepSeekApiKey;
    private readonly string _deepSeekModel;

    private const string BaseUrl = "https://generativelanguage.googleapis.com/v1beta/models";

    // Fallback models when primary model hits rate limits (ordered by preference)
    // gemini-2.5-flash is primary (works reliably), 2.0 models are rate-limited
    private static readonly string[] FallbackModels = {
        "gemini-2.5-flash-lite",       // 2.5 lite — lightweight fallback
        "gemini-2.5-pro",              // 2.5 pro — high quality fallback
        "gemini-2.0-flash-lite",       // 2.0 — last resort
        "gemini-2.0-flash"             // 2.0 — last resort
    };

    // ── Multi-key rotation ──
    private readonly string[] _apiKeys;
    private static int _currentKeyIndex = 0;
    private static readonly object _keyLock = new();
    // Track when each key was rate-limited (key index → UTC time when 429 was hit)
    private static readonly Dictionary<int, DateTime> _rateLimitedKeys = new();
    private static readonly TimeSpan KeyCooldown = TimeSpan.FromSeconds(60);

    // ── Circuit Breaker: skip Gemini entirely when all keys are exhausted ──
    private static DateTime _geminiCircuitOpenUntil = DateTime.MinValue;
    private static readonly TimeSpan CircuitBreakerDuration = TimeSpan.FromMinutes(10);

    // ── Rate limiting: max 1 concurrent Gemini call, min 6s between calls ──
    private static readonly SemaphoreSlim _rateLimiter = new(1, 1);
    private static DateTime _lastCallTime = DateTime.MinValue;
    private static readonly TimeSpan MinCallInterval = TimeSpan.FromSeconds(6);

    public GeminiService(HttpClient http, IConfiguration config, ILogger<GeminiService> logger, AiUsageLogger aiUsageLogger)
    {
        _http = http;
        _model = config["AiProvider:Gemini:Model"] ?? "gemini-2.5-flash";
        _logger = logger;
        _aiUsageLogger = aiUsageLogger;

        // Load multiple API keys (fallback to single key for backwards compatibility)
        var keys = config.GetSection("AiProvider:Gemini:ApiKeys").Get<string[]>();
        var singleKey = config["AiProvider:Gemini:ApiKey"];

        if (keys != null && keys.Length > 0)
        {
            _apiKeys = keys;
        }
        else if (!string.IsNullOrEmpty(singleKey))
        {
            _apiKeys = new[] { singleKey };
        }
        else
        {
            throw new InvalidOperationException("No Gemini API key configured");
        }

        _logger.LogInformation("Loaded {Count} Gemini API key(s)", _apiKeys.Length);

        // External fallback providers
        _groqApiKey = config["AiProvider:Groq:ApiKey"];
        _groqModel = config["AiProvider:Groq:Model"] ?? "llama-3.3-70b-versatile";
        _deepSeekApiKey = config["AiProvider:DeepSeek:ApiKey"];
        _deepSeekModel = config["AiProvider:DeepSeek:Model"] ?? "deepseek-chat";

        if (!string.IsNullOrEmpty(_groqApiKey))
            _logger.LogInformation("Groq fallback enabled (model: {Model})", _groqModel);
        if (!string.IsNullOrEmpty(_deepSeekApiKey))
            _logger.LogInformation("DeepSeek fallback enabled (model: {Model})", _deepSeekModel);
    }

    /// <summary>Get the next available API key, skipping rate-limited ones.</summary>
    private string GetAvailableApiKey()
    {
        lock (_keyLock)
        {
            var now = DateTime.UtcNow;

            // Try each key starting from current index
            for (int i = 0; i < _apiKeys.Length; i++)
            {
                var idx = (_currentKeyIndex + i) % _apiKeys.Length;

                // Check if this key is still in cooldown
                if (_rateLimitedKeys.TryGetValue(idx, out var limitedAt))
                {
                    if (now - limitedAt < KeyCooldown)
                        continue; // Still cooling down
                    else
                        _rateLimitedKeys.Remove(idx); // Cooldown expired
                }

                _currentKeyIndex = idx;
                return _apiKeys[idx];
            }

            // All keys are rate-limited — use the one with oldest cooldown
            var oldestIdx = _rateLimitedKeys.OrderBy(kv => kv.Value).First().Key;
            _currentKeyIndex = oldestIdx;
            _rateLimitedKeys.Remove(oldestIdx);
            _logger.LogWarning("All API keys rate-limited. Reusing key #{Idx} (oldest cooldown)", oldestIdx);
            return _apiKeys[oldestIdx];
        }
    }

    /// <summary>Mark current API key as rate-limited and rotate to the next one.</summary>
    private void MarkCurrentKeyRateLimited()
    {
        lock (_keyLock)
        {
            _rateLimitedKeys[_currentKeyIndex] = DateTime.UtcNow;
            var maskedKey = _apiKeys[_currentKeyIndex][..8] + "...";
            _logger.LogWarning("API key #{Idx} ({Key}) rate-limited. Rotating to next key...",
                _currentKeyIndex, maskedKey);
            _currentKeyIndex = (_currentKeyIndex + 1) % _apiKeys.Length;
        }
    }

    /// <summary>Wait for rate limiter + enforce minimum interval between API calls.</summary>
    private async Task WaitForRateLimitAsync()
    {
        await _rateLimiter.WaitAsync();
        try
        {
            var elapsed = DateTime.UtcNow - _lastCallTime;
            if (elapsed < MinCallInterval)
            {
                var waitTime = MinCallInterval - elapsed;
                _logger.LogDebug("Rate limiter: waiting {Ms}ms before next Gemini call", waitTime.TotalMilliseconds);
                await Task.Delay(waitTime);
            }
        }
        finally
        {
            _lastCallTime = DateTime.UtcNow;
            _rateLimiter.Release();
        }
    }

    // ══════════════════════════════════════════════════════════
    //  GENERATE FLASHCARDS FROM TEXT
    // ══════════════════════════════════════════════════════════

    public async Task<List<DraftCard>> GenerateFlashcardsFromTextAsync(string text, string language, int maxCards, Guid? userId = null)
    {
        _currentPromptType.Value = "GenerateFlashcards";
        _currentUserId.Value = userId;
        var prompt = $@"You are a smart flashcard generator. Create up to {maxCards} flashcards from the following text.

RULES:
1. Detect the language of the input text automatically.
2. frontText should be a key word, phrase, or concept from the text.
3. backText should be its definition or translation.
4. exampleText MUST be an example sentence written in the SAME language as the frontText.
   - If frontText is in English, exampleText must be in English.
   - If frontText is in Chinese, exampleText must be in Chinese.
   - If frontText is in Japanese, exampleText must be in Japanese.
   - If frontText is in Vietnamese, exampleText must be in Vietnamese.
5. DO NOT translate exampleText to Vietnamese unless the frontText itself is Vietnamese.

Input text:
---
{text}
---

Return a JSON array (NO markdown, NO explanation, ONLY JSON):
[
  {{""frontText"": ""word or phrase"", ""backText"": ""meaning or definition"", ""exampleText"": ""example sentence in same language as frontText""}}
]";

        var json = await CallGeminiAsync(prompt);
        return ParseFlashcards(json);
    }

    // ══════════════════════════════════════════════════════════
    //  EXTRACT VOCABULARY FROM TEXT PASSAGE
    // ══════════════════════════════════════════════════════════

    public async Task<List<DraftCard>> ExtractVocabularyFromTextAsync(string text, string targetLanguage, int maxWords, Guid? userId = null)
    {
        _currentPromptType.Value = "ExtractVocab";
        _currentUserId.Value = userId;
        var targetLangName = targetLanguage switch
        {
            "vi" => "Vietnamese",
            "en" => "English",
            "zh" => "Chinese",
            "ja" => "Japanese",
            "ko" => "Korean",
            "fr" => "French",
            _ => "Vietnamese"
        };

        var prompt = $@"You are a vocabulary extraction expert. Read the following text passage carefully and extract important, difficult, or noteworthy vocabulary words.

For each word you extract:
- frontText: the original word or phrase EXACTLY as it appears in the text
- backText: {targetLangName} translation + part of speech in parentheses. Format: ""(n/v/adj/adv) meaning""
- exampleText: copy the ORIGINAL sentence from the text that contains this word. Do NOT create a new sentence — use the exact sentence from the passage.

RULES:
1. Extract up to {maxWords} vocabulary words.
2. Only extract meaningful vocabulary: nouns, verbs, adjectives, adverbs, phrases, idioms.
3. SKIP very common/basic words like: the, a, an, is, are, was, were, have, has, do, does, in, on, at, to, for, and, or, but, with, this, that, it, I, you, he, she, we, they.
4. Prioritize: uncommon words, academic vocabulary, technical terms, idioms, phrasal verbs.
5. frontText must be in the ORIGINAL language of the text.
6. backText must be the translation/definition in {targetLangName}.
7. exampleText must be the actual sentence from the passage (same language as the passage).
8. If a word appears multiple times, pick the most illustrative sentence.

Text passage:
---
{text}
---

Return a JSON array (NO markdown, NO explanation, ONLY valid JSON):
[
  {{""frontText"": ""vocabulary word"", ""backText"": ""(part of speech) {targetLangName} meaning"", ""exampleText"": ""original sentence from text containing this word""}}
]";

        var json = await CallGeminiAsync(prompt);
        return ParseFlashcards(json);
    }

    // ══════════════════════════════════════════════════════════
    //  GENERATE EXAMPLE
    // ══════════════════════════════════════════════════════════

    public async Task<string> GenerateExampleAsync(string frontText, string backText, string language, Guid? userId = null)
    {
        _currentPromptType.Value = "GenerateExample";
        _currentUserId.Value = userId;
        var prompt = $@"Create ONE example sentence that uses the word/phrase from the front text.

- Front text: {frontText}
- Back text (meaning): {backText}

IMPORTANT RULES:
- The example sentence MUST be in the SAME language as the front text.
- If front text is English, write the example in English.
- If front text is Chinese, write the example in Chinese.
- If front text is Japanese, write the example in Japanese.
- If front text is Vietnamese, write the example in Vietnamese.
- Return ONLY the example sentence, nothing else. No explanation, no translation.";

        return (await CallGeminiAsync(prompt)).Trim().Trim('"');
    }

    // ══════════════════════════════════════════════════════════
    //  GENERATE IMAGE — Gemini AI image generation
    // ══════════════════════════════════════════════════════════

    // Correct model name for image generation (verified via API)
    private const string ImageModel = "gemini-2.5-flash-image";

    public async Task<string?> GenerateImageUrlAsync(string frontText, string backText, string baseUrl, Guid? userId = null)
    {
        _currentPromptType.Value = "GenerateImage";
        _currentUserId.Value = userId;
        _logger.LogInformation("Generating image for: '{Front}' (back: '{Back}')", frontText, backText);

        // Try Gemini image generation (primary — user wants AI-generated images)
        var (imageBytes, mimeType) = await GenerateImageWithGeminiAsync(frontText, backText);
        
        if (imageBytes != null && imageBytes.Length > 1000)
        {
            // Save to wwwroot/images/ and return accessible URL
            var ext = mimeType.Contains("jpeg") || mimeType.Contains("jpg") ? "jpg" : "png";
            var fileName = $"{Guid.NewGuid()}.{ext}";
            var imagesDir = Path.Combine(Directory.GetCurrentDirectory(), "wwwroot", "images");
            Directory.CreateDirectory(imagesDir);
            var filePath = Path.Combine(imagesDir, fileName);
            await File.WriteAllBytesAsync(filePath, imageBytes);
            _logger.LogInformation("Image saved: {File} ({Size} bytes, {Mime})", fileName, imageBytes.Length, mimeType);
            return $"{baseUrl}/images/{fileName}";
        }

        _logger.LogWarning("Gemini image generation failed for '{Front}'", frontText);
        return null;
    }

    /// <summary>
    /// Generate image using Gemini's native image generation.
    /// Uses gemini-2.5-flash-image model with responseModalities: ["IMAGE","TEXT"].
    /// Includes retry logic for 429 rate limits.
    /// </summary>
    private async Task<(byte[]?, string)> GenerateImageWithGeminiAsync(string frontText, string backText)
    {
        var prompt = $"Generate a high-quality, vivid, colorful image that visually represents the concept: \"{frontText}\"" +
                     (string.IsNullOrWhiteSpace(backText) || backText == frontText 
                         ? "." 
                         : $" (meaning: {backText}).") +
                     " The image should be a realistic photo or detailed illustration. " +
                     "Do NOT include any text, labels, or watermarks in the image.";

        var url = $"{BaseUrl}/{ImageModel}:generateContent?key={GetAvailableApiKey()}";
        var requestBody = new
        {
            contents = new[] { new { parts = new[] { new { text = prompt } } } },
            generationConfig = new
            {
                responseModalities = new[] { "IMAGE", "TEXT" }
            }
        };

        // Try up to 3 times with exponential backoff for rate limits
        for (int attempt = 0; attempt < 3; attempt++)
        {
            try
            {
                await WaitForRateLimitAsync();
                var content = new StringContent(
                    JsonSerializer.Serialize(requestBody), Encoding.UTF8, "application/json");
                
                _logger.LogInformation("Calling {Model} (attempt {Attempt})...", ImageModel, attempt + 1);
                var response = await _http.PostAsync(url, content);
                var responseJson = await response.Content.ReadAsStringAsync();

                if ((int)response.StatusCode == 429)
                {
                    // Parse retry delay from response if available
                    var retryDelay = 15 + (attempt * 15); // 15s, 30s, 45s default
                    try
                    {
                        using var errDoc = JsonDocument.Parse(responseJson);
                        // Try to extract retryDelay from response
                        if (responseJson.Contains("retryDelay"))
                        {
                            var match = System.Text.RegularExpressions.Regex.Match(responseJson, @"""retryDelay"":\s*""(\d+)s""");
                            if (match.Success && int.TryParse(match.Groups[1].Value, out var parsedDelay))
                                retryDelay = parsedDelay + 2; // Add 2s buffer
                        }
                    }
                    catch { /* use default delay */ }

                    _logger.LogWarning("Rate limited on {Model}. Waiting {Delay}s before retry...", ImageModel, retryDelay);
                    await Task.Delay(TimeSpan.FromSeconds(retryDelay));
                    continue;
                }

                if (!response.IsSuccessStatusCode)
                {
                    _logger.LogWarning("Gemini image API returned {Status}: {Body}",
                        response.StatusCode, responseJson.Length > 300 ? responseJson[..300] : responseJson);
                    return (null, "image/png");
                }

                // Parse response — look for inlineData with image bytes
                using var doc = JsonDocument.Parse(responseJson);
                if (doc.RootElement.TryGetProperty("candidates", out var candidates) &&
                    candidates.GetArrayLength() > 0)
                {
                    var parts = candidates[0].GetProperty("content").GetProperty("parts");
                    foreach (var part in parts.EnumerateArray())
                    {
                        if (part.TryGetProperty("inlineData", out var inlineData))
                        {
                            var mime = inlineData.TryGetProperty("mimeType", out var mt)
                                ? mt.GetString() ?? "image/png"
                                : "image/png";
                            var base64 = inlineData.GetProperty("data").GetString();
                            if (!string.IsNullOrEmpty(base64))
                            {
                                var bytes = Convert.FromBase64String(base64);
                                _logger.LogInformation("Gemini returned image: {Size} bytes, {Mime}", bytes.Length, mime);
                                return (bytes, mime);
                            }
                        }
                    }
                }

                _logger.LogWarning("Gemini response had no image data");
                return (null, "image/png");
            }
            catch (Exception ex)
            {
                _logger.LogWarning("Gemini image error (attempt {Attempt}): {Msg}", attempt + 1, ex.Message);
                if (attempt < 2) await Task.Delay(TimeSpan.FromSeconds(5));
            }
        }

        return (null, "image/png");
    }

    // ══════════════════════════════════════════════════════════
    //  GENERATE QUIZ
    // ══════════════════════════════════════════════════════════

    public async Task<List<QuizQuestion>> GenerateQuizAsync(List<QuizCardInput> cards, int questionCount, string language, Guid? userId = null)
    {
        _currentPromptType.Value = "Quiz";
        _currentUserId.Value = userId;
        var cardList = string.Join("\n", cards.Select((c, i) => $"{i + 1}. Front: {c.FrontText} | Back: {c.BackText} | Id: {c.Id}"));

        var prompt = $@"Dựa trên danh sách flashcard sau, hãy tạo {questionCount} câu hỏi trắc nghiệm (4 đáp án, 1 đáp án đúng).

Danh sách flashcard:
{cardList}

Ngôn ngữ: {language}

Trả về JSON array (KHÔNG markdown, CHỈ JSON):
[
  {{""questionText"": ""nội dung câu hỏi"", ""options"": [""A"", ""B"", ""C"", ""D""], ""correctIndex"": 0, ""sourceCardId"": ""guid-of-card""}}
]";

        var json = await CallGeminiAsync(prompt);
        return ParseQuizQuestions(json);
    }

    // ══════════════════════════════════════════════════════════
    //  AI TUTOR CHAT
    // ══════════════════════════════════════════════════════════

    public async Task<string> TutorChatAsync(string message, string language, List<ChatMessage>? history, Guid? userId = null)
    {
        _currentPromptType.Value = "TutorChat";
        _currentUserId.Value = userId;
        var systemPrompt = $@"Bạn là AI Tutor — trợ lý học tập thông minh cho ứng dụng flashcard. 
Ngôn ngữ giao tiếp: {language}.
Nhiệm vụ:
- Giải thích từ vựng, khái niệm
- Cho ví dụ câu, cách sử dụng
- Gợi ý cách ghi nhớ (mnemonics)
- Giải đáp thắc mắc liên quan đến học tập
Trả lời ngắn gọn, dễ hiểu, thân thiện. Sử dụng emoji phù hợp.";

        // Build conversation contents for Gemini
        var contents = new List<object>();

        // Add history if available
        if (history != null)
        {
            foreach (var msg in history)
            {
                contents.Add(new
                {
                    role = msg.Role == "user" ? "user" : "model",
                    parts = new[] { new { text = msg.Content } }
                });
            }
        }

        // Add current user message
        contents.Add(new
        {
            role = "user",
            parts = new[] { new { text = message } }
        });

        var requestBody = new
        {
            system_instruction = new { parts = new[] { new { text = systemPrompt } } },
            contents,
            generationConfig = new
            {
                temperature = 0.7,
                maxOutputTokens = 1024
            }
        };

        // Build list of models to try
        var modelsToTry = new List<string> { _model };
        foreach (var fb in FallbackModels)
        {
            if (!modelsToTry.Contains(fb)) modelsToTry.Add(fb);
        }

        // Try each model × each key combination
        foreach (var model in modelsToTry)
        {
            for (int keyAttempt = 0; keyAttempt < _apiKeys.Length; keyAttempt++)
            {
                var apiKey = GetAvailableApiKey();
                var url = $"{BaseUrl}/{model}:generateContent?key={apiKey}";
                var result = await SendWithRetryAsync(url, requestBody, model, maxRetries: 1);
                if (result != null) return result;
            }
        }

        // All Gemini models exhausted — try external fallback for chat
        var chatPrompt = systemPrompt + "\n\nUser: " + message;
        var externalResult = await TryExternalFallbackAsync(chatPrompt);
        if (externalResult != null) return externalResult;

        throw new InvalidOperationException("All AI providers are rate-limited. Please try again later.");
    }

    // ══════════════════════════════════════════════════════════
    //  ADAPTIVE HINT
    // ══════════════════════════════════════════════════════════

    public async Task<AdaptiveHint> GetAdaptiveHintAsync(FlashcardInfo flashcard, string language, Guid? userId = null)
    {
        _currentPromptType.Value = "AdaptiveHint";
        _currentUserId.Value = userId;
        var prompt = $@"Người dùng đang gặp khó khăn với flashcard sau (đã sai {flashcard.FailCount} lần):
- Mặt trước: {flashcard.FrontText}
- Mặt sau: {flashcard.BackText}
{(flashcard.ExampleText != null ? $"- Ví dụ: {flashcard.ExampleText}" : "")}

Ngôn ngữ: {language}

Hãy trả về JSON (KHÔNG markdown, CHỈ JSON):
{{
  ""simplifiedExplanation"": ""giải thích đơn giản, dễ nhớ"",
  ""additionalExamples"": [""ví dụ 1"", ""ví dụ 2""],
  ""splitSuggestion"": {(flashcard.FailCount >= 5 ? @"{ ""suggested"": true, ""cards"": [{""frontText"": ""phần 1"", ""backText"": ""trả lời 1"", ""exampleText"": null}, {""frontText"": ""phần 2"", ""backText"": ""trả lời 2"", ""exampleText"": null}] }" : "null")}
}}";

        var json = await CallGeminiAsync(prompt);
        return ParseAdaptiveHint(json);
    }

    // ══════════════════════════════════════════════════════════
    //  CORE: Call Gemini API
    // ══════════════════════════════════════════════════════════

    /// &lt;summary&gt;Send a raw prompt to Gemini and return the text response.&lt;/summary&gt;
    public async Task<string> SendPromptAsync(string prompt) => await CallGeminiAsync(prompt);

    private async Task<string> CallGeminiAsync(string prompt)
    {
        var sw = System.Diagnostics.Stopwatch.StartNew();
        var promptType = _currentPromptType.Value ?? "Unknown";
        var userId = _currentUserId.Value;

        var requestBody = new
        {
            contents = new[]
            {
                new
                {
                    parts = new[] { new { text = prompt } }
                }
            },
            generationConfig = new
            {
                temperature = 0.4,
                maxOutputTokens = 8192,
                responseMimeType = "application/json"
            }
        };

        // ══════════════════════════════════════════════════════════
        //  STEP 1: Try Gemini 2.5 Flash (primary model)
        // ══════════════════════════════════════════════════════════
        if (DateTime.UtcNow >= _geminiCircuitOpenUntil)
        {
            var apiKey = GetAvailableApiKey();
            var url = $"{BaseUrl}/{_model}:generateContent?key={apiKey}";
            var result = await SendWithRetryAsync(url, requestBody, _model, maxRetries: 0);
            if (result != null)
            {
                sw.Stop();
                _aiUsageLogger.Log(userId, promptType, _model, "Gemini",
                    result.Length / 4, "Success", sw.ElapsedMilliseconds);
                return result;
            }
            _logger.LogWarning("Gemini {Model} failed — trying Groq...", _model);
        }
        else
        {
            _logger.LogWarning("Gemini circuit breaker OPEN — skipping to Groq");
        }

        // ══════════════════════════════════════════════════════════
        //  STEP 2: Try Groq (fastest external fallback)
        // ══════════════════════════════════════════════════════════
        if (!string.IsNullOrEmpty(_groqApiKey) && DateTime.UtcNow >= _groqCircuitOpenUntil)
        {
            var groqResult = await CallOpenAiCompatibleAsync(
                "https://api.groq.com/openai/v1/chat/completions",
                _groqApiKey, _groqModel, prompt, "Groq");
            if (groqResult != null)
            {
                sw.Stop();
                return groqResult;
            }
            _logger.LogWarning("Groq failed — trying DeepSeek...");
        }
        else if (DateTime.UtcNow < _groqCircuitOpenUntil)
        {
            _logger.LogWarning("Groq circuit breaker OPEN — skipping to DeepSeek");
        }

        // ══════════════════════════════════════════════════════════
        //  STEP 3: Try DeepSeek
        // ══════════════════════════════════════════════════════════
        if (!string.IsNullOrEmpty(_deepSeekApiKey) && DateTime.UtcNow >= _deepSeekCircuitOpenUntil)
        {
            var dsResult = await CallOpenAiCompatibleAsync(
                "https://api.deepseek.com/chat/completions",
                _deepSeekApiKey, _deepSeekModel, prompt, "DeepSeek");
            if (dsResult != null)
            {
                sw.Stop();
                return dsResult;
            }
            _logger.LogWarning("DeepSeek failed — trying remaining Gemini models...");
        }
        else if (DateTime.UtcNow < _deepSeekCircuitOpenUntil)
        {
            _logger.LogWarning("DeepSeek circuit breaker OPEN — trying remaining Gemini models");
        }

        // ══════════════════════════════════════════════════════════
        //  STEP 4: Last resort — try remaining Gemini models
        // ══════════════════════════════════════════════════════════
        if (DateTime.UtcNow >= _geminiCircuitOpenUntil)
        {
            foreach (var fallbackModel in FallbackModels)
            {
                var apiKey = GetAvailableApiKey();
                var url = $"{BaseUrl}/{fallbackModel}:generateContent?key={apiKey}";
                var result = await SendWithRetryAsync(url, requestBody, fallbackModel, maxRetries: 0);
                if (result != null)
                {
                    sw.Stop();
                    _aiUsageLogger.Log(userId, promptType, fallbackModel, "Gemini",
                        result.Length / 4, "Success", sw.ElapsedMilliseconds);
                    return result;
                }
            }

            // All Gemini models also failed — open circuit breaker
            _geminiCircuitOpenUntil = DateTime.UtcNow + CircuitBreakerDuration;
            _logger.LogWarning("All Gemini models exhausted. Circuit breaker OPENED for {Min} min",
                CircuitBreakerDuration.TotalMinutes);
        }

        // ══════════════════════════════════════════════════════════
        //  ALL PROVIDERS EXHAUSTED
        // ══════════════════════════════════════════════════════════
        sw.Stop();
        _aiUsageLogger.Log(userId, promptType, _model, "Gemini",
            0, "RateLimited", sw.ElapsedMilliseconds, "All providers exhausted");

        throw new InvalidOperationException("All AI providers are rate-limited. Please try again later.");
    }

    /// <summary>
    /// Try Groq and DeepSeek as fallback when Gemini is rate-limited.
    /// Each provider has its own circuit breaker to avoid wasting requests.
    /// </summary>
    private static DateTime _groqCircuitOpenUntil = DateTime.MinValue;
    private static DateTime _deepSeekCircuitOpenUntil = DateTime.MinValue;

    private async Task<string?> TryExternalFallbackAsync(string prompt)
    {
        // Try Groq first (fastest inference)
        if (!string.IsNullOrEmpty(_groqApiKey) && DateTime.UtcNow >= _groqCircuitOpenUntil)
        {
            var result = await CallOpenAiCompatibleAsync(
                "https://api.groq.com/openai/v1/chat/completions",
                _groqApiKey, _groqModel, prompt, "Groq");
            if (result != null) return result;
        }
        else if (DateTime.UtcNow < _groqCircuitOpenUntil)
        {
            _logger.LogWarning("Groq circuit breaker OPEN — skipping (resets at {Reset:HH:mm:ss})", _groqCircuitOpenUntil);
        }

        // Try DeepSeek
        if (!string.IsNullOrEmpty(_deepSeekApiKey) && DateTime.UtcNow >= _deepSeekCircuitOpenUntil)
        {
            var result = await CallOpenAiCompatibleAsync(
                "https://api.deepseek.com/chat/completions",
                _deepSeekApiKey, _deepSeekModel, prompt, "DeepSeek");
            if (result != null) return result;
        }
        else if (DateTime.UtcNow < _deepSeekCircuitOpenUntil)
        {
            _logger.LogWarning("DeepSeek circuit breaker OPEN — skipping (resets at {Reset:HH:mm:ss})", _deepSeekCircuitOpenUntil);
        }

        return null;
    }

    /// <summary>
    /// Call an OpenAI-compatible API (Groq, DeepSeek, etc.)
    /// Opens provider-specific circuit breaker on 429/402 errors.
    /// </summary>
    private async Task<string?> CallOpenAiCompatibleAsync(
        string endpoint, string apiKey, string model, string prompt, string providerName)
    {
        try
        {
            _logger.LogInformation("Trying {Provider} fallback (model: {Model})...", providerName, model);

            var requestBody = new
            {
                model = model,
                messages = new[]
                {
                    new { role = "user", content = prompt }
                },
                temperature = 0.4,
                max_tokens = 8192
            };

            var request = new HttpRequestMessage(HttpMethod.Post, endpoint);
            request.Headers.Add("Authorization", $"Bearer {apiKey}");
            request.Content = new StringContent(
                JsonSerializer.Serialize(requestBody), Encoding.UTF8, "application/json");

            var response = await _http.SendAsync(request);
            var responseJson = await response.Content.ReadAsStringAsync();

            if (response.IsSuccessStatusCode)
            {
                using var doc = JsonDocument.Parse(responseJson);
                var content = doc.RootElement
                    .GetProperty("choices")[0]
                    .GetProperty("message")
                    .GetProperty("content")
                    .GetString() ?? "";
                _logger.LogInformation("{Provider} returned {Len} chars successfully", providerName, content.Length);
                _aiUsageLogger.Log(_currentUserId.Value, _currentPromptType.Value ?? "Unknown",
                    model, providerName, content.Length / 4, "Success", 0);
                return content;
            }

            var statusCode = (int)response.StatusCode;
            _logger.LogWarning("{Provider} API error: {Status} - {Body}",
                providerName, response.StatusCode,
                responseJson.Length > 200 ? responseJson[..200] : responseJson);

            // Open circuit breaker for this provider on rate limit or payment errors
            if (statusCode == 429 || statusCode == 402)
            {
                var cooldown = statusCode == 402 
                    ? TimeSpan.FromHours(1)    // Payment issue — long cooldown
                    : TimeSpan.FromMinutes(5); // Rate limit — shorter cooldown

                if (providerName == "Groq") _groqCircuitOpenUntil = DateTime.UtcNow + cooldown;
                if (providerName == "DeepSeek") _deepSeekCircuitOpenUntil = DateTime.UtcNow + cooldown;

                _logger.LogWarning("{Provider} circuit breaker OPENED for {Min} min (status: {Status})",
                    providerName, cooldown.TotalMinutes, statusCode);
            }

            return null;
        }
        catch (Exception ex)
        {
            _logger.LogWarning("{Provider} fallback failed: {Msg}", providerName, ex.Message);
            return null;
        }
    }

    /// <summary>
    /// Sends request to Gemini with retry on 429.
    /// Returns null if quota is exceeded (so caller can try next model/key).
    /// </summary>
    private async Task<string?> SendWithRetryAsync(string url, object requestBody, string model, int maxRetries = 2)
    {
        for (int attempt = 0; attempt <= maxRetries; attempt++)
        {
            await WaitForRateLimitAsync();
            var content = new StringContent(JsonSerializer.Serialize(requestBody), Encoding.UTF8, "application/json");
            var response = await _http.PostAsync(url, content);
            var responseJson = await response.Content.ReadAsStringAsync();

            if (response.IsSuccessStatusCode)
                return ExtractTextFromResponse(responseJson);

            if ((int)response.StatusCode == 429)
            {
                // Mark this key as rate-limited so we rotate to next
                MarkCurrentKeyRateLimited();

                // Per-minute rate limit — retry with backoff
                if (attempt < maxRetries)
                {
                    var delay = 10 + (attempt * 15); // 10s, 25s
                    _logger.LogWarning("Gemini 429 on {Model}. Waiting {Delay}s (attempt {Att}/{Max})...",
                        model, delay, attempt + 1, maxRetries);
                    await Task.Delay(TimeSpan.FromSeconds(delay));
                    continue;
                }

                // Max retries exceeded for this model+key
                _logger.LogWarning("Model {Model} rate limit retries exhausted. Trying fallback...", model);
                return null;
            }

            _logger.LogWarning("Gemini API error on {Model}: {Status}. Trying next model/key...", model, response.StatusCode);
            return null; // Return null so fallback loop continues
        }

        return null;
    }

    // ══════════════════════════════════════════════════════════
    //  RESPONSE PARSING
    // ══════════════════════════════════════════════════════════

    private static string ExtractTextFromResponse(string responseJson)
    {
        using var doc = JsonDocument.Parse(responseJson);
        var candidates = doc.RootElement.GetProperty("candidates");
        var firstCandidate = candidates[0];
        var parts = firstCandidate.GetProperty("content").GetProperty("parts");
        return parts[0].GetProperty("text").GetString() ?? "";
    }

    /// <summary>Strip markdown code fences if present, then parse JSON.</summary>
    private static string CleanJsonResponse(string text)
    {
        text = text.Trim();
        // Remove ```json ... ``` wrapping
        if (text.StartsWith("```"))
        {
            var firstNewline = text.IndexOf('\n');
            if (firstNewline > 0) text = text[(firstNewline + 1)..];
            if (text.EndsWith("```")) text = text[..^3];
            text = text.Trim();
        }
        return text;
    }

    private static List<DraftCard> ParseFlashcards(string text)
    {
        try
        {
            var clean = CleanJsonResponse(text);
            var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
            return JsonSerializer.Deserialize<List<DraftCard>>(clean, options) ?? new List<DraftCard>();
        }
        catch
        {
            // Try to repair truncated JSON array (e.g. missing closing ])
            try
            {
                var clean = CleanJsonResponse(text);
                // Find last complete object (ends with })
                var lastBrace = clean.LastIndexOf('}');
                if (lastBrace > 0)
                {
                    var repaired = clean[..(lastBrace + 1)] + "]";
                    var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                    var result = JsonSerializer.Deserialize<List<DraftCard>>(repaired, options);
                    if (result != null && result.Count > 0) return result;
                }
            }
            catch { /* ignore repair failure */ }

            return new List<DraftCard>();
        }
    }

    private static List<QuizQuestion> ParseQuizQuestions(string text)
    {
        try
        {
            var clean = CleanJsonResponse(text);
            var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
            return JsonSerializer.Deserialize<List<QuizQuestion>>(clean, options) ?? new List<QuizQuestion>();
        }
        catch
        {
            return new List<QuizQuestion>();
        }
    }

    private static AdaptiveHint ParseAdaptiveHint(string text)
    {
        try
        {
            var clean = CleanJsonResponse(text);
            var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
            var hint = JsonSerializer.Deserialize<AdaptiveHint>(clean, options);
            return hint ?? new AdaptiveHint("Hãy thử chia nhỏ nội dung để dễ nhớ hơn.", new List<string>(), null);
        }
        catch
        {
            return new AdaptiveHint("Hãy thử chia nhỏ nội dung để dễ nhớ hơn.", new List<string>(), null);
        }
    }
}

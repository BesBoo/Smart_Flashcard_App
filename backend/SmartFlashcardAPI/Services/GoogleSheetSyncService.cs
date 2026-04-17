using Microsoft.EntityFrameworkCore;
using SmartFlashcardAPI.Data;
using SmartFlashcardAPI.Models.Entities;

namespace SmartFlashcardAPI.Services;

/// <summary>
/// Syncs flashcards from a published Google Sheet (CSV format).
/// Adds NEW rows and UPDATES existing ones (matched by front text).
/// </summary>
public class GoogleSheetSyncService
{
    private readonly AppDbContext _db;
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly ILogger<GoogleSheetSyncService> _logger;

    public GoogleSheetSyncService(
        AppDbContext db,
        IHttpClientFactory httpClientFactory,
        ILogger<GoogleSheetSyncService> logger)
    {
        _db = db;
        _httpClientFactory = httpClientFactory;
        _logger = logger;
    }

    /// <summary>
    /// Link a Google Sheet URL to a deck. Converts share URLs to published CSV format.
    /// </summary>
    public async Task<string> LinkSheetAsync(Guid userId, Guid deckId, string sheetUrl)
    {
        var deck = await _db.Decks.FirstOrDefaultAsync(
            d => d.Id == deckId && d.UserId == userId && !d.IsDeleted)
            ?? throw new KeyNotFoundException("Deck not found");

        var csvUrl = ConvertToCsvUrl(sheetUrl);

        deck.GoogleSheetUrl = csvUrl;
        deck.UpdatedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync();

        _logger.LogInformation("Linked Google Sheet to deck {DeckId}: {Url}", deckId, csvUrl);
        return csvUrl;
    }

    /// <summary>
    /// Unlink a Google Sheet from a deck.
    /// </summary>
    public async Task UnlinkSheetAsync(Guid userId, Guid deckId)
    {
        var deck = await _db.Decks.FirstOrDefaultAsync(
            d => d.Id == deckId && d.UserId == userId && !d.IsDeleted)
            ?? throw new KeyNotFoundException("Deck not found");

        deck.GoogleSheetUrl = null;
        deck.UpdatedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync();
    }

    /// <summary>
    /// Sync: fetch CSV from Google Sheet, add new cards, update existing ones.
    /// Returns (added, updated, total) counts.
    /// </summary>
    public async Task<(int Added, int Skipped, int TotalRows)> SyncAsync(Guid userId, Guid deckId)
    {
        var deck = await _db.Decks.FirstOrDefaultAsync(
            d => d.Id == deckId && d.UserId == userId && !d.IsDeleted)
            ?? throw new KeyNotFoundException("Deck not found");

        if (string.IsNullOrEmpty(deck.GoogleSheetUrl))
            throw new InvalidOperationException("CONFLICT:No Google Sheet linked to this deck");

        // 1. Fetch CSV (handles multi-line cells correctly)
        var rows = await FetchCsvAsync(deck.GoogleSheetUrl);
        if (rows.Count == 0)
            return (0, 0, 0);

        // 2. Get ALL existing cards for this deck (for dedup)
        var existingCards = await _db.Flashcards
            .Where(f => f.DeckId == deckId && !f.IsDeleted)
            .ToListAsync();

        // Build lookup: normalized "front|back" → card entity
        // Only exact matches (same front AND back text) are considered duplicates
        var existingByKey = new HashSet<string>();
        foreach (var card in existingCards)
        {
            var key = NormalizeKey(card.FrontText, card.BackText);
            if (!string.IsNullOrEmpty(key))
                existingByKey.Add(key);
        }

        _logger.LogInformation(
            "Sheet sync: {RowCount} rows from CSV, {ExistingCount} existing cards in deck",
            rows.Count, existingCards.Count);

        // 3. Process rows: add new cards, skip exact duplicates
        int added = 0, skipped = 0;
        foreach (var row in rows)
        {
            var front = (row.FrontText ?? "").Trim();
            var back = (row.BackText ?? "").Trim();
            var example = string.IsNullOrWhiteSpace(row.Example) ? null : row.Example.Trim();

            if (string.IsNullOrWhiteSpace(front) && string.IsNullOrWhiteSpace(back))
                continue;

            var fullKey = NormalizeKey(front, back);

            if (!string.IsNullOrEmpty(fullKey) && existingByKey.Contains(fullKey))
            {
                // Exact duplicate — already exists, skip
                skipped++;
                continue;
            }

            // ADD new card
            var newCard = new Flashcard
            {
                UserId = userId,
                DeckId = deckId,
                FrontText = front,
                BackText = back,
                ExampleText = example,
                NextReviewDate = DateTime.UtcNow
            };
            _db.Flashcards.Add(newCard);

            // Track to prevent duplicates within the same CSV batch
            if (!string.IsNullOrEmpty(fullKey))
                existingByKey.Add(fullKey);

            added++;
        }

        if (added > 0)
            await _db.SaveChangesAsync();

        _logger.LogInformation(
            "Sheet sync for deck {DeckId}: {Added} added, {Skipped} skipped (already exist), {Total} total rows",
            deckId, added, skipped, rows.Count);

        return (added, skipped, rows.Count);
    }

    private static string NormalizeFront(string front) =>
        front.Trim().ToLowerInvariant().Replace("\r", "").Replace("\n", " ");

    private static string NormalizeKey(string front, string back) =>
        NormalizeFront(front) + "|" + (back ?? "").Trim().ToLowerInvariant();

    // ══════════════════════════════════════════════════════════
    //  CSV FETCHER + PARSER (handles multi-line cells)
    // ══════════════════════════════════════════════════════════

    private async Task<List<SheetRow>> FetchCsvAsync(string csvUrl)
    {
        var client = _httpClientFactory.CreateClient();
        client.Timeout = TimeSpan.FromSeconds(30);

        // Try primary URL first
        var csvText = await TryFetchCsvText(client, csvUrl);

        // If primary failed with auth error and it's an /export URL, fallback to gviz
        if (csvText == null && csvUrl.Contains("/export?"))
        {
            var gvizUrl = csvUrl.Replace("/export?format=csv", "/gviz/tq?tqx=out:csv");
            _logger.LogInformation("Export URL failed, trying gviz fallback: {Url}", gvizUrl);
            csvText = await TryFetchCsvText(client, gvizUrl);
        }

        if (csvText == null)
        {
            throw new InvalidOperationException(
                "FORBIDDEN:Google Sheet chưa được công khai. " +
                "Vui lòng vào Google Sheet → File → Share → Publish to web, " +
                "hoặc Share → 'Anyone with the link' → Viewer.");
        }

        // Parse CSV with proper multi-line field support
        var allRows = ParseCsvFull(csvText);
        _logger.LogInformation(
            "CSV stats: text length={Length}, parsed rows={ParsedCount}",
            csvText.Length, allRows.Count);

        // Skip header row if detected
        var rows = new List<SheetRow>();
        bool isFirst = true;
        foreach (var columns in allRows)
        {
            if (isFirst)
            {
                isFirst = false;
                var firstCol = (columns.ElementAtOrDefault(0) ?? "").ToLower();
                if (firstCol.Contains("front") || firstCol.Contains("back") ||
                    firstCol.Contains("mặt trước") || firstCol.Contains("câu hỏi") ||
                    firstCol.Contains("từ vựng") || firstCol.Contains("nghĩa"))
                {
                    _logger.LogInformation("Skipped header row: {Header}", firstCol);
                    continue; // Skip header row
                }
            }

            rows.Add(new SheetRow
            {
                FrontText = columns.ElementAtOrDefault(0) ?? "",
                BackText = columns.ElementAtOrDefault(1) ?? "",
                Example = columns.ElementAtOrDefault(2)
            });
        }

        _logger.LogInformation("Fetched {Count} data rows from Google Sheet", rows.Count);
        return rows;
    }

    /// <summary>
    /// Try to fetch CSV text from a URL. Returns null if auth error (401/403/HTML response).
    /// Throws on other errors.
    /// </summary>
    private async Task<string?> TryFetchCsvText(HttpClient client, string url)
    {
        try
        {
            var response = await client.GetAsync(url);
            if (!response.IsSuccessStatusCode)
            {
                var code = (int)response.StatusCode;
                if (code == 401 || code == 403)
                {
                    _logger.LogWarning("Auth error {Code} fetching {Url}", code, url);
                    return null;
                }
                throw new InvalidOperationException($"CONFLICT:Không thể tải Google Sheet (HTTP {code})");
            }

            var text = await response.Content.ReadAsStringAsync();

            // Check if response is HTML (Google login page) instead of CSV
            if (text.TrimStart().StartsWith("<!") || text.TrimStart().StartsWith("<html"))
            {
                _logger.LogWarning("Got HTML instead of CSV from {Url}", url);
                return null;
            }

            return text;
        }
        catch (TaskCanceledException)
        {
            throw new InvalidOperationException("CONFLICT:Google Sheet tải quá lâu (timeout)");
        }
    }

    /// <summary>
    /// Full CSV parser that correctly handles:
    /// - Multi-line values inside quoted fields (newlines within "...")
    /// - Escaped quotes ("" inside fields)
    /// - Commas inside quoted fields
    /// </summary>
    private static List<List<string>> ParseCsvFull(string csv)
    {
        var result = new List<List<string>>();
        var currentField = new System.Text.StringBuilder();
        var currentRow = new List<string>();
        bool inQuotes = false;
        int i = 0;

        while (i < csv.Length)
        {
            char c = csv[i];

            if (inQuotes)
            {
                if (c == '"')
                {
                    // Check for escaped quote ""
                    if (i + 1 < csv.Length && csv[i + 1] == '"')
                    {
                        currentField.Append('"');
                        i += 2;
                        continue;
                    }
                    else
                    {
                        // End of quoted field
                        inQuotes = false;
                        i++;
                        continue;
                    }
                }
                else
                {
                    // Inside quotes: accept everything including newlines
                    currentField.Append(c);
                    i++;
                }
            }
            else
            {
                if (c == '"')
                {
                    inQuotes = true;
                    i++;
                }
                else if (c == ',')
                {
                    currentRow.Add(currentField.ToString().Trim());
                    currentField.Clear();
                    i++;
                }
                else if (c == '\r')
                {
                    // End of row: \r\n or \r
                    currentRow.Add(currentField.ToString().Trim());
                    currentField.Clear();
                    if (currentRow.Any(f => !string.IsNullOrEmpty(f)))
                        result.Add(currentRow);
                    currentRow = new List<string>();
                    i++;
                    if (i < csv.Length && csv[i] == '\n') i++;
                }
                else if (c == '\n')
                {
                    // End of row: \n only
                    currentRow.Add(currentField.ToString().Trim());
                    currentField.Clear();
                    if (currentRow.Any(f => !string.IsNullOrEmpty(f)))
                        result.Add(currentRow);
                    currentRow = new List<string>();
                    i++;
                }
                else
                {
                    currentField.Append(c);
                    i++;
                }
            }
        }

        // Don't forget the last row
        if (currentField.Length > 0 || currentRow.Count > 0)
        {
            currentRow.Add(currentField.ToString().Trim());
            if (currentRow.Any(f => !string.IsNullOrEmpty(f)))
                result.Add(currentRow);
        }

        return result;
    }

    /// <summary>
    /// Convert various Google Sheet URL formats to CSV export URL.
    /// Uses gviz format which works with "Anyone with the link" sharing.
    /// </summary>
    private static string ConvertToCsvUrl(string url)
    {
        url = url.Trim();

        if (url.Contains("/export?") || url.Contains("output=csv") || url.Contains("/pub?"))
            return url;

        var match = System.Text.RegularExpressions.Regex.Match(
            url, @"/spreadsheets/d/([a-zA-Z0-9_-]+)");

        if (match.Success)
        {
            var spreadsheetId = match.Groups[1].Value;
            var gidMatch = System.Text.RegularExpressions.Regex.Match(url, @"gid=(\d+)");
            var gid = gidMatch.Success ? gidMatch.Groups[1].Value : "0";
            // Use export?format=csv — more reliable for large sheets, no row limit
            // Falls back to gviz if export fails (requires less permissions)
            return $"https://docs.google.com/spreadsheets/d/{spreadsheetId}/export?format=csv&gid={gid}";
        }

        if (url.StartsWith("http"))
            return url;

        throw new ArgumentException("Invalid Google Sheet URL. Please use a Google Sheets share link.");
    }

    private class SheetRow
    {
        public string? FrontText { get; set; }
        public string? BackText { get; set; }
        public string? Example { get; set; }
    }
}

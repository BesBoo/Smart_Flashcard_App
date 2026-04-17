-- ============================================================
-- SMART FLASHCARD APPLICATION — Sample Seed Data
-- ============================================================
-- Database: SmartFlashcardDB
-- Purpose:  Insert test data for development & demo
-- Date:     2026-03-16 (Updated: added Language columns)
--
-- INSTRUCTIONS:
--   1. Run SmartFlashcardDB_Create.sql FIRST
--   2. Then run this file in SSMS (F5)
-- ============================================================

USE SmartFlashcardDB;
GO

-- ============================================================
-- 0. ADD LANGUAGE COLUMNS (if not exists)
-- ============================================================
-- Users.PreferredLanguage: UI language ('vi', 'en')
-- Decks.Language: Content language ('vi', 'en', 'ja', 'ko', 'zh', ...)

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Users') AND name = 'PreferredLanguage')
BEGIN
    ALTER TABLE Users
    ADD PreferredLanguage NVARCHAR(10) NOT NULL DEFAULT 'vi';
    PRINT N'✓ Column [Users.PreferredLanguage] added.';
END
ELSE
    PRINT N'⊘ Column [Users.PreferredLanguage] already exists — skipped.';
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Decks') AND name = 'Language')
BEGIN
    ALTER TABLE Decks
    ADD Language NVARCHAR(10) NOT NULL DEFAULT 'vi';
    PRINT N'✓ Column [Decks.Language] added.';
END
ELSE
    PRINT N'⊘ Column [Decks.Language] already exists — skipped.';
GO

-- ============================================================
-- 1. USERS (2 test users)
-- ============================================================

DECLARE @User1 UNIQUEIDENTIFIER = 'A1111111-1111-1111-1111-111111111111';
DECLARE @User2 UNIQUEIDENTIFIER = 'B2222222-2222-2222-2222-222222222222';

INSERT INTO Users (Id, Email, PasswordHash, DisplayName, SubscriptionTier, AiUsageToday, PreferredLanguage)
VALUES
    (@User1, N'besboo@gmail.com',
     N'26072004besboo',
     N'BesBoo', 'Premium', 3, 'vi'),

    (@User2, N'tran.thib@smartflash.app',
     N'$2a$12$LJ3m4ys5Lp8Pnk7Q9vK1eOZq7w8x2y3z4A5B6C7D8E9F0G1H2I3J',
     N'Trần Thị B', 'Premium', 0, 'vi');

PRINT N'✓ 2 users inserted (BesBoo — Premium/vi, Trần Thị B — Premium/vi)';
GO

-- ============================================================
-- 2. DECKS (4 decks with Language)
-- ============================================================

DECLARE @User1 UNIQUEIDENTIFIER = 'A1111111-1111-1111-1111-111111111111';
DECLARE @User2 UNIQUEIDENTIFIER = 'B2222222-2222-2222-2222-222222222222';

DECLARE @Deck_IELTS    UNIQUEIDENTIFIER = 'D0000001-0001-0001-0001-000000000001';
DECLARE @Deck_Biology  UNIQUEIDENTIFIER = 'D0000002-0002-0002-0002-000000000002';
DECLARE @Deck_History  UNIQUEIDENTIFIER = 'D0000003-0003-0003-0003-000000000003';
DECLARE @Deck_Japanese UNIQUEIDENTIFIER = 'D0000004-0004-0004-0004-000000000004';

INSERT INTO Decks (Id, UserId, Name, Description, Language)
VALUES
    -- User 1's decks
    (@Deck_IELTS,   @User1, N'IELTS Vocabulary',
     N'Common IELTS vocabulary for band 7.0+',
     'en'),

    (@Deck_Biology, @User1, N'Sinh học 12 — Di truyền',
     N'Kiến thức di truyền học chuẩn bị thi THPT',
     'vi'),

    (@Deck_History, @User1, N'Lịch sử Việt Nam — Kháng chiến',
     N'Các mốc lịch sử quan trọng thời kỳ kháng chiến',
     'vi'),

    -- User 2's deck
    (@Deck_Japanese, @User2, N'Tiếng Nhật N5',
     N'Từ vựng cơ bản cho kỳ thi JLPT N5',
     'ja');

PRINT N'✓ 4 decks inserted (en: IELTS, vi: Sinh học + Lịch sử, ja: Tiếng Nhật)';
GO

-- ============================================================
-- 3. FLASHCARDS — Deck: IELTS Vocabulary (8 cards, lang=en)
-- ============================================================

DECLARE @User1 UNIQUEIDENTIFIER = 'A1111111-1111-1111-1111-111111111111';
DECLARE @Deck_IELTS UNIQUEIDENTIFIER = 'D0000001-0001-0001-0001-000000000001';

INSERT INTO Flashcards (Id, UserId, DeckId, FrontText, BackText, ExampleText,
                        Repetition, IntervalDays, EaseFactor, NextReviewDate, FailCount, TotalReviews)
VALUES
    -- Card 1: Well-practiced (mastered)
    (NEWID(), @User1, @Deck_IELTS,
     N'Ambiguous',
     N'(adj) Mơ hồ, không rõ ràng — having more than one possible meaning',
     N'The politician''s ambiguous statement left reporters confused.',
     4, 21, 2.7, DATEADD(DAY, 21, GETUTCDATE()), 0, 6),

    -- Card 2: In progress
    (NEWID(), @User1, @Deck_IELTS,
     N'Diligent',
     N'(adj) Siêng năng, chăm chỉ — showing careful and persistent effort',
     N'She was a diligent student who always completed her homework on time.',
     2, 6, 2.6, DATEADD(DAY, 2, GETUTCDATE()), 0, 3),

    -- Card 3: Struggling (failCount = 4 — triggers AI adaptive)
    (NEWID(), @User1, @Deck_IELTS,
     N'Ubiquitous',
     N'(adj) Có mặt khắp nơi — present, appearing, or found everywhere',
     N'Smartphones have become ubiquitous in modern society.',
     0, 1, 1.8, GETUTCDATE(), 4, 7),

    -- Card 4: Due today
    (NEWID(), @User1, @Deck_IELTS,
     N'Ephemeral',
     N'(adj) Phù du, tạm thời — lasting for a very short time',
     N'The beauty of cherry blossoms is ephemeral.',
     1, 1, 2.5, GETUTCDATE(), 1, 2),

    -- Card 5: Due today
    (NEWID(), @User1, @Deck_IELTS,
     N'Pragmatic',
     N'(adj) Thực dụng — dealing with things sensibly and realistically',
     N'We need a pragmatic approach to solve this problem.',
     1, 1, 2.5, GETUTCDATE(), 0, 1),

    -- Card 6: New (never reviewed)
    (NEWID(), @User1, @Deck_IELTS,
     N'Meticulous',
     N'(adj) Tỉ mỉ, cẩn thận — showing great attention to detail',
     N'The artist was meticulous in her brushwork.',
     0, 1, 2.5, GETUTCDATE(), 0, 0),

    -- Card 7: New
    (NEWID(), @User1, @Deck_IELTS,
     N'Benevolent',
     N'(adj) Nhân từ, tốt bụng — well-meaning and kindly',
     N'The benevolent donor funded scholarships for underprivileged students.',
     0, 1, 2.5, GETUTCDATE(), 0, 0),

    -- Card 8: New
    (NEWID(), @User1, @Deck_IELTS,
     N'Candid',
     N'(adj) Thẳng thắn, chân thành — truthful and straightforward',
     N'She gave a candid interview about her struggles with anxiety.',
     0, 1, 2.5, GETUTCDATE(), 0, 0);

PRINT N'✓ 8 IELTS flashcards inserted (1 mastered, 2 in-progress, 2 due today, 3 new)';
GO

-- ============================================================
-- 4. FLASHCARDS — Deck: Sinh học 12 (5 cards, lang=vi)
-- ============================================================

DECLARE @User1 UNIQUEIDENTIFIER = 'A1111111-1111-1111-1111-111111111111';
DECLARE @Deck_Biology UNIQUEIDENTIFIER = 'D0000002-0002-0002-0002-000000000002';

INSERT INTO Flashcards (Id, UserId, DeckId, FrontText, BackText, ExampleText,
                        Repetition, IntervalDays, EaseFactor, NextReviewDate, FailCount, TotalReviews)
VALUES
    (NEWID(), @User1, @Deck_Biology,
     N'Quy luật phân ly của Mendel là gì?',
     N'Mỗi tính trạng do một cặp nhân tố di truyền quy định. Khi hình thành giao tử, các nhân tố di truyền phân ly đồng đều về các giao tử.',
     N'Ví dụ: Lai giữa cây hoa đỏ (AA) và hoa trắng (aa) → F1 toàn hoa đỏ (Aa), F2 tỉ lệ 3 đỏ : 1 trắng.',
     1, 1, 2.5, GETUTCDATE(), 0, 1),

    (NEWID(), @User1, @Deck_Biology,
     N'Phân biệt nguyên phân và giảm phân',
     N'Nguyên phân: 1 lần phân bào → 2 tế bào con (2n). Giảm phân: 2 lần phân bào → 4 tế bào con (n).',
     N'Nguyên phân xảy ra ở tế bào sinh dưỡng, giảm phân xảy ra ở tế bào sinh dục.',
     0, 1, 2.5, GETUTCDATE(), 2, 3),

    (NEWID(), @User1, @Deck_Biology,
     N'ADN là gì?',
     N'Axit Deoxyribonucleic — đại phân tử mang thông tin di truyền, cấu trúc xoắn kép gồm 2 mạch polynucleotide.',
     N'ADN nằm chủ yếu trong nhân tế bào, ngoài ra còn có ở ty thể và lục lạp.',
     0, 1, 2.5, GETUTCDATE(), 0, 0),

    (NEWID(), @User1, @Deck_Biology,
     N'Đột biến gen là gì?',
     N'Là những biến đổi trong cấu trúc của gen, liên quan đến một hoặc một số cặp nucleotide.',
     N'Các dạng: mất, thêm, thay thế cặp nucleotide. VD: bệnh hồng cầu hình liềm do thay thế 1 cặp nucleotide.',
     0, 1, 2.5, GETUTCDATE(), 0, 0),

    (NEWID(), @User1, @Deck_Biology,
     N'Mã di truyền có đặc điểm gì?',
     N'Mã bộ ba (triplet), tính đặc hiệu, tính thoái hóa, tính phổ biến. Có 64 bộ ba, trong đó 61 mã hóa axit amin, 3 bộ ba kết thúc.',
     NULL,
     0, 1, 2.5, GETUTCDATE(), 0, 0);

PRINT N'✓ 5 Biology flashcards inserted (Vietnamese biology content)';
GO

-- ============================================================
-- 5. FLASHCARDS — Deck: Lịch sử (3 cards, lang=vi)
-- ============================================================

DECLARE @User1 UNIQUEIDENTIFIER = 'A1111111-1111-1111-1111-111111111111';
DECLARE @Deck_History UNIQUEIDENTIFIER = 'D0000003-0003-0003-0003-000000000003';

INSERT INTO Flashcards (Id, UserId, DeckId, FrontText, BackText, ExampleText,
                        Repetition, IntervalDays, EaseFactor, NextReviewDate, FailCount, TotalReviews)
VALUES
    (NEWID(), @User1, @Deck_History,
     N'Cách mạng tháng Tám diễn ra khi nào?',
     N'Tháng 8 năm 1945. Ngày 19/8/1945: khởi nghĩa thắng lợi ở Hà Nội. Ngày 2/9/1945: Chủ tịch Hồ Chí Minh đọc Tuyên ngôn Độc lập.',
     NULL,
     2, 6, 2.6, DATEADD(DAY, 3, GETUTCDATE()), 0, 3),

    (NEWID(), @User1, @Deck_History,
     N'Chiến thắng Điện Biên Phủ diễn ra năm nào?',
     N'Ngày 7/5/1954. Kết thúc 56 ngày đêm chiến đấu. Buộc Pháp ký Hiệp định Genève (21/7/1954).',
     N'Chiến dịch do Đại tướng Võ Nguyên Giáp chỉ huy.',
     1, 1, 2.5, GETUTCDATE(), 0, 1),

    (NEWID(), @User1, @Deck_History,
     N'Hiệp định Paris được ký kết khi nào?',
     N'Ngày 27/1/1973. Mỹ công nhận độc lập, chủ quyền, thống nhất và toàn vẹn lãnh thổ của Việt Nam. Mỹ rút quân khỏi miền Nam.',
     NULL,
     0, 1, 2.5, GETUTCDATE(), 0, 0);

PRINT N'✓ 3 History flashcards inserted (Vietnamese history content)';
GO

-- ============================================================
-- 6. FLASHCARDS — Deck: Tiếng Nhật N5 (5 cards, lang=ja)
-- ============================================================

DECLARE @User2 UNIQUEIDENTIFIER = 'B2222222-2222-2222-2222-222222222222';
DECLARE @Deck_Japanese UNIQUEIDENTIFIER = 'D0000004-0004-0004-0004-000000000004';

INSERT INTO Flashcards (Id, UserId, DeckId, FrontText, BackText, ExampleText,
                        Repetition, IntervalDays, EaseFactor, NextReviewDate, FailCount, TotalReviews)
VALUES
    (NEWID(), @User2, @Deck_Japanese,
     N'おはようございます',
     N'Chào buổi sáng (Ohayou gozaimasu)',
     N'先生、おはようございます。(Sensei, ohayou gozaimasu) — Chào buổi sáng, thầy/cô.',
     3, 15, 2.7, DATEADD(DAY, 10, GETUTCDATE()), 0, 4),

    (NEWID(), @User2, @Deck_Japanese,
     N'ありがとうございます',
     N'Cảm ơn (Arigatou gozaimasu)',
     N'プレゼントをありがとうございます。(Purezento wo arigatou gozaimasu) — Cảm ơn vì món quà.',
     2, 6, 2.6, DATEADD(DAY, 3, GETUTCDATE()), 0, 3),

    (NEWID(), @User2, @Deck_Japanese,
     N'すみません',
     N'Xin lỗi / Xin phép (Sumimasen)',
     N'すみません、駅はどこですか。(Sumimasen, eki wa doko desu ka) — Xin lỗi, nhà ga ở đâu ạ?',
     1, 1, 2.5, GETUTCDATE(), 0, 1),

    (NEWID(), @User2, @Deck_Japanese,
     N'食べる (たべる)',
     N'Ăn (Taberu) — Động từ nhóm 2',
     N'朝ごはんを食べます。(Asagohan wo tabemasu) — Tôi ăn sáng.',
     0, 1, 2.5, GETUTCDATE(), 0, 0),

    (NEWID(), @User2, @Deck_Japanese,
     N'飲む (のむ)',
     N'Uống (Nomu) — Động từ nhóm 1',
     N'水を飲みます。(Mizu wo nomimasu) — Tôi uống nước.',
     0, 1, 2.5, GETUTCDATE(), 0, 0);

PRINT N'✓ 5 Japanese flashcards inserted (JLPT N5 level)';
GO

-- ============================================================
-- 7. REVIEW LOGS (15 sample reviews)
-- ============================================================

DECLARE @User1 UNIQUEIDENTIFIER = 'A1111111-1111-1111-1111-111111111111';

-- Get some card IDs to attach reviews to
DECLARE @Card1 UNIQUEIDENTIFIER, @Card2 UNIQUEIDENTIFIER, @Card3 UNIQUEIDENTIFIER;

SELECT TOP 1 @Card1 = Id FROM Flashcards
WHERE UserId = @User1 AND FrontText = N'Ambiguous';

SELECT TOP 1 @Card2 = Id FROM Flashcards
WHERE UserId = @User1 AND FrontText = N'Diligent';

SELECT TOP 1 @Card3 = Id FROM Flashcards
WHERE UserId = @User1 AND FrontText = N'Ubiquitous';

-- Reviews for "Ambiguous" (mastered — all good/easy)
INSERT INTO ReviewLogs (Id, UserId, FlashcardId, Quality, ResponseTimeMs, ReviewedAt)
VALUES
    (NEWID(), @User1, @Card1, 3, 5200, DATEADD(DAY, -14, GETUTCDATE())),
    (NEWID(), @User1, @Card1, 3, 4100, DATEADD(DAY, -13, GETUTCDATE())),
    (NEWID(), @User1, @Card1, 5, 2800, DATEADD(DAY, -7, GETUTCDATE())),
    (NEWID(), @User1, @Card1, 5, 2100, DATEADD(DAY, -1, GETUTCDATE()));

-- Reviews for "Diligent" (progressing)
INSERT INTO ReviewLogs (Id, UserId, FlashcardId, Quality, ResponseTimeMs, ReviewedAt)
VALUES
    (NEWID(), @User1, @Card2, 2, 7500, DATEADD(DAY, -5, GETUTCDATE())),
    (NEWID(), @User1, @Card2, 3, 4200, DATEADD(DAY, -4, GETUTCDATE())),
    (NEWID(), @User1, @Card2, 3, 3800, DATEADD(DAY, -1, GETUTCDATE()));

-- Reviews for "Ubiquitous" (struggling — multiple fails)
INSERT INTO ReviewLogs (Id, UserId, FlashcardId, Quality, ResponseTimeMs, ReviewedAt)
VALUES
    (NEWID(), @User1, @Card3, 0, 9200, DATEADD(DAY, -6, GETUTCDATE())),
    (NEWID(), @User1, @Card3, 0, 8500, DATEADD(DAY, -5, GETUTCDATE())),
    (NEWID(), @User1, @Card3, 2, 7100, DATEADD(DAY, -4, GETUTCDATE())),
    (NEWID(), @User1, @Card3, 0, 8800, DATEADD(DAY, -3, GETUTCDATE())),
    (NEWID(), @User1, @Card3, 0, 9500, DATEADD(DAY, -2, GETUTCDATE())),
    (NEWID(), @User1, @Card3, 2, 6200, DATEADD(DAY, -1, GETUTCDATE())),
    (NEWID(), @User1, @Card3, 0, 7800, GETUTCDATE());

PRINT N'✓ 15 review logs inserted (4 mastered + 3 progressing + 8 struggling)';
GO

-- ============================================================
-- 8. AI CHAT HISTORY (1 sample conversation)
-- ============================================================

DECLARE @User1 UNIQUEIDENTIFIER = 'A1111111-1111-1111-1111-111111111111';
DECLARE @SessionId UNIQUEIDENTIFIER = 'C0000001-0001-0001-0001-000000000001';

INSERT INTO AiChatHistory (Id, UserId, SessionId, Role, Content, CreatedAt)
VALUES
    (NEWID(), @User1, @SessionId, 'user',
     N'Giải thích từ "ubiquitous" cho em đơn giản hơn',
     DATEADD(MINUTE, -10, GETUTCDATE())),

    (NEWID(), @User1, @SessionId, 'assistant',
     N'Từ "ubiquitous" nghĩa là "có mặt ở khắp mọi nơi". Bạn có thể nhớ đơn giản: giống như Wi-Fi vậy — đi đâu cũng thấy! 📡 Ví dụ: "Smartphones are ubiquitous" = Điện thoại thông minh có mặt ở khắp nơi.',
     DATEADD(MINUTE, -9, GETUTCDATE())),

    (NEWID(), @User1, @SessionId, 'user',
     N'Cho em thêm ví dụ nữa',
     DATEADD(MINUTE, -8, GETUTCDATE())),

    (NEWID(), @User1, @SessionId, 'assistant',
     N'Dĩ nhiên! Đây là thêm ví dụ nhé: 1️⃣ "Coffee shops are ubiquitous in big cities." (Quán cà phê có mặt khắp nơi ở thành phố lớn.) 2️⃣ "Social media has become ubiquitous among teenagers." (Mạng xã hội đã trở nên phổ biến khắp nơi trong giới trẻ.) 3️⃣ "The ubiquitous plastic bag is a major environmental concern." (Túi nhựa có mặt khắp nơi là một vấn đề môi trường lớn.)',
     DATEADD(MINUTE, -7, GETUTCDATE()));

PRINT N'✓ 4 AI chat messages inserted (1 conversation session)';
GO

-- ============================================================
-- SUMMARY
-- ============================================================

PRINT N'';
PRINT N'============================================================';
PRINT N'  SAMPLE DATA INSERTED SUCCESSFULLY';
PRINT N'============================================================';
PRINT N'';
PRINT N'  Schema updates:';
PRINT N'    + Users.PreferredLanguage (vi/en)';
PRINT N'    + Decks.Language (vi/en/ja/ko/zh/...)';
PRINT N'';
PRINT N'  Users:       2 (BesBoo — Premium, Trần Thị B — Premium)';
PRINT N'  Decks:       4 (en: IELTS, vi: Sinh học + Lịch sử, ja: Tiếng Nhật)';
PRINT N'  Flashcards: 21 (8 IELTS + 5 Bio + 3 History + 5 Japanese)';
PRINT N'  ReviewLogs: 15 (mixed quality scores)';
PRINT N'  AI Chat:     4 messages (1 session)';
PRINT N'';
PRINT N'  Test accounts:';
PRINT N'    besboo@gmail.com             (Premium, 3 decks, 16 cards)';
PRINT N'    tran.thib@smartflash.app     (Premium, 1 deck,   5 cards)';
PRINT N'';
PRINT N'  Language mapping:';
PRINT N'    IELTS Vocabulary        → en (English)';
PRINT N'    Sinh học 12             → vi (Vietnamese)';
PRINT N'    Lịch sử Việt Nam        → vi (Vietnamese)';
PRINT N'    Tiếng Nhật N5           → ja (Japanese)';
PRINT N'============================================================';
GO

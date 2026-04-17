-- ============================================================
-- SMART FLASHCARD APPLICATION — SQL Server Database Script
-- ============================================================
-- Database: SmartFlashcardDB
-- Server:   SQL Server (SSMS)
-- Date:     2026-03-16
-- Version:  1.0
-- 
-- INSTRUCTIONS:
--   1. Open this file in SSMS
--   2. Connect to your SQL Server instance
--   3. Press F5 (Execute) to run the entire script
--   4. Verify: all 6 tables + 8 indexes created
-- ============================================================

-- ============================================================
-- STEP 1: CREATE DATABASE
-- ============================================================
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'SmartFlashcardDB')
BEGIN
    CREATE DATABASE SmartFlashcardDB;
END
GO

USE SmartFlashcardDB;
GO

-- ============================================================
-- STEP 2: CREATE TABLES
-- ============================================================

-- ------------------------------------------------------------
-- TABLE 1: Users
-- Purpose: Authentication, profile, subscription, AI usage
-- ------------------------------------------------------------
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Users')
BEGIN
    CREATE TABLE Users (
        -- Identity
        Id                UNIQUEIDENTIFIER  PRIMARY KEY DEFAULT NEWID(),
        Email             NVARCHAR(255)     NOT NULL,
        PasswordHash      NVARCHAR(512)     NOT NULL,
        DisplayName       NVARCHAR(100)     NOT NULL,
        AvatarUrl         NVARCHAR(500)     NULL,
        Role              NVARCHAR(20)      NOT NULL DEFAULT 'user',

        -- Subscription & AI usage
        SubscriptionTier  NVARCHAR(20)      NOT NULL DEFAULT 'Free',
        AiUsageToday      INT               NOT NULL DEFAULT 0,
        AiUsageResetDate  DATE              NOT NULL DEFAULT CAST(GETUTCDATE() AS DATE),

        -- Status & timestamps
        IsActive          BIT               NOT NULL DEFAULT 1,
        CreatedAt         DATETIME2         NOT NULL DEFAULT GETUTCDATE(),
        UpdatedAt         DATETIME2         NOT NULL DEFAULT GETUTCDATE(),

        -- Constraints
        CONSTRAINT UQ_Users_Email UNIQUE (Email),
        CONSTRAINT CK_Users_Tier CHECK (SubscriptionTier IN ('Free', 'Premium')),
        CONSTRAINT CK_Users_Role CHECK (Role IN ('user', 'admin'))
    );

    PRINT '✓ Table [Users] created successfully.';
END
ELSE
    PRINT '⊘ Table [Users] already exists — skipped.';
GO

-- ------------------------------------------------------------
-- TABLE 2: Decks
-- Purpose: Group flashcards into named collections
-- ------------------------------------------------------------
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Decks')
BEGIN
    CREATE TABLE Decks (
        Id                UNIQUEIDENTIFIER  PRIMARY KEY DEFAULT NEWID(),
        UserId            UNIQUEIDENTIFIER  NOT NULL,
        Name              NVARCHAR(200)     NOT NULL,
        Description       NVARCHAR(1000)    NULL,
        CoverImageUrl     NVARCHAR(500)     NULL,

        -- Soft delete & timestamps
        IsDeleted         BIT               NOT NULL DEFAULT 0,
        CreatedAt         DATETIME2         NOT NULL DEFAULT GETUTCDATE(),
        UpdatedAt         DATETIME2         NOT NULL DEFAULT GETUTCDATE(),

        -- Foreign keys
        CONSTRAINT FK_Decks_Users FOREIGN KEY (UserId) REFERENCES Users(Id)
    );

    PRINT '✓ Table [Decks] created successfully.';
END
ELSE
    PRINT '⊘ Table [Decks] already exists — skipped.';
GO

-- ------------------------------------------------------------
-- TABLE 3: Flashcards
-- Purpose: Core learning unit — card content + SM-2 state
-- 
-- !! SM-2 fields (Repetition, IntervalDays, EaseFactor,
--    NextReviewDate) are CLIENT-CALCULATED.
--    Server validates bounds but NEVER recalculates.
-- ------------------------------------------------------------
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Flashcards')
BEGIN
    CREATE TABLE Flashcards (
        -- Identity
        Id                UNIQUEIDENTIFIER  PRIMARY KEY DEFAULT NEWID(),
        UserId            UNIQUEIDENTIFIER  NOT NULL,
        DeckId            UNIQUEIDENTIFIER  NOT NULL,

        -- Card content
        FrontText         NVARCHAR(MAX)     NOT NULL,
        BackText          NVARCHAR(MAX)     NOT NULL,
        ExampleText       NVARCHAR(MAX)     NULL,
        ImageUrl          NVARCHAR(500)     NULL,
        AudioUrl          NVARCHAR(500)     NULL,

        -- SM-2 state (client-calculated, server stores only)
        Repetition        INT               NOT NULL DEFAULT 0,
        IntervalDays      INT               NOT NULL DEFAULT 1,
        EaseFactor        FLOAT             NOT NULL DEFAULT 2.5,
        NextReviewDate    DATETIME2         NOT NULL DEFAULT GETUTCDATE(),
        FailCount         INT               NOT NULL DEFAULT 0,
        TotalReviews      INT               NOT NULL DEFAULT 0,

        -- Soft delete & timestamps
        IsDeleted         BIT               NOT NULL DEFAULT 0,
        CreatedAt         DATETIME2         NOT NULL DEFAULT GETUTCDATE(),
        UpdatedAt         DATETIME2         NOT NULL DEFAULT GETUTCDATE(),

        -- Foreign keys
        CONSTRAINT FK_Flashcards_Users FOREIGN KEY (UserId) REFERENCES Users(Id),
        CONSTRAINT FK_Flashcards_Decks FOREIGN KEY (DeckId) REFERENCES Decks(Id),

        -- SM-2 validation constraints
        CONSTRAINT CK_Flashcards_EF   CHECK (EaseFactor >= 1.3),
        CONSTRAINT CK_Flashcards_Rep  CHECK (Repetition >= 0),
        CONSTRAINT CK_Flashcards_Int  CHECK (IntervalDays >= 1)
    );

    PRINT '✓ Table [Flashcards] created successfully.';
END
ELSE
    PRINT '⊘ Table [Flashcards] already exists — skipped.';
GO

-- ------------------------------------------------------------
-- TABLE 4: ReviewLogs
-- Purpose: Append-only review history for stats & AI analysis
-- 
-- !! This table is APPEND-ONLY. No updates, no deletes.
--    Sync direction: push-only (client → server).
-- ------------------------------------------------------------
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ReviewLogs')
BEGIN
    CREATE TABLE ReviewLogs (
        Id                UNIQUEIDENTIFIER  PRIMARY KEY DEFAULT NEWID(),
        UserId            UNIQUEIDENTIFIER  NOT NULL,
        FlashcardId       UNIQUEIDENTIFIER  NOT NULL,
        Quality           INT               NOT NULL,
        ResponseTimeMs    BIGINT            NULL,
        ReviewedAt        DATETIME2         NOT NULL DEFAULT GETUTCDATE(),

        -- Foreign keys
        CONSTRAINT FK_ReviewLogs_Users FOREIGN KEY (UserId) REFERENCES Users(Id),
        CONSTRAINT FK_ReviewLogs_Cards FOREIGN KEY (FlashcardId) REFERENCES Flashcards(Id),

        -- Quality must be one of: 0 (Học lại), 2 (Khó), 3 (Tốt), 5 (Dễ)
        CONSTRAINT CK_ReviewLogs_Quality CHECK (Quality IN (0, 2, 3, 5))
    );

    PRINT '✓ Table [ReviewLogs] created successfully.';
END
ELSE
    PRINT '⊘ Table [ReviewLogs] already exists — skipped.';
GO

-- ------------------------------------------------------------
-- TABLE 5: AiChatHistory
-- Purpose: Persist AI Tutor conversation history
-- ------------------------------------------------------------
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'AiChatHistory')
BEGIN
    CREATE TABLE AiChatHistory (
        Id                UNIQUEIDENTIFIER  PRIMARY KEY DEFAULT NEWID(),
        UserId            UNIQUEIDENTIFIER  NOT NULL,
        SessionId         UNIQUEIDENTIFIER  NOT NULL,
        Role              NVARCHAR(20)      NOT NULL,
        Content           NVARCHAR(MAX)     NOT NULL,
        CreatedAt         DATETIME2         NOT NULL DEFAULT GETUTCDATE(),

        -- Foreign keys
        CONSTRAINT FK_AiChat_Users FOREIGN KEY (UserId) REFERENCES Users(Id),

        -- Role must be 'user' or 'assistant'
        CONSTRAINT CK_AiChat_Role CHECK (Role IN ('user', 'assistant'))
    );

    PRINT '✓ Table [AiChatHistory] created successfully.';
END
ELSE
    PRINT '⊘ Table [AiChatHistory] already exists — skipped.';
GO

-- ------------------------------------------------------------
-- TABLE 6: SyncMetadata
-- Purpose: Track pending sync operations for delta sync
-- ------------------------------------------------------------
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'SyncMetadata')
BEGIN
    CREATE TABLE SyncMetadata (
        Id                UNIQUEIDENTIFIER  PRIMARY KEY DEFAULT NEWID(),
        UserId            UNIQUEIDENTIFIER  NOT NULL,
        EntityType        NVARCHAR(50)      NOT NULL,
        EntityId          UNIQUEIDENTIFIER  NOT NULL,
        Action            NVARCHAR(20)      NOT NULL,
        UpdatedAt         DATETIME2         NOT NULL DEFAULT GETUTCDATE(),
        IsSynced          BIT               NOT NULL DEFAULT 0,

        -- Foreign keys
        CONSTRAINT FK_Sync_Users FOREIGN KEY (UserId) REFERENCES Users(Id),

        -- Constraints
        CONSTRAINT CK_Sync_Action CHECK (Action IN ('CREATE', 'UPDATE', 'DELETE')),
        CONSTRAINT CK_Sync_Entity CHECK (EntityType IN ('deck', 'flashcard', 'review_log', 'ai_chat'))
    );

    PRINT '✓ Table [SyncMetadata] created successfully.';
END
ELSE
    PRINT '⊘ Table [SyncMetadata] already exists — skipped.';
GO


-- ============================================================
-- STEP 3: CREATE INDEXES
-- ============================================================

-- Decks: list user's active decks
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Decks_UserId')
    CREATE INDEX IX_Decks_UserId
        ON Decks(UserId) WHERE IsDeleted = 0;
GO

-- Flashcards: list cards in a deck (active only)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Flashcards_UserDeck')
    CREATE INDEX IX_Flashcards_UserDeck
        ON Flashcards(UserId, DeckId) WHERE IsDeleted = 0;
GO

-- Flashcards: daily study queue query (due cards)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Flashcards_NextReview')
    CREATE INDEX IX_Flashcards_NextReview
        ON Flashcards(UserId, NextReviewDate) WHERE IsDeleted = 0;
GO

-- Flashcards: delta sync pull (changes since timestamp)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Flashcards_UpdatedAt')
    CREATE INDEX IX_Flashcards_UpdatedAt
        ON Flashcards(UserId, UpdatedAt);
GO

-- ReviewLogs: AI adaptive analysis per card
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_ReviewLogs_FlashcardId')
    CREATE INDEX IX_ReviewLogs_FlashcardId
        ON ReviewLogs(FlashcardId);
GO

-- ReviewLogs: statistics by date range
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_ReviewLogs_UserDate')
    CREATE INDEX IX_ReviewLogs_UserDate
        ON ReviewLogs(UserId, ReviewedAt);
GO

-- AiChatHistory: load conversation in order
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_AiChat_UserSession')
    CREATE INDEX IX_AiChat_UserSession
        ON AiChatHistory(UserId, SessionId, CreatedAt);
GO

-- SyncMetadata: find pending sync entries
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Sync_UserPending')
    CREATE INDEX IX_Sync_UserPending
        ON SyncMetadata(UserId, IsSynced) WHERE IsSynced = 0;
GO

PRINT '';
PRINT '============================================================';
PRINT '  ALL TABLES AND INDEXES CREATED SUCCESSFULLY';
PRINT '============================================================';
PRINT '';
PRINT '  Tables:  Users, Decks, Flashcards, ReviewLogs,';
PRINT '           AiChatHistory, SyncMetadata';
PRINT '  Indexes: 8 indexes created';
PRINT '';
PRINT '  Next: Run SmartFlashcardDB_SeedData.sql (optional)';
PRINT '============================================================';
GO


-- ============================================================
-- STEP 4: SEED DATA (Optional — test data for development)
-- ============================================================
-- Uncomment the block below to insert sample data for testing.

/*
-- Sample user (password: "Test123!" hashed with bcrypt)
DECLARE @TestUserId UNIQUEIDENTIFIER = NEWID();
INSERT INTO Users (Id, Email, PasswordHash, DisplayName, SubscriptionTier)
VALUES (
    @TestUserId,
    N'testuser@smartflash.app',
    N'$2a$12$LJ3m4ys5Lp8Pnk7Q9vK1eOZq7w8x2y3z4A5B6C7D8E9F0G1H2I3J',
    N'Test User',
    'Free'
);

-- Sample deck
DECLARE @TestDeckId UNIQUEIDENTIFIER = NEWID();
INSERT INTO Decks (Id, UserId, Name, Description)
VALUES (
    @TestDeckId,
    @TestUserId,
    N'English Vocabulary - IELTS',
    N'Common IELTS vocabulary words'
);

-- Sample flashcards
INSERT INTO Flashcards (Id, UserId, DeckId, FrontText, BackText, ExampleText, Repetition, IntervalDays, EaseFactor, NextReviewDate)
VALUES
    (NEWID(), @TestUserId, @TestDeckId,
     N'Ambiguous',
     N'Mơ hồ, không rõ ràng — having more than one possible meaning',
     N'The politician''s ambiguous statement confused reporters.',
     0, 1, 2.5, GETUTCDATE()),

    (NEWID(), @TestUserId, @TestDeckId,
     N'Diligent',
     N'Siêng năng, chăm chỉ — showing careful and persistent effort',
     N'She was a diligent student who always completed her homework on time.',
     2, 6, 2.6, DATEADD(DAY, 6, GETUTCDATE())),

    (NEWID(), @TestUserId, @TestDeckId,
     N'Ubiquitous',
     N'Có mặt khắp nơi — present, appearing, or found everywhere',
     N'Smartphones have become ubiquitous in modern society.',
     0, 1, 2.5, GETUTCDATE()),

    (NEWID(), @TestUserId, @TestDeckId,
     N'Ephemeral',
     N'Phù du, tạm thời — lasting for a very short time',
     N'The beauty of cherry blossoms is ephemeral, lasting only a few weeks.',
     1, 1, 2.5, DATEADD(DAY, 1, GETUTCDATE())),

    (NEWID(), @TestUserId, @TestDeckId,
     N'Pragmatic',
     N'Thực dụng — dealing with things sensibly and realistically',
     N'We need a pragmatic approach to solve this problem.',
     3, 15, 2.7, DATEADD(DAY, 15, GETUTCDATE()));

-- Sample review logs
DECLARE @CardId UNIQUEIDENTIFIER;
SELECT TOP 1 @CardId = Id FROM Flashcards WHERE UserId = @TestUserId;

INSERT INTO ReviewLogs (Id, UserId, FlashcardId, Quality, ResponseTimeMs, ReviewedAt)
VALUES
    (NEWID(), @TestUserId, @CardId, 3, 4200, DATEADD(DAY, -2, GETUTCDATE())),
    (NEWID(), @TestUserId, @CardId, 5, 2100, DATEADD(DAY, -1, GETUTCDATE())),
    (NEWID(), @TestUserId, @CardId, 3, 3500, GETUTCDATE());

PRINT '';
PRINT '✓ Sample data inserted: 1 user, 1 deck, 5 cards, 3 review logs';
*/
GO

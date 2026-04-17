-- ============================================================
-- Migration: add Role column to existing Users table
-- Run once on SmartFlashcardDB if the table was created before Role existed.
-- ============================================================
USE SmartFlashcardDB;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Users') AND name = N'Role'
)
BEGIN
    ALTER TABLE dbo.Users ADD Role NVARCHAR(20) NOT NULL CONSTRAINT DF_Users_Role DEFAULT ('user');
    ALTER TABLE dbo.Users ADD CONSTRAINT CK_Users_Role CHECK (Role IN ('user', 'admin'));
    PRINT '✓ Column [Users].[Role] added.';
END
ELSE
    PRINT '⊘ Column [Users].[Role] already exists — skipped.';
GO

-- Optional: promote an account to admin (set email before running)
-- UPDATE dbo.Users SET Role = N'admin' WHERE Email = N'your-admin@email.com';
GO

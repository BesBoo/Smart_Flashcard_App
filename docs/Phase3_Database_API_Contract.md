# Phase 3: Database Schema Design & API Contract
## Smart Flashcard Mobile Application

> **Document Version**: 1.0
> **Date**: 2026-03-16
> **Prerequisite**: Phase 1 (Requirements) + Phase 2 (Architecture) — completed
> **Next Phase**: Phase 4 — Backend API Development

---

## Part A: Database Schema Design (SQL Server — SSMS)

### General Rules

| Rule | Detail |
|---|---|
| **RDBMS** | Microsoft SQL Server, managed via SSMS |
| **Primary Keys** | `UNIQUEIDENTIFIER` (UUID), generated client-side for offline-first |
| **Timestamps** | All tables have `CreatedAt` / `UpdatedAt` in `DATETIME2`, stored as **UTC** |
| **Multi-tenancy** | All data tables include `UserId` FK — every query is user-scoped |
| **Soft Delete** | Tables with user content use `IsDeleted BIT DEFAULT 0` |
| **Naming** | PascalCase for tables and columns. Singular table names. |
| **Collation** | `Vietnamese_CI_AS` for text columns supporting Vietnamese content |

---

### Table 1: Users

**Purpose**: Authentication, user profile, subscription tier, and AI usage tracking.

```sql
CREATE TABLE Users (
    -- Identity
    Id                UNIQUEIDENTIFIER  PRIMARY KEY DEFAULT NEWID(),
    Email             NVARCHAR(255)     NOT NULL,
    PasswordHash      NVARCHAR(512)     NOT NULL,
    DisplayName       NVARCHAR(100)     NOT NULL,
    AvatarUrl         NVARCHAR(500)     NULL,

    -- Subscription & AI
    SubscriptionTier  NVARCHAR(20)      NOT NULL DEFAULT 'Free',  -- 'Free' | 'Premium'
    AiUsageToday      INT               NOT NULL DEFAULT 0,
    AiUsageResetDate  DATE              NOT NULL DEFAULT CAST(GETUTCDATE() AS DATE),

    -- Status
    IsActive          BIT               NOT NULL DEFAULT 1,
    CreatedAt         DATETIME2         NOT NULL DEFAULT GETUTCDATE(),
    UpdatedAt         DATETIME2         NOT NULL DEFAULT GETUTCDATE(),

    -- Constraints
    CONSTRAINT UQ_Users_Email UNIQUE (Email),
    CONSTRAINT CK_Users_Tier CHECK (SubscriptionTier IN ('Free', 'Premium'))
);
```

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `Id` | UNIQUEIDENTIFIER | No | PK, default NEWID() |
| `Email` | NVARCHAR(255) | No | Unique, login identifier |
| `PasswordHash` | NVARCHAR(512) | No | bcrypt hash (server-side) |
| `DisplayName` | NVARCHAR(100) | No | User's display name |
| `AvatarUrl` | NVARCHAR(500) | Yes | URL to profile image |
| `SubscriptionTier` | NVARCHAR(20) | No | 'Free' or 'Premium' |
| `AiUsageToday` | INT | No | Counter reset daily. Compared against tier limit. |
| `AiUsageResetDate` | DATE | No | Date when `AiUsageToday` was last reset. If != today, reset to 0. |
| `IsActive` | BIT | No | Soft account disable |
| `CreatedAt` | DATETIME2 | No | UTC |
| `UpdatedAt` | DATETIME2 | No | UTC, updated on every write |

---

### Table 2: Decks

**Purpose**: Group flashcards into named collections.

```sql
CREATE TABLE Decks (
    Id                UNIQUEIDENTIFIER  PRIMARY KEY DEFAULT NEWID(),
    UserId            UNIQUEIDENTIFIER  NOT NULL,
    Name              NVARCHAR(200)     NOT NULL,
    Description       NVARCHAR(1000)    NULL,
    CoverImageUrl     NVARCHAR(500)     NULL,

    IsDeleted         BIT               NOT NULL DEFAULT 0,
    CreatedAt         DATETIME2         NOT NULL DEFAULT GETUTCDATE(),
    UpdatedAt         DATETIME2         NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT FK_Decks_Users FOREIGN KEY (UserId) REFERENCES Users(Id)
);

-- Indexes
CREATE INDEX IX_Decks_UserId ON Decks(UserId) WHERE IsDeleted = 0;
```

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `Id` | UNIQUEIDENTIFIER | No | PK, generated client-side |
| `UserId` | UNIQUEIDENTIFIER | No | FK → Users.Id |
| `Name` | NVARCHAR(200) | No | Deck title |
| `Description` | NVARCHAR(1000) | Yes | Optional description |
| `CoverImageUrl` | NVARCHAR(500) | Yes | Optional image URL |
| `IsDeleted` | BIT | No | Soft delete flag |
| `CreatedAt` | DATETIME2 | No | UTC |
| `UpdatedAt` | DATETIME2 | No | UTC — used for sync delta |

---

### Table 3: Flashcards

**Purpose**: Core learning unit. Stores card content and SM-2 scheduling state.

```sql
CREATE TABLE Flashcards (
    -- Identity
    Id                UNIQUEIDENTIFIER  PRIMARY KEY DEFAULT NEWID(),
    UserId            UNIQUEIDENTIFIER  NOT NULL,
    DeckId            UNIQUEIDENTIFIER  NOT NULL,

    -- Card Content
    FrontText         NVARCHAR(MAX)     NOT NULL,
    BackText          NVARCHAR(MAX)     NOT NULL,
    ExampleText       NVARCHAR(MAX)     NULL,
    ImageUrl          NVARCHAR(500)     NULL,
    AudioUrl          NVARCHAR(500)     NULL,

    -- SM-2 State (CLIENT-CALCULATED — server stores only)
    Repetition        INT               NOT NULL DEFAULT 0,
    IntervalDays      INT               NOT NULL DEFAULT 1,
    EaseFactor        FLOAT             NOT NULL DEFAULT 2.5,
    NextReviewDate    DATETIME2         NOT NULL DEFAULT GETUTCDATE(),
    FailCount         INT               NOT NULL DEFAULT 0,
    TotalReviews      INT               NOT NULL DEFAULT 0,

    -- Metadata
    IsDeleted         BIT               NOT NULL DEFAULT 0,
    CreatedAt         DATETIME2         NOT NULL DEFAULT GETUTCDATE(),
    UpdatedAt         DATETIME2         NOT NULL DEFAULT GETUTCDATE(),

    -- Constraints
    CONSTRAINT FK_Flashcards_Users FOREIGN KEY (UserId) REFERENCES Users(Id),
    CONSTRAINT FK_Flashcards_Decks FOREIGN KEY (DeckId) REFERENCES Decks(Id),
    CONSTRAINT CK_Flashcards_EF   CHECK (EaseFactor >= 1.3),
    CONSTRAINT CK_Flashcards_Rep  CHECK (Repetition >= 0),
    CONSTRAINT CK_Flashcards_Int  CHECK (IntervalDays >= 1)
);

-- Indexes
CREATE INDEX IX_Flashcards_UserDeck
    ON Flashcards(UserId, DeckId) WHERE IsDeleted = 0;

CREATE INDEX IX_Flashcards_NextReview
    ON Flashcards(UserId, NextReviewDate) WHERE IsDeleted = 0;

CREATE INDEX IX_Flashcards_UpdatedAt
    ON Flashcards(UserId, UpdatedAt);
```

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `Id` | UNIQUEIDENTIFIER | No | PK, generated client-side |
| `UserId` | UNIQUEIDENTIFIER | No | FK → Users.Id (data scoping) |
| `DeckId` | UNIQUEIDENTIFIER | No | FK → Decks.Id |
| `FrontText` | NVARCHAR(MAX) | No | Question / Word |
| `BackText` | NVARCHAR(MAX) | No | Answer / Definition |
| `ExampleText` | NVARCHAR(MAX) | Yes | Contextual example |
| `ImageUrl` | NVARCHAR(500) | Yes | Optional image |
| `AudioUrl` | NVARCHAR(500) | Yes | Optional TTS audio |
| `Repetition` | INT | No | SM-2: n (count of successful reviews) |
| `IntervalDays` | INT | No | SM-2: I(n) in days |
| `EaseFactor` | FLOAT | No | SM-2: EF (≥ 1.3) |
| `NextReviewDate` | DATETIME2 | No | SM-2: next scheduled review |
| `FailCount` | INT | No | Consecutive failures — triggers AI adaptive at ≥ 3 |
| `TotalReviews` | INT | No | Lifetime review count |
| `IsDeleted` | BIT | No | Soft delete flag |
| `CreatedAt` | DATETIME2 | No | UTC |
| `UpdatedAt` | DATETIME2 | No | UTC — sync delta key |

> [!IMPORTANT]
> SM-2 columns (`Repetition`, `IntervalDays`, `EaseFactor`, `NextReviewDate`) are **client-calculated**.
> The server validates bounds but **never** recalculates these values.

---

### Table 4: ReviewLogs

**Purpose**: Append-only review history for statistics and AI adaptive analysis.

```sql
CREATE TABLE ReviewLogs (
    Id                UNIQUEIDENTIFIER  PRIMARY KEY DEFAULT NEWID(),
    UserId            UNIQUEIDENTIFIER  NOT NULL,
    FlashcardId       UNIQUEIDENTIFIER  NOT NULL,
    Quality           INT               NOT NULL,
    ResponseTimeMs    BIGINT            NULL,
    ReviewedAt        DATETIME2         NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT FK_ReviewLogs_Users FOREIGN KEY (UserId) REFERENCES Users(Id),
    CONSTRAINT FK_ReviewLogs_Cards FOREIGN KEY (FlashcardId) REFERENCES Flashcards(Id),
    CONSTRAINT CK_ReviewLogs_Quality CHECK (Quality IN (0, 2, 3, 5))
);

-- Indexes
CREATE INDEX IX_ReviewLogs_FlashcardId ON ReviewLogs(FlashcardId);
CREATE INDEX IX_ReviewLogs_UserDate ON ReviewLogs(UserId, ReviewedAt);
```

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `Id` | UNIQUEIDENTIFIER | No | PK |
| `UserId` | UNIQUEIDENTIFIER | No | FK → Users.Id |
| `FlashcardId` | UNIQUEIDENTIFIER | No | FK → Flashcards.Id |
| `Quality` | INT | No | SM-2 quality: 0 (Học lại), 2 (Khó), 3 (Tốt), 5 (Dễ) |
| `ResponseTimeMs` | BIGINT | Yes | Time from card display to response tap (ms) |
| `ReviewedAt` | DATETIME2 | No | UTC timestamp of review |

> [!NOTE]
> ReviewLogs is **append-only**. No updates, no deletes. Sync is push-only (client → server).

---

### Table 5: AiChatHistory

**Purpose**: Persist AI Tutor conversation history for session continuity.

```sql
CREATE TABLE AiChatHistory (
    Id                UNIQUEIDENTIFIER  PRIMARY KEY DEFAULT NEWID(),
    UserId            UNIQUEIDENTIFIER  NOT NULL,
    SessionId         UNIQUEIDENTIFIER  NOT NULL,
    Role              NVARCHAR(20)      NOT NULL,
    Content           NVARCHAR(MAX)     NOT NULL,
    CreatedAt         DATETIME2         NOT NULL DEFAULT GETUTCDATE(),

    CONSTRAINT FK_AiChat_Users FOREIGN KEY (UserId) REFERENCES Users(Id),
    CONSTRAINT CK_AiChat_Role CHECK (Role IN ('user', 'assistant'))
);

-- Indexes
CREATE INDEX IX_AiChat_UserSession ON AiChatHistory(UserId, SessionId, CreatedAt);
```

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `Id` | UNIQUEIDENTIFIER | No | PK |
| `UserId` | UNIQUEIDENTIFIER | No | FK → Users.Id |
| `SessionId` | UNIQUEIDENTIFIER | No | Groups messages in one conversation |
| `Role` | NVARCHAR(20) | No | 'user' or 'assistant' |
| `Content` | NVARCHAR(MAX) | No | Message body |
| `CreatedAt` | DATETIME2 | No | UTC |

---

### Table 6: SyncMetadata

**Purpose**: Track pending sync operations from each client. Used for delta sync coordination.

```sql
CREATE TABLE SyncMetadata (
    Id                UNIQUEIDENTIFIER  PRIMARY KEY DEFAULT NEWID(),
    UserId            UNIQUEIDENTIFIER  NOT NULL,
    EntityType        NVARCHAR(50)      NOT NULL,
    EntityId          UNIQUEIDENTIFIER  NOT NULL,
    Action            NVARCHAR(20)      NOT NULL,
    UpdatedAt         DATETIME2         NOT NULL DEFAULT GETUTCDATE(),
    IsSynced          BIT               NOT NULL DEFAULT 0,

    CONSTRAINT FK_Sync_Users FOREIGN KEY (UserId) REFERENCES Users(Id),
    CONSTRAINT CK_Sync_Action CHECK (Action IN ('CREATE', 'UPDATE', 'DELETE')),
    CONSTRAINT CK_Sync_Entity CHECK (EntityType IN ('deck', 'flashcard', 'review_log', 'ai_chat'))
);

-- Indexes
CREATE INDEX IX_Sync_UserPending ON SyncMetadata(UserId, IsSynced) WHERE IsSynced = 0;
```

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `Id` | UNIQUEIDENTIFIER | No | PK |
| `UserId` | UNIQUEIDENTIFIER | No | FK → Users.Id |
| `EntityType` | NVARCHAR(50) | No | 'deck', 'flashcard', 'review_log', 'ai_chat' |
| `EntityId` | UNIQUEIDENTIFIER | No | PK of the changed entity |
| `Action` | NVARCHAR(20) | No | 'CREATE', 'UPDATE', or 'DELETE' |
| `UpdatedAt` | DATETIME2 | No | Timestamp of the change |
| `IsSynced` | BIT | No | 0 = pending, 1 = synced |

---

### Entity-Relationship Diagram

```
┌──────────┐
│  Users   │
│  (PK: Id)│
└────┬─────┘
     │ 1
     ├────────────────────────────┬──────────────────┬──────────────────┐
     │ N                         │ N                │ N                │ N
┌────┴─────┐              ┌──────┴───────┐   ┌─────┴────────┐  ┌─────┴──────────┐
│  Decks   │              │ ReviewLogs   │   │AiChatHistory │  │ SyncMetadata   │
│(FK:UserId│              │(FK:UserId,   │   │(FK:UserId)   │  │(FK:UserId)     │
│ PK: Id)  │              │ FK:FlashcardId│  └──────────────┘  └────────────────┘
└────┬─────┘              └──────────────┘
     │ 1
     │ N
┌────┴──────────┐
│  Flashcards   │
│(FK:UserId,    │
│ FK:DeckId,    │
│ PK: Id)       │
│               │
│ SM-2 fields:  │
│  Repetition   │
│  IntervalDays │
│  EaseFactor   │
│  NextReviewDate│
│  FailCount    │
└───────────────┘
```

### Index Summary

| Table | Index | Columns | Filter | Purpose |
|---|---|---|---|---|
| Decks | IX_Decks_UserId | UserId | IsDeleted = 0 | List user's decks (filtered) |
| Flashcards | IX_Flashcards_UserDeck | UserId, DeckId | IsDeleted = 0 | List cards in a deck |
| Flashcards | IX_Flashcards_NextReview | UserId, NextReviewDate | IsDeleted = 0 | Daily study queue query |
| Flashcards | IX_Flashcards_UpdatedAt | UserId, UpdatedAt | — | Delta sync pull |
| ReviewLogs | IX_ReviewLogs_FlashcardId | FlashcardId | — | AI adaptive analysis per card |
| ReviewLogs | IX_ReviewLogs_UserDate | UserId, ReviewedAt | — | Statistics by date range |
| AiChatHistory | IX_AiChat_UserSession | UserId, SessionId, CreatedAt | — | Load conversation in order |
| SyncMetadata | IX_Sync_UserPending | UserId, IsSynced | IsSynced = 0 | Find pending changes |

---

## Part B: Offline-First Sync Strategy

### Sync Principles

| # | Principle |
|---|---|
| 1 | **Mobile is source of SM-2 calculation** — server never recomputes SM-2 |
| 2 | **Server is source of truth for persistence** — canonical copy for recovery |
| 3 | **Writes go to Room first, then sync** — user never waits for network |
| 4 | **Timestamp-based delta sync** — only transfer changes since last sync |
| 5 | **Last-write-wins** — compare `UpdatedAt` to resolve conflicts |

### Push Flow (Client → Server)

```
CLIENT (Android)                              SERVER (API)
────────────────                              ────────────

1. Collect pending changes:
   SELECT * FROM SyncMetadata
   WHERE IsSynced = 0
   ORDER BY UpdatedAt ASC

2. For each change, load full entity:
   - deck → Decks table
   - flashcard → Flashcards table
   - review_log → ReviewLogs table

3. Build push payload:
   {
     "changes": [
       {
         "entityType": "flashcard",
         "entityId": "abc-123",
         "action": "UPDATE",
         "updatedAt": "2026-03-16T08:30:00Z",
         "data": { ...full entity fields }
       },
       ...
     ]
   }

4. POST /api/sync/push
                                    ──→ 5. For each change:
                                          a. Find existing record by Id
                                          b. If no conflict (server.UpdatedAt
                                             <= client.UpdatedAt):
                                               Apply change to SQL Server
                                          c. If conflict (server.UpdatedAt
                                             > client.UpdatedAt):
                                               Add to conflicts[]
                                          d. If action = CREATE & not exists:
                                               INSERT into SQL Server

                                    ←── 6. Return response:
                                          {
                                            "accepted": ["id1","id2"],
                                            "conflicts": [
                                              {
                                                "entityId": "xyz",
                                                "serverVersion": {...},
                                                "resolution": "server_wins"
                                              }
                                            ]
                                          }

7. Mark accepted as synced:
   UPDATE SyncMetadata
   SET IsSynced = 1
   WHERE EntityId IN (accepted[])

8. Apply conflict resolutions:
   Overwrite local entity with
   serverVersion data
```

### Pull Flow (Server → Client)

```
CLIENT                                        SERVER
──────                                        ──────

1. GET /api/sync/pull?since=2026-03-16T00:00:00Z

                                    ──→ 2. Query all entities for this user
                                          WHERE UpdatedAt > @since
                                          From: Decks, Flashcards, ReviewLogs

                                    ←── 3. Return:
                                          {
                                            "changes": [
                                              {
                                                "entityType": "flashcard",
                                                "entityId": "def-456",
                                                "action": "UPDATE",
                                                "updatedAt": "...",
                                                "data": {...}
                                              }
                                            ],
                                            "syncTimestamp": "2026-03-16T08:35:00Z"
                                          }

4. Apply remote changes to Room:
   - CREATE → INSERT into Room
   - UPDATE → UPDATE Room entity
   - DELETE → Set IsDeleted = 1 in Room

5. Save syncTimestamp locally
   for next pull request
```

### Conflict Resolution Matrix

| Scenario | Resolution | Justification |
|---|---|---|
| Both sides updated same entity | **Last-write-wins** (later `UpdatedAt`) | Simple, predictable. SM-2 is single-user so conflicts are rare. |
| Client creates, server has same ID | Accept client version (nearly impossible UUID collision) | UUIDs are globally unique |
| Client updates, server deleted | **Server delete wins** | Deletes are intentional and irreversible |
| Client deletes, server updated | **Client delete wins** | User explicitly chose to delete |
| ReviewLog conflict | **Never happens** — append-only, no updates | Logs only support INSERT |

---

## Part C: REST API Contract

### Base Configuration

| Setting | Value |
|---|---|
| **Base URL** | `https://api.smartflash.app/api` |
| **Content-Type** | `application/json` |
| **Auth** | Bearer JWT token in `Authorization` header |
| **Pagination** | Cursor-based: `?cursor={lastId}&limit={n}` (default limit = 50, max = 200) |
| **Timestamps** | ISO 8601 UTC: `2026-03-16T08:30:00Z` |
| **IDs** | UUID string: `"550e8400-e29b-41d4-a716-446655440000"` |

### Standard Error Response

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Human-readable error description",
    "details": [
      { "field": "email", "message": "Email is already in use" }
    ]
  }
}
```

### Error Codes

| HTTP Status | Code | Meaning |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Invalid input data |
| 401 | `UNAUTHORIZED` | Missing or invalid JWT |
| 403 | `FORBIDDEN` | Valid JWT but insufficient permissions |
| 404 | `NOT_FOUND` | Resource does not exist |
| 409 | `CONFLICT` | Sync conflict or duplicate |
| 429 | `RATE_LIMITED` | AI usage limit exceeded |
| 500 | `INTERNAL_ERROR` | Server error |

---

### API Group 1: Authentication

#### `POST /api/auth/register`

| Field | Value |
|---|---|
| **Auth** | None |
| **Purpose** | Create new user account |

**Request**:
```json
{
  "email": "student@example.com",
  "password": "SecurePass123!",
  "displayName": "Nguyen Van A"
}
```

**Response** `201 Created`:
```json
{
  "userId": "uuid-string",
  "accessToken": "jwt-string",
  "refreshToken": "refresh-token-string",
  "expiresIn": 3600
}
```

**Errors**: `400` (invalid email/password format), `409` (email already exists)

---

#### `POST /api/auth/login`

| Field | Value |
|---|---|
| **Auth** | None |
| **Purpose** | Authenticate and receive tokens |

**Request**:
```json
{
  "email": "student@example.com",
  "password": "SecurePass123!"
}
```

**Response** `200 OK`:
```json
{
  "userId": "uuid-string",
  "accessToken": "jwt-string",
  "refreshToken": "refresh-token-string",
  "expiresIn": 3600
}
```

**Errors**: `401` (invalid credentials)

---

#### `POST /api/auth/refresh`

| Field | Value |
|---|---|
| **Auth** | None (refresh token in body) |
| **Purpose** | Exchange refresh token for new access token |

**Request**:
```json
{
  "refreshToken": "refresh-token-string"
}
```

**Response** `200 OK`:
```json
{
  "accessToken": "new-jwt-string",
  "refreshToken": "new-refresh-token-string",
  "expiresIn": 3600
}
```

**Errors**: `401` (invalid or expired refresh token)

---

### API Group 2: Decks

#### `GET /api/decks?cursor={lastId}&limit={n}`

| Field | Value |
|---|---|
| **Auth** | JWT required |
| **Purpose** | List user's decks with card counts |

**Response** `200 OK`:
```json
{
  "data": [
    {
      "id": "uuid",
      "name": "English Vocabulary",
      "description": "IELTS words",
      "coverImageUrl": "https://...",
      "cardCount": 120,
      "dueCount": 15,
      "createdAt": "2026-03-01T10:00:00Z",
      "updatedAt": "2026-03-16T08:00:00Z"
    }
  ],
  "nextCursor": "uuid-of-last-item",
  "hasMore": true
}
```

---

#### `POST /api/decks`

| Field | Value |
|---|---|
| **Auth** | JWT required |
| **Purpose** | Create a new deck |

**Request**:
```json
{
  "id": "client-generated-uuid",
  "name": "Biology Chapter 5",
  "description": "Cell division concepts",
  "coverImageUrl": null
}
```

**Response** `201 Created`:
```json
{
  "id": "client-generated-uuid",
  "name": "Biology Chapter 5",
  "description": "Cell division concepts",
  "coverImageUrl": null,
  "cardCount": 0,
  "dueCount": 0,
  "createdAt": "2026-03-16T08:30:00Z",
  "updatedAt": "2026-03-16T08:30:00Z"
}
```

---

#### `PUT /api/decks/{id}`

| Field | Value |
|---|---|
| **Auth** | JWT required |
| **Purpose** | Update deck name/description/cover |

**Request**:
```json
{
  "name": "Biology Chapter 5 — Updated",
  "description": "Cell division and mitosis",
  "coverImageUrl": "https://..."
}
```

**Response** `200 OK`: Updated deck object (same shape as POST response)

**Errors**: `404` (deck not found or not owned by user)

---

#### `DELETE /api/decks/{id}`

| Field | Value |
|---|---|
| **Auth** | JWT required |
| **Purpose** | Soft-delete a deck and all its cards |

**Response** `204 No Content`

**Errors**: `404` (deck not found)

---

### API Group 3: Flashcards

#### `GET /api/decks/{deckId}/cards?cursor={lastId}&limit={n}`

| Field | Value |
|---|---|
| **Auth** | JWT required |
| **Purpose** | List cards in a deck |

**Response** `200 OK`:
```json
{
  "data": [
    {
      "id": "uuid",
      "deckId": "deck-uuid",
      "frontText": "Mitosis",
      "backText": "Cell division producing two identical cells",
      "exampleText": "Skin cells reproduce through mitosis",
      "imageUrl": null,
      "audioUrl": null,
      "repetition": 3,
      "intervalDays": 15,
      "easeFactor": 2.6,
      "nextReviewDate": "2026-03-20T00:00:00Z",
      "failCount": 0,
      "totalReviews": 5,
      "createdAt": "2026-03-01T10:00:00Z",
      "updatedAt": "2026-03-15T14:00:00Z"
    }
  ],
  "nextCursor": "uuid",
  "hasMore": false
}
```

---

#### `POST /api/cards`

| Field | Value |
|---|---|
| **Auth** | JWT required |
| **Purpose** | Create a new flashcard |

**Request**:
```json
{
  "id": "client-generated-uuid",
  "deckId": "deck-uuid",
  "frontText": "Photosynthesis",
  "backText": "Process by which plants convert sunlight to energy",
  "exampleText": "Leaves are green because of chlorophyll used in photosynthesis",
  "imageUrl": null,
  "audioUrl": null,
  "repetition": 0,
  "intervalDays": 1,
  "easeFactor": 2.5,
  "nextReviewDate": "2026-03-16T00:00:00Z",
  "failCount": 0,
  "totalReviews": 0
}
```

**Response** `201 Created`: Full flashcard object

---

#### `PUT /api/cards/{id}`

| Field | Value |
|---|---|
| **Auth** | JWT required |
| **Purpose** | Update card content AND/OR SM-2 state |

**Request**: Same shape as POST (all fields, including SM-2 fields)

**Response** `200 OK`: Updated flashcard object

> [!IMPORTANT]
> The server validates SM-2 bounds (`EaseFactor >= 1.3`, `IntervalDays >= 1`, `Repetition >= 0`) but does **not** recalculate them. Values come from the client's SM-2 engine.

---

#### `DELETE /api/cards/{id}`

| Field | Value |
|---|---|
| **Auth** | JWT required |
| **Purpose** | Soft-delete a flashcard |

**Response** `204 No Content`

---

### API Group 4: Reviews

#### `POST /api/reviews`

| Field | Value |
|---|---|
| **Auth** | JWT required |
| **Purpose** | Log a review event (append-only) |

**Request**:
```json
{
  "id": "client-generated-uuid",
  "flashcardId": "card-uuid",
  "quality": 3,
  "responseTimeMs": 4200,
  "reviewedAt": "2026-03-16T08:35:00Z"
}
```

**Response** `201 Created`:
```json
{
  "id": "uuid",
  "flashcardId": "card-uuid",
  "quality": 3,
  "responseTimeMs": 4200,
  "reviewedAt": "2026-03-16T08:35:00Z"
}
```

---

### API Group 5: Sync

#### `POST /api/sync/push`

| Field | Value |
|---|---|
| **Auth** | JWT required |
| **Purpose** | Push local changes to server |

**Request**:
```json
{
  "changes": [
    {
      "entityType": "flashcard",
      "entityId": "uuid",
      "action": "UPDATE",
      "updatedAt": "2026-03-16T08:30:00Z",
      "data": {
        "frontText": "Updated front",
        "backText": "Updated back",
        "repetition": 4,
        "intervalDays": 21,
        "easeFactor": 2.7,
        "nextReviewDate": "2026-04-06T00:00:00Z",
        "failCount": 0,
        "totalReviews": 6
      }
    },
    {
      "entityType": "review_log",
      "entityId": "uuid",
      "action": "CREATE",
      "updatedAt": "2026-03-16T08:30:00Z",
      "data": {
        "flashcardId": "card-uuid",
        "quality": 3,
        "responseTimeMs": 3500,
        "reviewedAt": "2026-03-16T08:30:00Z"
      }
    }
  ]
}
```

**Response** `200 OK`:
```json
{
  "accepted": ["uuid-1", "uuid-2"],
  "conflicts": [
    {
      "entityType": "flashcard",
      "entityId": "uuid-3",
      "resolution": "server_wins",
      "serverVersion": { "...full entity..." },
      "serverUpdatedAt": "2026-03-16T08:31:00Z"
    }
  ]
}
```

---

#### `GET /api/sync/pull?since={ISO8601_timestamp}`

| Field | Value |
|---|---|
| **Auth** | JWT required |
| **Purpose** | Pull server changes since last sync |

**Response** `200 OK`:
```json
{
  "changes": [
    {
      "entityType": "deck",
      "entityId": "uuid",
      "action": "UPDATE",
      "updatedAt": "2026-03-16T08:32:00Z",
      "data": { "...full deck object..." }
    },
    {
      "entityType": "flashcard",
      "entityId": "uuid",
      "action": "DELETE",
      "updatedAt": "2026-03-16T08:33:00Z",
      "data": null
    }
  ],
  "syncTimestamp": "2026-03-16T08:35:00Z"
}
```

---

### API Group 6: AI (Proxy)

> All AI endpoints return HTTP `429` with `retryAfterSeconds` when free-tier limit is exceeded.

#### Rate Limits

| Tier | Text Gen | PDF/DOCX | Example Gen | Image Gen | Tutor Chat | Total/Day |
|---|---|---|---|---|---|---|
| **Free** | 20 req | 5 files | 20 req | ❌ | 10 msg | ~55 |
| **Premium** | Unlimited | Unlimited | Unlimited | Unlimited | Unlimited | Unlimited |

**429 Response**:
```json
{
  "error": {
    "code": "RATE_LIMITED",
    "message": "Bạn đã hết lượt AI hôm nay. Thử lại vào ngày mai.",
    "retryAfterSeconds": 43200,
    "usage": { "used": 20, "limit": 20, "resetsAt": "2026-03-17T00:00:00Z" }
  }
}
```

---

#### `POST /api/ai/flashcards/text`

| Field | Value |
|---|---|
| **Auth** | JWT required |
| **Purpose** | Generate flashcard drafts from raw text |

**Request**:
```json
{
  "text": "Photosynthesis is the process by which plants convert...",
  "language": "vi",
  "maxCards": 10
}
```

**Response** `200 OK`:
```json
{
  "drafts": [
    {
      "frontText": "Quang hợp là gì?",
      "backText": "Quá trình thực vật chuyển đổi ánh sáng mặt trời thành năng lượng",
      "exampleText": "Lá cây có màu xanh nhờ chất diệp lục dùng trong quang hợp"
    }
  ],
  "usage": { "used": 5, "limit": 20 }
}
```

---

#### `POST /api/ai/flashcards/pdf`

| Field | Value |
|---|---|
| **Auth** | JWT required |
| **Content-Type** | `multipart/form-data` |
| **Purpose** | Generate flashcard drafts from PDF |

**Request**: Form fields: `file` (PDF binary), `language` ("vi"/"en"), `maxCards` (int)

**Response** `200 OK`: Same shape as `/flashcards/text` response

---

#### `POST /api/ai/flashcards/docx`

Same as PDF endpoint but accepts `.docx` files.

---

#### `POST /api/ai/example`

| Field | Value |
|---|---|
| **Auth** | JWT required |
| **Purpose** | Generate example sentence for a card |

**Request**:
```json
{
  "frontText": "Ambiguous",
  "backText": "Having more than one meaning; unclear",
  "language": "en"
}
```

**Response** `200 OK`:
```json
{
  "example": "The politician's ambiguous statement left reporters confused about his actual position.",
  "usage": { "used": 12, "limit": 20 }
}
```

---

#### `POST /api/ai/image`

| Field | Value |
|---|---|
| **Auth** | JWT required (Premium only) |
| **Purpose** | Generate an image for a card |

**Request**:
```json
{
  "frontText": "Mitosis",
  "backText": "Cell division producing two identical cells",
  "style": "educational_diagram"
}
```

**Response** `200 OK`:
```json
{
  "imageUrl": "https://storage.smartflash.app/images/uuid.png"
}
```

**Errors**: `403` (free-tier user)

---

#### `POST /api/ai/quiz`

| Field | Value |
|---|---|
| **Auth** | JWT required |
| **Purpose** | Generate multiple-choice questions from cards |

**Request**:
```json
{
  "cards": [
    { "id": "uuid", "frontText": "...", "backText": "..." },
    { "id": "uuid", "frontText": "...", "backText": "..." }
  ],
  "questionCount": 10,
  "language": "vi"
}
```

**Response** `200 OK`:
```json
{
  "questions": [
    {
      "questionText": "Quang hợp là gì?",
      "options": [
        "Quá trình chuyển đổi ánh sáng thành năng lượng",
        "Quá trình hô hấp tế bào",
        "Quá trình phân bào",
        "Quá trình trao đổi khí"
      ],
      "correctIndex": 0,
      "sourceCardId": "uuid"
    }
  ]
}
```

---

#### `POST /api/ai/tutor`

| Field | Value |
|---|---|
| **Auth** | JWT required |
| **Purpose** | AI Tutor chat (streamed response via SSE) |

**Request**:
```json
{
  "sessionId": "uuid",
  "message": "Giải thích từ 'ambiguous' cho em",
  "language": "vi",
  "history": [
    { "role": "user", "content": "previous message" },
    { "role": "assistant", "content": "previous response" }
  ]
}
```

**Response** `200 OK` (Server-Sent Events stream):
```
data: {"token": "Từ "}
data: {"token": "'ambiguous' "}
data: {"token": "có nghĩa "}
data: {"token": "là mơ hồ, "}
data: {"token": "không rõ ràng."}
data: {"done": true, "usage": {"used": 8, "limit": 10}}
```

---

#### `POST /api/ai/adaptive`

| Field | Value |
|---|---|
| **Auth** | JWT required |
| **Purpose** | Get adaptive learning hints for a struggling card |

**Request**:
```json
{
  "flashcard": {
    "frontText": "Mitochondria",
    "backText": "Powerhouse of the cell",
    "exampleText": null,
    "failCount": 4
  },
  "recentReviews": [
    { "quality": 0, "responseTimeMs": 8000, "reviewedAt": "..." },
    { "quality": 0, "responseTimeMs": 7500, "reviewedAt": "..." },
    { "quality": 2, "responseTimeMs": 6000, "reviewedAt": "..." }
  ],
  "language": "vi"
}
```

**Response** `200 OK`:
```json
{
  "hint": {
    "simplifiedExplanation": "Ty thể giống như nhà máy điện bên trong tế bào. Nó tạo ra năng lượng (ATP) để tế bào hoạt động.",
    "additionalExamples": [
      "Giống như pin cung cấp năng lượng cho đồ chơi, ty thể cung cấp năng lượng cho tế bào.",
      "Khi bạn chạy bộ, ty thể trong tế bào cơ tạo ra nhiều ATP hơn để bạn có sức."
    ],
    "splitSuggestion": {
      "suggested": true,
      "cards": [
        { "front": "Ty thể là gì?", "back": "Bào quan tạo năng lượng cho tế bào" },
        { "front": "Ty thể tạo ra chất gì?", "back": "ATP (adenosine triphosphate)" }
      ]
    }
  },
  "usage": { "used": 3, "limit": 20 }
}
```

---

## Part D: API Summary Table

| Method | Endpoint | Auth | Request | Response | Notes |
|---|---|---|---|---|---|
| POST | `/api/auth/register` | No | email, password, displayName | tokens + userId | Creates account |
| POST | `/api/auth/login` | No | email, password | tokens + userId | Authenticates |
| POST | `/api/auth/refresh` | No | refreshToken | new tokens | Token renewal |
| GET | `/api/decks` | JWT | cursor, limit | Deck[] | Paginated list |
| POST | `/api/decks` | JWT | Deck body (client ID) | Deck | Create with client UUID |
| PUT | `/api/decks/{id}` | JWT | Deck fields | Deck | Partial update |
| DELETE | `/api/decks/{id}` | JWT | — | 204 | Soft delete |
| GET | `/api/decks/{deckId}/cards` | JWT | cursor, limit | Flashcard[] | Cards in deck |
| POST | `/api/cards` | JWT | Card body (client ID) | Flashcard | Create with SM-2 defaults |
| PUT | `/api/cards/{id}` | JWT | Card fields + SM-2 | Flashcard | SM-2 state from client |
| DELETE | `/api/cards/{id}` | JWT | — | 204 | Soft delete |
| POST | `/api/reviews` | JWT | Review body | ReviewLog | Append-only |
| POST | `/api/sync/push` | JWT | changes[] | accepted[], conflicts[] | Delta sync push |
| GET | `/api/sync/pull` | JWT | since (timestamp) | changes[], syncTimestamp | Delta sync pull |
| POST | `/api/ai/flashcards/text` | JWT | text, language, maxCards | drafts[] | AI generation |
| POST | `/api/ai/flashcards/pdf` | JWT | file (multipart) | drafts[] | PDF → cards |
| POST | `/api/ai/flashcards/docx` | JWT | file (multipart) | drafts[] | DOCX → cards |
| POST | `/api/ai/example` | JWT | frontText, backText | example string | Single example |
| POST | `/api/ai/image` | JWT | frontText, backText | imageUrl | Premium only |
| POST | `/api/ai/quiz` | JWT | cards[], count | questions[] | MCQ generation |
| POST | `/api/ai/tutor` | JWT | message, history, sessionId | SSE stream | AI chat |
| POST | `/api/ai/adaptive` | JWT | flashcard, reviews | hint object | Adaptive hints |

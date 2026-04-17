# Phase 2: System Architecture Design
## Smart Flashcard Mobile Application

> **Document Version**: 1.0
> **Date**: 2026-03-16
> **Status**: Ready for Review
> **Prerequisite**: Phase 1 — Requirement Analysis (completed)
> **Next Phase**: Phase 3 — Database Schema Design + API Contract

---

## 1. High-Level System Architecture

### 1.1 Architecture Overview

The system follows a **Client-Server** architecture with an **offline-first** Android client communicating with a centralized backend through REST APIs. AI capabilities are delivered through a dedicated orchestration layer on the backend, which proxies requests to external AI providers.

```
┌────────────────────────────────────────────────────────────────┐
│                     ANDROID MOBILE APP                        │
│                     (Offline-First Client)                    │
│                                                               │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐              │
│  │ Presentation│  │   Domain   │  │    Data    │              │
│  │   (Compose) │→ │(Use Cases) │→ │ (Room+API) │              │
│  └────────────┘  └────────────┘  └────────────┘              │
│                                       │                       │
│                              Room (SQLite)                    │
│                          [Offline-first cache]                │
└────────────────────────┬──────────────────────────────────────┘
                         │ HTTPS / REST (JSON)
                         │ JWT Authentication
                         ▼
┌────────────────────────────────────────────────────────────────┐
│                      BACKEND API SERVER                       │
│                  (ASP.NET Core Web API)                       │
│                                                               │
│  ┌──────────┐  ┌──────────────┐  ┌────────────────┐          │
│  │ Auth     │  │ Business     │  │ AI Orchestration│          │
│  │ Module   │  │ Logic Layer  │  │ Layer           │          │
│  └──────────┘  └──────────────┘  └───────┬────────┘          │
│                       │                   │                   │
│              ┌────────┴────────┐    ┌─────┴──────┐            │
│              │  Sync Engine   │    │ Rate Limiter│            │
│              └────────┬────────┘    └─────┬──────┘            │
│                       │                   │                   │
└───────────────────────┼───────────────────┼───────────────────┘
                        │                   │
               ┌────────┴────────┐   ┌──────┴──────────┐
               │  SQL Server    │   │ AI Provider API  │
               │  (SSMS)        │   │ (OpenAI / Gemini)│
               │                │   │                  │
               │ - Users        │   │ - Text → Cards   │
               │ - Decks        │   │ - Chat (Tutor)   │
               │ - Flashcards   │   │ - Image Gen      │
               │ - ReviewLogs   │   │ - Adaptive hints │
               │ - SyncMetadata │   └──────────────────┘
               │ - AiChatHistory│
               └────────────────┘
```

### 1.2 Tier Responsibilities

| Tier | Role | Key Responsibilities |
|---|---|---|
| **Android App** | Offline-first client | SM-2 computation, local storage, UI rendering, TTS playback, study queue generation. All core study features work without network. |
| **Backend API** | Central coordinator | User authentication, data persistence in SQL Server, AI request orchestration, sync conflict resolution, rate limiting. |
| **AI Services** | Intelligence layer | Flashcard generation, tutor chat, adaptive analysis, image generation. Accessed exclusively through the backend proxy — never called directly from the client. |
| **SQL Server** | Source of truth (cloud) | Persistent storage for all user data. The canonical copy when sync conflicts arise. |
| **Room (SQLite)** | Local cache | Offline copy of user data on the device. Writes go to Room first, then sync to server. |

### 1.3 Justification

| Decision | Rationale |
|---|---|
| **Offline-first** | Students study in classrooms, subways, and areas with poor connectivity. SM-2 and core study must work without internet. (Req: NFR-P06, SYNC-01) |
| **SM-2 on client** | SM-2 calculations are latency-sensitive (user taps a button → instant feedback). Running on-device ensures < 16ms response. The server does NOT recalculate SM-2 — it only stores the results. |
| **AI behind backend proxy** | AI API keys must never be exposed in client code. Backend enforces rate limits, caches common requests, and allows provider switching without app updates. (Req: NFR-SC04, AI-14) |
| **SQL Server** | Required per project spec. Managed via SSMS. Schema designed for relational integrity with foreign keys and indexes. |
| **REST over WebSocket** | Most interactions are request-response. AI Tutor streaming uses HTTP chunked transfer (SSE) — no WebSocket complexity needed. |

### 1.4 Cross-Tier Data Flow

```
USER ACTION                     ANDROID APP                 BACKEND              AI PROVIDER
─────────────────────────────────────────────────────────────────────────────────────────────
Create card       ──→  Write to Room (local)
                       Queue sync entry
                       ──→  (when online) POST /api/flashcards  ──→  SQL Server INSERT

Study card        ──→  SM-2 compute (local)
                       Write result to Room
                       Queue sync entry
                       ──→  (when online) POST /api/reviews      ──→  SQL Server INSERT

AI Generate       ──→  POST /api/ai/generate  ──→  Extract text (PDF/DOCX)
                                                    ──→  Call AI Provider
                                              ←──  Return draft cards
                  ←──  Show drafts to user
                       User edits & saves
                       Write to Room (local)

AI Tutor Chat     ──→  POST /api/ai/tutor (SSE stream) ──→  Call AI Provider
                  ←──  Stream response tokens                  (streaming)
                       Display in chat UI

Sync (periodic)   ──→  POST /api/sync/push  { localChanges[] }
                  ←──  200 OK { conflicts[], remoteChanges[] }
                       Apply remote changes to Room
```

---

## 2. Mobile App Architecture (Android)

### 2.1 Clean Architecture + MVVM

The Android app uses **Clean Architecture** with three distinct layers, enforced by package boundaries. Dependencies flow inward: Presentation → Domain ← Data.

```
┌─────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                    │
│  ┌──────────┐  ┌──────────────┐  ┌───────────────────┐ │
│  │ Screens/ │  │ ViewModels   │  │ UI State (data    │ │
│  │ Composable│→│ (Hilt inject)│→ │ class per screen) │ │
│  └──────────┘  └──────┬───────┘  └───────────────────┘ │
│                       │ calls                           │
├───────────────────────┼─────────────────────────────────┤
│                   DOMAIN LAYER                          │
│  ┌──────────────┐  ┌──┴──────────────┐  ┌────────────┐ │
│  │ Use Cases    │  │ Repository      │  │ SM2Engine  │ │
│  │ (interactors)│  │ Interfaces      │  │ (pure algo)│ │
│  └──────────────┘  └──┬──────────────┘  └────────────┘ │
│                       │ implemented by                  │
├───────────────────────┼─────────────────────────────────┤
│                     DATA LAYER                          │
│  ┌────────────┐  ┌───┴────────────┐  ┌───────────────┐ │
│  │ Room DB    │  │ Repository     │  │ Retrofit API  │ │
│  │ (Entities, │  │ Impls          │  │ (DTOs,        │ │
│  │  DAOs)     │  │ (offline-first)│  │  Interceptors)│ │
│  └────────────┘  └────────────────┘  └───────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### 2.2 Layer Contracts

| Layer | Knows About | Does NOT Know About |
|---|---|---|
| **Presentation** | Domain models, Use Cases | Room entities, Retrofit, SQL, JSON |
| **Domain** | Domain models, Repository interfaces, SM2Engine | Android SDK, Room, Retrofit, Compose |
| **Data** | Room entities, DTOs, DAOs, API interfaces, Domain repository interfaces | ViewModels, Compose, UI State |

### 2.3 Module Breakdown

#### AUTH Module
| Component | Layer | Responsibility |
|---|---|---|
| `LoginScreen` / `RegisterScreen` | Presentation | Email/password forms, validation feedback |
| `AuthViewModel` | Presentation | Login/register state, token status |
| `LoginUseCase` / `RegisterUseCase` | Domain | Orchestrate auth flow, validate input |
| `AuthRepository` (interface) | Domain | Abstract auth operations |
| `AuthRepositoryImpl` | Data | Call AuthApi, store JWT in DataStore |
| `AuthApi` | Data | Retrofit interface: POST /auth/login, /auth/register |
| `TokenManager` | Data | Read/write/refresh JWT tokens in EncryptedDataStore |

#### DECK Module
| Component | Layer | Responsibility |
|---|---|---|
| `DeckListScreen` / `DeckDetailScreen` | Presentation | Deck grid, card list, search |
| `DeckViewModel` | Presentation | Deck list state, CRUD operations |
| `CreateDeckUseCase` / `DeleteDeckUseCase` | Domain | Deck business rules |
| `DeckRepository` (interface) | Domain | Abstract deck operations |
| `DeckRepositoryImpl` | Data | Room CRUD + sync queue |
| `DeckDao` | Data | Room queries for decks |

#### CARD Module
| Component | Layer | Responsibility |
|---|---|---|
| `FlashcardEditorScreen` | Presentation | Card create/edit form, AI buttons |
| `CardEditorViewModel` | Presentation | Card editing state |
| `CreateFlashcardUseCase` / `UpdateFlashcardUseCase` | Domain | Card business rules, default SM-2 values |
| `FlashcardRepository` (interface) | Domain | Abstract card operations |
| `FlashcardRepositoryImpl` | Data | Room CRUD + sync queue |
| `FlashcardDao` | Data | Room queries for cards |

#### STUDY Module
| Component | Layer | Responsibility |
|---|---|---|
| `StudySessionScreen` / `StudySummaryScreen` | Presentation | Flip card UI, SM-2 buttons, progress |
| `StudyViewModel` | Presentation | Session state, card queue, timer |
| `GetStudyQueueUseCase` | Domain | Build daily queue (40 new / 150 review) |
| `ReviewCardUseCase` | Domain | Call SM2Engine → update card → log review |
| `SM2Engine` | Domain | **Pure SM-2 algorithm** (stateless, no dependencies) |
| `ReviewLogRepository` (interface) | Domain | Abstract review log operations |
| `FlashcardDao.getDueCards()` | Data | Room query: WHERE nextReviewDate <= today |

#### AI Module
| Component | Layer | Responsibility |
|---|---|---|
| `AiGenerateScreen` / `AiTutorScreen` / `QuizScreen` | Presentation | Generation UI, chat UI, quiz UI |
| `AiViewModel` / `TutorViewModel` / `QuizViewModel` | Presentation | AI state, streaming, drafts |
| `GenerateFlashcardsUseCase` | Domain | Send text/file → receive drafts |
| `AiTutorUseCase` | Domain | Send message → stream response |
| `GenerateQuizUseCase` | Domain | Select cards → request MCQ |
| `AdaptiveScheduler` | Domain | Analyze fail patterns → request AI suggestions |
| `AiRepository` (interface) | Domain | Abstract AI operations |
| `AiRepositoryImpl` | Data | Retrofit calls to /api/ai/* endpoints |
| `AiApi` | Data | Retrofit interface for AI endpoints |

#### SYNC Module
| Component | Layer | Responsibility |
|---|---|---|
| Sync indicator (in HomeScreen) | Presentation | Show synced/syncing/offline badge |
| `SyncDataUseCase` | Domain | Orchestrate push/pull cycle |
| `SyncRepository` (interface) | Domain | Abstract sync operations |
| `SyncManager` | Data | Delta detection, conflict resolution, retry |
| `SyncApi` | Data | Retrofit interface for sync endpoints |
| `SyncMetadataDao` | Data | Room: offline change queue |
| `SyncWorker` (WorkManager) | Data | Background periodic sync |

#### SETTINGS Module
| Component | Layer | Responsibility |
|---|---|---|
| `SettingsScreen` | Presentation | All settings UI |
| `SettingsViewModel` | Presentation | Read/write preferences |
| `UserPreferences` | Data | DataStore for daily limits, theme, language |

### 2.4 SM-2 Placement — Core Architectural Rule

> [!IMPORTANT]
> **SM-2 is a pure, deterministic algorithm that lives exclusively in the Domain Layer.**
> It has zero dependencies on Android SDK, AI, network, or database.

```
SM2Engine (Domain Layer)
├── Input:  quality (Int), repetition (Int), interval (Int), easeFactor (Double)
├── Output: SM2Result { repetition, interval, easeFactor, nextReviewDate }
├── Dependencies: NONE (plain Kotlin)
└── Called by: ReviewCardUseCase only
```

**AI Module boundary rule**:
- AI does NOT call SM2Engine.
- AI does NOT write to SM-2 fields (repetition, interval, easeFactor, nextReviewDate).
- AI reads SM-2 outputs + review history as READ-ONLY input.
- AI outputs are always SUGGESTIONS displayed to the user — never auto-applied.

---

## 3. Backend API Architecture

### 3.1 Architecture Pattern

The backend follows a **Layered Architecture** with clear separation:

```
┌──────────────────────────────────────────────────────┐
│                  API CONTROLLERS                     │
│  AuthController │ DeckController │ FlashcardController│
│  SyncController │ AiController                       │
├──────────────────────────────────────────────────────┤
│                  SERVICE LAYER                       │
│  AuthService    │ DeckService    │ FlashcardService   │
│  SyncService    │ AiOrchestrator                     │
├──────────────────────────────────────────────────────┤
│              DATA ACCESS LAYER (DAL)                 │
│  UserRepository │ DeckRepository │ FlashcardRepository│
│  ReviewLogRepository │ SyncRepository                │
├──────────────────────────────────────────────────────┤
│              INFRASTRUCTURE                          │
│  SQL Server (EF Core / Dapper)                       │
│  AI Provider Client (HttpClient)                     │
│  JWT Token Provider                                  │
│  Rate Limiter                                        │
└──────────────────────────────────────────────────────┘
```

### 3.2 API Endpoint Groups

| Group | Base Path | Auth Required | Purpose |
|---|---|---|---|
| **Auth** | `/api/auth` | No (register/login) | User registration, login, token refresh |
| **Decks** | `/api/decks` | Yes (JWT) | CRUD operations on decks |
| **Flashcards** | `/api/flashcards` | Yes (JWT) | CRUD operations on cards |
| **Reviews** | `/api/reviews` | Yes (JWT) | Log review results |
| **Sync** | `/api/sync` | Yes (JWT) | Push/pull delta changes |
| **AI** | `/api/ai` | Yes (JWT) | Flashcard generation, tutor, quiz, adaptive |

### 3.3 JWT Authentication Flow

```
CLIENT                          BACKEND                         SQL SERVER
──────                          ───────                         ──────────
POST /api/auth/login
  { email, password }
                          ──→   Validate credentials
                                Hash comparison (bcrypt)
                          ──→   SELECT * FROM Users WHERE Email = @email
                          ←──   User record
                                Generate JWT (1h expiry)
                                Generate Refresh Token (30d)
←── 200 { accessToken,
          refreshToken,
          expiresIn }

... (token expires after 1h) ...

POST /api/auth/refresh
  { refreshToken }
                          ──→   Validate refresh token
                                Issue new JWT + refresh token
←── 200 { accessToken,
          refreshToken }
```

### 3.4 Critical Backend Rules

| Rule | Enforcement |
|---|---|
| **Backend does NOT compute SM-2** | No SM-2 logic on server. Server stores SM-2 fields as opaque data from the client. |
| **Backend validates SM-2 bounds** | Server validates: `easeFactor >= 1.3`, `interval >= 1`, `repetition >= 0`. Rejects invalid data. |
| **AI quota enforced server-side** | Rate limiter tracks `AiUsageCount` per user per day. Returns HTTP 429 when exceeded. |
| **All data scoped by userId** | Every query includes `WHERE UserId = @currentUserId`. No cross-user data access. |
| **Sync is idempotent** | Repeated sync pushes with the same data produce the same result. |

### 3.5 AI Orchestration Layer

The AI orchestration layer sits in the Service Layer and abstracts the AI provider:

```
AiOrchestrator
├── IAiProvider (interface)
│   ├── OpenAiProvider : IAiProvider
│   ├── GeminiProvider : IAiProvider
│   └── MockAiProvider : IAiProvider (for testing)
│
├── Methods:
│   ├── GenerateFlashcards(text, language, cardCount) → DraftCard[]
│   ├── GenerateFromFile(fileBytes, fileType, language) → DraftCard[]
│   ├── GenerateExample(front, back, language) → string
│   ├── GenerateImage(description) → imageUrl
│   ├── Chat(messages[], language) → streamedResponse
│   ├── GenerateQuiz(cards[], questionCount) → QuizQuestion[]
│   └── AnalyzePerformance(reviewHistory[]) → AdaptiveHint
│
├── Cross-cutting:
│   ├── RateLimiter.Check(userId, operationType) → allowed/denied
│   ├── PromptTemplateManager.Build(templateName, vars) → prompt
│   └── ResponseCache.GetOrSet(cacheKey, ttl) → cachedResult?
```

---

## 4. AI System Architecture

### 4.1 AI Module Decomposition

```
┌─────────────────────────────────────────────────────────┐
│                    AI SERVICES LAYER                    │
│                                                         │
│  ┌─────────────────────────────────────────────┐        │
│  │        AI FLASHCARD GENERATOR               │        │
│  │  Input: raw text / PDF text / DOCX text     │        │
│  │  Output: DraftCard[] { front, back, example }│       │
│  │  Trigger: User-initiated (AI Generate Screen)│       │
│  └─────────────────────────────────────────────┘        │
│                                                         │
│  ┌─────────────────────────────────────────────┐        │
│  │        AI TUTOR (CHAT)                      │        │
│  │  Input: user message + conversation history │        │
│  │  Output: streamed text response             │        │
│  │  Trigger: User-initiated (AI Tutor Screen)  │        │
│  └─────────────────────────────────────────────┘        │
│                                                         │
│  ┌─────────────────────────────────────────────┐        │
│  │        AI ADAPTIVE LEARNING ENGINE          │        │
│  │  Input: reviewHistory[], failCount, respTime│        │
│  │  Output: AdaptiveHint { explanation,        │        │
│  │          examples, splitSuggestion }         │        │
│  │  Trigger: failCount >= 3 (automatic)        │        │
│  └─────────────────────────────────────────────┘        │
│                                                         │
│  ┌─────────────────────────────────────────────┐        │
│  │        AI QUIZ GENERATOR                    │        │
│  │  Input: Flashcard[]                         │        │
│  │  Output: QuizQuestion[] { question, options, │       │
│  │          correctAnswer }                     │        │
│  │  Trigger: User-initiated (Quiz Screen)      │        │
│  └─────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────┘
```

### 4.2 AI Input / Output Specification

| AI Module | Reads (Input Data) | Produces (Output) |
|---|---|---|
| **Flashcard Generator** | Raw text, PDF extracted text, DOCX extracted text, target language | `DraftCard[] { front, back, exampleText }` — always drafts, never auto-saved |
| **AI Tutor** | User message, conversation history (last 20 messages), user's language preference | Streamed text response (friendly, educational tone) |
| **Adaptive Engine** | `reviewHistory[]` (quality scores), `failCount` per card, `avgResponseTimeMs`, card content (front, back) | `AdaptiveHint { simplifiedExplanation, additionalExamples[], splitSuggestion? }` |
| **Quiz Generator** | `Flashcard[]` from a deck (min 4 cards), target question count | `QuizQuestion[] { questionText, options[4], correctIndex }` |
| **Example Generator** | Card front, card back, language | Single example sentence string |
| **Image Generator** | Card front, card back, context description | Image URL (stored on server) |

### 4.3 Core Rule: SM-2 vs AI Boundary

> [!CAUTION]
> **SM-2 is the SOURCE OF TRUTH. AI is the DECISION SUPPORT LAYER.**

```
                    ┌──────────────────────────┐
                    │    SM-2 ENGINE            │
                    │    (Source of Truth)       │
                    │                           │
                    │  Controls:                │
                    │  ✓ repetition count       │
                    │  ✓ interval (days)        │
                    │  ✓ ease factor            │
                    │  ✓ nextReviewDate         │
                    │                           │
                    │  NOBODY else writes to    │
                    │  these fields.            │
                    └──────────┬───────────────┘
                               │ (read-only access)
                    ┌──────────┴───────────────┐
                    │    AI DECISION SUPPORT    │
                    │    (Advisory Only)        │
                    │                           │
                    │  CAN read:               │
                    │  ✓ review history         │
                    │  ✓ fail count             │
                    │  ✓ response times         │
                    │  ✓ SM-2 current state     │
                    │                           │
                    │  CAN produce:             │
                    │  ✓ explanations           │
                    │  ✓ examples               │
                    │  ✓ split suggestions      │
                    │  ✓ learning insights      │
                    │                           │
                    │  CANNOT:                  │
                    │  ✗ Override SM-2 interval  │
                    │  ✗ Change ease factor     │
                    │  ✗ Modify nextReviewDate  │
                    │  ✗ Auto-save cards        │
                    └──────────────────────────┘
```

---

## 5. SM-2 + AI Interaction Flow

### 5.1 Card Review Flow (Complete Sequence)

```
STEP  ACTOR         ACTION                                    LAYER
─────────────────────────────────────────────────────────────────────
1     User          Opens study session                       Presentation
2     System        GetStudyQueueUseCase:                     Domain
                      - Query Room: dueCards WHERE nextReview <= today
                      - Query Room: newCards WHERE repetition == 0
                      - Apply limits: max 40 new, max 150 review
                      - Shuffle and merge into queue
3     System        Display first card (front side)           Presentation
4     User          Taps card to flip                         Presentation
5     System        Show back + example + image               Presentation
6     User          Taps response: "Khó" (q=2)               Presentation

7     System        ReviewCardUseCase:                        Domain
                      7a. SM2Engine.calculate(q=2, n, I, EF)
                          → SM2Result { n', I', EF', nextDate }
                      7b. Update card in Room:
                          - repetition = n'
                          - interval = I'
                          - easeFactor = EF'
                          - nextReviewDate = nextDate
                          - failCount += (if q < 3 then 1 else 0)
                          - totalReviews += 1
                      7c. Insert ReviewLog in Room:
                          - quality, timestamp, responseTimeMs
                      7d. Queue sync entry (SyncMetadata)

8     System        CHECK: card.failCount >= 3?               Domain
                      │
                      ├─ NO → Continue to next card (STEP 3)
                      │
                      └─ YES → Trigger AdaptiveScheduler       Domain
                                8a. Collect recent review history
                                8b. Build AI analysis request
                                8c. Call AiRepository.analyzePerformance()
                                                                 Data
                                8d. Backend:
                                    - AiOrchestrator.AnalyzePerformance()
                                    - Returns AdaptiveHint
                                8e. Display hint to user:         Presentation
                                    - "Thẻ này khó với bạn. Đây là
                                      giải thích đơn giản hơn: ..."
                                    - Show additional examples
                                    - Offer "Tách thẻ" (Split card) button
                                8f. User decides:
                                    - Dismiss → continue
                                    - Split card → create sub-cards

9     System        Next card → repeat from STEP 3

10    System        All cards done → Show StudySummaryScreen   Presentation
                      - Cards studied, accuracy, time
                      - Next review forecast

11    System        Background: SyncWorker runs               Data
                      - Push review logs + card updates
                      - Pull any remote changes
```

### 5.2 AI Activation Rules

| Condition | AI Activates? | AI Action |
|---|---|---|
| User reviews any card normally | **NO** | SM-2 processes response alone |
| User taps "Học lại" (failCount < 3) | **NO** | SM-2 resets card. No AI intervention. |
| User taps "Học lại" (failCount reaches 3) | **YES** | AI generates simplified explanation + extra examples |
| User taps "Học lại" (failCount reaches 5) | **YES** | AI additionally suggests splitting the card |
| User explicitly taps "AI help" button | **YES** | AI generates on-demand help regardless of failCount |
| End of study session (≥ 20 cards reviewed) | **YES** (background) | AI analyzes session performance for weekly insights |
| User opens AI Tutor | **YES** | Independent chat, not tied to study flow |

### 5.3 What AI Can NEVER Do Automatically

- ❌ Change a card's `nextReviewDate`
- ❌ Modify a card's `easeFactor` or `interval`
- ❌ Auto-save generated flashcards without user confirmation
- ❌ Skip a card in the study queue
- ❌ Override the daily study limits

---

## 6. Database & Sync Architecture (Conceptual)

### 6.1 Dual-Database Strategy

```
ANDROID DEVICE                              CLOUD SERVER
┌───────────────────┐                       ┌───────────────────┐
│    Room (SQLite)   │  ← sync →            │   SQL Server      │
│    LOCAL COPY      │                       │   CANONICAL COPY  │
│                    │                       │                    │
│  Fast reads/writes │                       │  All users' data   │
│  Works offline     │                       │  Backup & recovery │
│  Single user       │                       │  Multi-device      │
└───────────────────┘                       └───────────────────┘
```

### 6.2 Entity Sync Groups

| Group | Entities | Sync Direction | Priority |
|---|---|---|---|
| **Core Learning** | Decks, Flashcards (incl. SM-2 fields) | Bidirectional | **Critical** — this data must never be lost |
| **Progress Data** | ReviewLogs | Push-only (client → server) | High — analytics depend on this |
| **AI Data** | AiChatHistory | Push-only (client → server) | Low — chat is ephemeral, nice to have |
| **User Profile** | Users (displayName, avatar, settings) | Bidirectional | Medium |
| **System** | SyncMetadata | Local-only (not synced) | Internal — tracking pending changes |

### 6.3 Sync Strategy: Offline-First with Last-Write-Wins

```
┌───────────────────────────────────────────────────────────┐
│                    SYNC LIFECYCLE                         │
│                                                           │
│  1. LOCAL WRITE                                           │
│     User action → Write to Room → Insert SyncMetadata     │
│     { entityType, entityId, action, timestamp, synced=0 } │
│                                                           │
│  2. SYNC TRIGGER (any of):                                │
│     • App comes to foreground                             │
│     • Network connectivity restored                       │
│     • User taps "Sync now"                                │
│     • WorkManager periodic (every 15 min)                 │
│                                                           │
│  3. PUSH PHASE                                            │
│     Collect all SyncMetadata WHERE synced = 0              │
│     POST /api/sync/push { changes[] }                     │
│     Server applies changes, returns conflicts              │
│     Mark SyncMetadata as synced = 1                        │
│                                                           │
│  4. PULL PHASE                                            │
│     GET /api/sync/pull?since={lastSyncTimestamp}           │
│     Server returns changes since last sync                 │
│     Apply remote changes to Room                          │
│                                                           │
│  5. CONFLICT RESOLUTION                                   │
│     Compare updatedAt timestamps                          │
│     Last-Write-Wins: newer timestamp prevails             │
│     Log conflict for debugging                            │
└───────────────────────────────────────────────────────────┘
```

### 6.4 Offline Change Queue

Every write to Room also inserts a `SyncMetadata` record:

```
SyncMetadata:
  - entityType: "flashcard" | "deck" | "review_log" | "user"
  - entityId: UUID of the changed entity
  - action: "CREATE" | "UPDATE" | "DELETE"
  - timestamp: device local time (ISO 8601)
  - synced: false (becomes true after successful push)
```

When the device is offline, records accumulate in SyncMetadata. When connectivity resumes, the SyncWorker processes them in chronological order.

### 6.5 Timestamp & Conflict Rules

| Scenario | Resolution |
|---|---|
| Same card edited on device A and device B | Compare `updatedAt` — the later timestamp wins |
| Card deleted on device A, edited on device B | Delete wins (delete is irreversible) |
| New card created offline on two devices with same UUID | Extremely unlikely (UUID collision). If detected, server generates a new ID for one. |
| ReviewLog conflict | Never happens — ReviewLogs are append-only, never edited |

---

## 7. Deployment & Scalability

### 7.1 Backend Deployment Architecture

```
┌──────────────────────────────────────────────────────┐
│                  PRODUCTION ENVIRONMENT              │
│                                                      │
│  ┌────────────────┐     ┌────────────────────────┐   │
│  │  Load Balancer │ ──→ │ API Server (Instance 1)│   │
│  │  (nginx/Azure) │ ──→ │ API Server (Instance 2)│   │
│  └────────────────┘     │ API Server (Instance N)│   │
│                         └──────────┬─────────────┘   │
│                                    │                 │
│              ┌─────────────────────┼───────────┐     │
│              │                     │           │     │
│  ┌───────────┴──┐  ┌──────────────┴──┐  ┌─────┴──┐  │
│  │ SQL Server   │  │ Redis Cache     │  │ Blob   │  │
│  │ (Primary +   │  │ (Rate limiting, │  │ Storage│  │
│  │  Read Replica)│  │  session cache) │  │ (Images│  │
│  └──────────────┘  └─────────────────┘  │ Audio) │  │
│                                         └────────┘  │
└──────────────────────────────────────────────────────┘
         │
         │ HTTPS
         ▼
┌──────────────────┐
│ AI Provider APIs │
│ (OpenAI / Gemini)│
└──────────────────┘
```

### 7.2 AI Provider Abstraction

The `IAiProvider` interface ensures provider-switching without app or server code changes:

```
IAiProvider
├── GenerateText(prompt, maxTokens, temperature) → string
├── GenerateTextStream(prompt, maxTokens) → IAsyncEnumerable<string>
├── GenerateImage(prompt, size) → imageBytes
│
├── Implementations:
│   ├── OpenAiProvider  → calls api.openai.com
│   ├── GeminiProvider  → calls generativelanguage.googleapis.com
│   └── MockAiProvider  → returns static test data
│
└── Selected via configuration (appsettings.json):
    "AiProvider": "OpenAi" | "Gemini" | "Mock"
```

### 7.3 AI Cost Control for Free Tier

| Mechanism | Implementation |
|---|---|
| **Server-side rate limit** | Redis counter per `userId:date:operationType`. Checked before every AI call. Returns HTTP 429 with reset time. |
| **Prompt optimization** | Concise system prompts, minimum token output limits, avoid unnecessary context. |
| **Response caching** | Cache AI-generated examples by card content hash. Same card → same example from cache (TTL: 24h). |
| **Batch requests** | Flashcard generation sends full text in one prompt → one API call generates multiple cards (not one call per card). |
| **Provider cost comparison** | IAiProvider abstraction allows switching to cheapest adequate provider at any time. |
| **Usage dashboard** | Admin dashboard tracks total AI cost per day. Alerts if cost exceeds thresholds. |

### 7.4 Scaling Strategy

| Component | Scale Strategy |
|---|---|
| **API Servers** | Horizontal scaling — stateless servers behind load balancer. Add instances during peak hours (evenings, exam periods). |
| **SQL Server** | Vertical scaling first (better hardware). Read replicas for analytics queries. Partitioning by user ID if needed. |
| **AI Requests** | Queue-based — non-urgent AI requests (batch generation, session analysis) go through a message queue to smooth spikes. Only tutor chat is real-time. |
| **File Storage** | Blob storage (Azure Blob / S3) for images and audio. CDN for fast delivery. |
| **Rate Limiting** | Redis-based sliding window. Separate limits per AI operation type. |

### 7.5 Future Premium Scale Points

| Feature | Scale Consideration |
|---|---|
| Multi-device sync | Same sync architecture, just remove the 1-device limit for premium users. |
| Collaborative decks | New API endpoints + shared deck permissions table. Architecture already supports via userId scoping. |
| Advanced analytics | Read replica dedicated to analytics queries. Pre-aggregated stats tables. |
| Higher AI limits | Configuration change per subscription tier — no code changes needed. |

---

## 8. Architecture Decision Summary

| # | Decision | Alternatives Considered | Rationale |
|---|---|---|---|
| AD-1 | SM-2 runs on client only | SM-2 on server, SM-2 on both | Latency: user expects instant feedback. Offline: must work without network. Server stores results, not logic. |
| AD-2 | AI proxied through backend | Direct AI API calls from client | Security: API keys never in APK. Rate limiting: enforced server-side. Caching: shared cache across users. |
| AD-3 | Room for local DB | SQLite directly, Realm | Room is Google's official Android ORM. Compile-time SQL validation. LiveData/Flow integration. First-class Compose support. |
| AD-4 | SQL Server for remote DB | PostgreSQL, MySQL, MongoDB | Project requirement. Strong SSMS tooling. Enterprise-grade reliability. |
| AD-5 | REST API (not GraphQL) | GraphQL, gRPC | Simpler for CRUD operations. Well-known by team. SSE for AI streaming is sufficient. |
| AD-6 | Hilt for DI | Koin, Manual DI | Compile-time safety. Official Google recommendation. Lifecycle-aware. |
| AD-7 | Offline-first sync | Online-first, Real-time sync | Students study offline. Sync complexity is manageable with last-write-wins. |
| AD-8 | Last-write-wins conflict resolution | Merge conflicts, CRDT | Simple to implement. SM-2 data is single-user, so conflicts are rare (multi-device edge case only). |

---

## Appendix: Requirement Traceability

| Requirement | Addressed By |
|---|---|
| AUTH-01 to AUTH-08 | AUTH Module (§2.3), JWT Flow (§3.3) |
| CARD-01 to CARD-12 | CARD + DECK Modules (§2.3), Backend CRUD (§3.2) |
| STUDY-01 to STUDY-11 | STUDY Module (§2.3), SM-2 Placement (§2.4), Review Flow (§5.1) |
| AI-01 to AI-16 | AI Module (§2.3), AI Architecture (§4), AI Activation Rules (§5.2) |
| SYNC-01 to SYNC-08 | SYNC Module (§2.3), Sync Architecture (§6) |
| NFR-P01 to NFR-P06 | Offline-first (§1.3), SM-2 on client (§2.4) |
| NFR-S01 to NFR-S06 | JWT Auth (§3.3), Backend Rules (§3.4) |
| NFR-SC01 to NFR-SC04 | Provider Abstraction (§7.2), Scale Strategy (§7.4), Premium Points (§7.5) |
| NFR-R01 to NFR-R04 | Sync Recovery (§6.3), Offline Queue (§6.4) |
| NFR-U01 to NFR-U06 | SETTINGS Module (§2.3) — implementation deferred to Phase 8 |

# 📋 Smart Flashcard App — TODO

> Complete, ordered checklist of all tasks required to finish the application.
> Grouped by phase/module. Update status as work progresses.

---

## Phase 1: Requirement Analysis & Planning

- [x] Define target users (university students, high school students, language learners)
- [x] Define core feature list (flashcards, SM-2, AI, cloud sync)
- [x] Document functional requirements
- [x] Document non-functional requirements (performance, security, scalability)
- [x] Define free tier vs premium feature boundaries
- [x] Create wireframes / UI mockups for all screens
- [x] Review and approve requirements with stakeholders

---

## Phase 2: System Architecture Design

- [x] Define Clean Architecture layers (data → domain → presentation)
- [x] Define package structure and module boundaries
- [x] Choose tech stack (Kotlin, Compose, Hilt, Room, Retrofit, etc.)
- [x] Design REST API contract between Android app and backend
- [x] Design data flow diagrams (local ↔ remote sync)
- [x] Design authentication flow (JWT-based)
- [x] Plan AI integration architecture (backend proxy to OpenAI/Gemini)
- [x] Document architecture decisions (ADRs)

---

## Phase 3: Database Schema Design (SQL Server — SSMS)

- [x] Design `Users` table (id, email, passwordHash, displayName, avatarUrl, aiUsage)
- [x] Design `Decks` table (id, userId, name, description, coverImage, cardCount)
- [x] Design `Flashcards` table (id, deckId, front, back, example, image, audio, SM-2 fields)
- [x] Design `ReviewLogs` table (id, flashcardId, userId, quality, responseTime, reviewedAt)
- [x] Design `AiChatHistory` table (id, userId, role, content, createdAt)
- [x] Design `SyncMetadata` table (id, userId, entityType, entityId, action, timestamp, synced)
- [x] Create indexes for performance (NextReviewDate, DeckId, UserId)
- [x] Write SQL migration scripts
- [x] Test schema on SQL Server (SSMS)
- [x] Design Room (SQLite) entities mirroring the SQL Server schema for offline cache

---

## Phase 4: Backend API Development

- [x] Set up backend project (ASP.NET Core Web API + .NET 9)
- [x] Implement user authentication endpoints (register, login, refresh token)
- [x] Implement deck CRUD API endpoints
- [x] Implement flashcard CRUD API endpoints
- [x] Implement review log API endpoints
- [x] Implement sync API endpoints (pull/push changes)
- [x] Implement AI proxy endpoints (placeholder — actual AI integration in later phase):
  - [x] Generate flashcards from text
  - [x] Generate flashcards from PDF file
  - [x] Generate flashcards from DOCX file
  - [x] Generate examples for a card
  - [x] Generate images for a card
  - [ ] Generate TTS audio
  - [x] Generate multiple-choice questions
  - [x] AI Tutor chat endpoint
- [ ] Implement AI usage tracking and rate limiting (free tier)
- [x] Add input validation and error handling
- [ ] Add API documentation (Swagger / OpenAPI)
- [ ] Write backend unit tests
- [ ] Deploy backend to staging environment

---

## Phase 5: Android Project Setup & Configuration

- [x] Update `libs.versions.toml` with all required dependencies
- [x] Update root `build.gradle.kts` with KSP, Hilt, Serialization plugins
- [x] Update app `build.gradle.kts` with all plugins and dependencies
- [x] Create `@HiltAndroidApp` Application class
- [x] Set up package structure (`data/`, `domain/`, `presentation/`, `di/`, `util/`)
- [x] Configure ProGuard rules for production builds
- [x] Set up `AndroidManifest.xml` (permissions: Internet, TTS, file access)
- [ ] Verify project builds successfully

---

## Phase 6: Android Data Layer Implementation

### 6.1 Local Database (Room)
- [x] Create `DeckEntity` with Room annotations
- [x] Create `FlashcardEntity` with SM-2 fields
- [x] Create `ReviewLogEntity`
- [x] Create `UserEntity`
- [x] Create `AiChatEntity`
- [x] Create `SyncMetadataEntity`
- [x] Create `DeckDao` (CRUD + queries)
- [x] Create `FlashcardDao` (CRUD + due cards query + new cards query)
- [x] Create `ReviewLogDao`
- [x] Create `UserDao`
- [x] Create `AiChatDao`
- [x] Create `AppDatabase` (Room database class with all DAOs)
- [ ] Write Room DAO unit tests

### 6.2 Remote API (Retrofit)
- [x] Create `AuthApi` interface (login, register, refresh)
- [x] Create `FlashcardApi` interface (deck/card CRUD, sync)
- [x] Create `AiApi` interface (generate, tutor, quiz)
- [x] Create `SyncApi` interface (push/pull)
- [x] Create DTO classes for requests and responses
- [x] Create `AuthInterceptor` for JWT token injection
- [x] Create `TokenManager` (DataStore-based)

### 6.3 Repository Implementations
- [x] Implement `DeckRepositoryImpl` (local + remote)
- [x] Implement `FlashcardRepositoryImpl` (local + remote)
- [x] Implement `AiRepositoryImpl` (remote only)
- [x] Implement `UserRepositoryImpl` (auth + profile)
- [x] Implement `SyncManager` (conflict resolution, delta sync)

---

## Phase 7: Android Domain Layer Implementation

### 7.1 Domain Models
- [x] Create `Deck` domain model
- [x] Create `Flashcard` domain model
- [x] Create `SM2Data` value object
- [x] Create `ReviewQuality` enum ("Học lại", "Khó", "Tốt", "Dễ")
- [x] Create `StudyQueue` model
- [x] Create `User` domain model
- [x] Create mapper functions (Entity ↔ Domain ↔ DTO)
- [x] Create `StudyModels` (ReviewLog, SM2Result, StudySessionSummary, DailyStats, LearningStats)

### 7.2 SM-2 Algorithm
- [x] Implement `SM2Engine.calculate()` — pure SM-2 formula
- [x] Handle quality < 3 reset path
- [x] Handle quality >= 3 progression path
- [x] Implement ease factor bounds (min 1.3)
- [x] Write comprehensive unit tests for SM2Engine
  - [x] Test q=0 (Học lại) — resets to interval=1
  - [x] Test q=2 (Khó) — short interval, lower EF
  - [x] Test q=3 (Tốt) — normal progression
  - [x] Test q=5 (Dễ) — longer interval, higher EF
  - [x] Test EF never drops below 1.3
  - [x] Test interval progression: 1 → 6 → calculated

### 7.3 Adaptive Scheduler
- [x] Implement `AdaptiveScheduler` wrapping SM-2
- [x] Detect repeated failures (failCount >= 3)
- [x] Trigger AI intervention for struggling cards
- [x] Analyze response time patterns
- [x] Adjust intervals based on AI recommendations
- [x] Write unit tests for AdaptiveScheduler

### 7.4 Use Cases
- [x] `GetStudyQueueUseCase` (daily queue with 40 new / 150 review limits)
- [x] `ReviewCardUseCase` (process user response, call SM-2, log review)
- [x] `CreateDeckUseCase`
- [x] `DeleteDeckUseCase`
- [x] `CreateFlashcardUseCase`
- [x] `UpdateFlashcardUseCase`
- [x] `DeleteFlashcardUseCase`
- [x] `GenerateFlashcardsUseCase` (AI: from text/PDF/DOCX)
- [x] `AiTutorUseCase` (send message, get response)
- [x] `GenerateQuizUseCase` (AI: MCQ from flashcard pool)
- [x] `GetLearningStatsUseCase`
- [x] `SyncDataUseCase`

---

## Phase 8: Android UI/UX Implementation

### 8.1 Design System & Theme
- [x] Define color palette (modern, dark mode support)
- [x] Define typography scale (Google Fonts: Inter)
- [x] Define shapes, elevation, spacing tokens
- [x] Implement `Color.kt`, `Theme.kt`, `Type.kt`
- [x] Support light/dark theme

### 8.2 Navigation
- [x] Define all screen routes as sealed class
- [x] Implement `NavGraph` with Compose Navigation
- [x] Implement bottom navigation bar (Home, Decks, AI Tutor, Stats, Settings)

### 8.3 Screens
- [x] **Home Screen** — Dashboard with today's study summary, quick start, streak
- [x] **Deck List Screen** — Grid/list of user's decks with card counts
- [x] **Deck Detail Screen** — Card list inside a deck, add/edit/delete
- [x] **Flashcard Editor Screen** — Create/edit card (front, back, example, image, audio)
  - [x] AI "Generate" buttons for examples, images, TTS
- [x] **Study Session Screen** — Flip-card animation, SM-2 response buttons
  - [x] "Học lại" / "Khó" / "Tốt" / "Dễ" buttons
  - [x] Progress indicator (cards remaining)
  - [x] TTS playback button
- [x] **AI Generate Screen** — Paste text → AI generates draft cards
  - [x] Review & edit generated cards before saving
- [x] **AI Tutor Screen** — Chat interface (Vietnamese & English)
- [x] **Quiz Screen** — Multiple-choice questions generated from cards
- [x] **Stats Screen** — Charts (cards reviewed, accuracy, streak, forecast)
- [x] **Settings Screen** — Daily limits, theme, language, account
- [x] **Login Screen** — Email + password authentication
- [x] **Register Screen** — New account creation

### 8.4 Reusable Components
- [x] `FlipCard` composable (3D flip animation)
- [x] `DeckCard` composable (deck thumbnail)
- [x] `SM2Buttons` composable (4 response buttons)
- [x] `ChatBubble` composable (AI tutor messages)
- [x] `LoadingOverlay` composable
- [x] `EmptyState` composable
- [x] `SearchBar` composable

---

## Phase 9: AI Integration (Android Side)

- [x] Backend: Implement GeminiService (real Gemini API integration)
- [x] Backend: Update AiController to use GeminiService (replace mock responses)
- [x] Implement AI flashcard generation from text input (AiGenerateScreen + ViewModel)
- [x] Implement AI flashcard generation from PDF upload (PdfPig text extraction + Gemini)
- [x] Implement AI flashcard generation from DOCX upload (OpenXml text extraction + Gemini)
- [x] Implement AI example generation for individual cards (backend ready)
- [x] Implement AI image generation for cards (Gemini gemini-2.5-flash-image model, saved to wwwroot)
- [x] Implement TTS audio generation (Google Translate TTS URL in FlashcardEditor + StudySession)
- [x] Implement AI tutor chat (AiTutorScreen + AiTutorViewModel with real Gemini API)
- [x] Implement AI multiple-choice question generation (QuizScreen + QuizViewModel)
- [x] Implement adaptive learning analysis after study sessions (backend ready)
- [x] Track AI usage count and enforce free tier limits (AiController reads User.AiUsageToday, checks RateLimits config)
- [x] Handle AI errors gracefully (network, rate limit, quota)
- [x] Update NavGraph with AI Generate and Quiz routes
- [x] Add AI Generate + Quiz buttons to DeckDetailScreen
- [x] Implement vocabulary extraction from PDF/DOCX (ExtractVocabularyFromTextAsync + extract-vocab endpoint + Android UI with mode selection and target language picker)

---

## Phase 10: Cloud Sync Implementation

- [x] Implement user login/register flow (LoginScreen + LoginViewModel + AuthController)
- [x] Implement JWT token storage (TokenManager with DataStore preferences)
- [x] Implement automatic token refresh (TokenRefreshInterceptor with synchronized lock)
- [x] Implement delta sync (SyncManager: push local changes → pull remote changes)
- [x] Handle merge conflicts (server-wins / last-write-wins strategy)
- [x] Implement background sync (SyncWorker + SyncScheduler with WorkManager, periodic 30min)
- [x] Implement offline-first behavior (all changes saved to local Room DB first, queued for next sync)
- [x] Show sync status indicator in UI (SyncIndicator in HomeScreen: syncing/success/failed states)

---

## Phase 11: Testing

### 11.1 Unit Tests
- [x] SM-2 algorithm tests (all quality paths) — `SM2EngineTest.kt`
- [x] Daily queue generation tests (limits, ordering) — `GetStudyQueueUseCaseTest.kt`
- [x] Adaptive scheduler tests — `AdaptiveSchedulerTest.kt`
- [x] Use case tests (with mocked repositories) — `ReviewCardUseCaseTest.kt`
- [x] Domain model tests (SM2Data, ReviewQuality, StudyQueue, StudySessionSummary) — `ModelTests.kt`
- [ ] ViewModel tests (requires Turbine/coroutines-test — skipped for now)

### 11.2 Integration Tests
- [ ] Room DAO integration tests (in-memory DB)
- [ ] API integration tests (mock server)
- [ ] Sync manager integration tests

### 11.3 UI Tests
- [ ] Study session flow (flip card → tap response → next card)
- [ ] Deck creation flow
- [ ] Flashcard editor flow
- [ ] Navigation between all screens
- [ ] AI tutor chat flow

---

## Phase 12: Optimization & Refactoring

- [x] Profile and optimize Room queries (indexes verified: composite on userId+deckId, userId+nextReviewDate, userId+reviewedAt)
- [x] Optimize Compose recomposition (stable keys, remember, animateFloatAsState already used in critical screens)
- [ ] Implement pagination for large card lists (Paging 3)
- [x] Optimize image loading and caching (Coil with crossfade enabled)
- [x] Reduce APK size (R8 minification + resource shrinking enabled for release builds)
- [x] Code review and cleanup (removed unused AI image generation button from FlashcardEditor)
- [x] ProGuard rules updated (added Google Credential Manager / Identity rules)
- [ ] Add KDoc documentation to public APIs

---

## Phase 13: Deployment Preparation

- [x] Configure signing keys (release keystore) — `app/release.keystore`, alias=memohop
- [x] Enable ProGuard/R8 minification for release builds — `isMinifyEnabled=true`, `isShrinkResources=true`, signingConfig linked
- [x] Create app icon and splash screen — `flashcard_launcher` icon exists, splash theme added (`Theme.MyApplication.Splash`)
- [x] Write Play Store listing (title, description, screenshots) — `STORE_LISTING.md` created
- [ ] Perform final QA testing on multiple devices *(THỦ CÔNG — cần test trên nhiều thiết bị thực)*
- [x] Create release build (`./gradlew assembleRelease`) — `app-release.apk` (4.34 MB) + `app-release.aab` (7.55 MB)
- [ ] Upload to Google Play Console (internal testing track) *(THỦ CÔNG — xem hướng dẫn bên dưới)*
- [x] Prepare backend for production deployment — `appsettings.Production.json` created
- [ ] Monitor crash reports (Firebase Crashlytics) *(THỦ CÔNG — cần tạo Firebase project)*
- [x] Plan post-launch updates and premium features — Roadmap v1.1 → v2.0 trong `STORE_LISTING.md`

### 📋 Hướng dẫn các bước THỦ CÔNG:

#### 1. QA Testing trên nhiều thiết bị
- Cài `app-release.apk` lên ít nhất 2-3 thiết bị (hoặc emulator) khác nhau
- Test: đăng ký/đăng nhập, tạo deck, tạo flashcard, học thẻ, quiz, AI tutor, sync
- Kiểm tra landscape/portrait, dark mode

#### 2. Upload lên Google Play Console
1. Vào https://play.google.com/console → Tạo ứng dụng mới
2. Upload `app/build/outputs/bundle/release/app-release.aab`
3. Điền thông tin từ `STORE_LISTING.md`
4. Chụp ít nhất 2 screenshot từ app (1080x1920) upload lên
5. Chọn Internal testing → Review → Publish

#### 3. Firebase Crashlytics
1. Vào https://console.firebase.google.com → Add project
2. Add Android app với package `com.example.myapplication`
3. Download `google-services.json` → đặt vào `app/`
4. Thêm Firebase dependencies vào `build.gradle.kts`
5. Build lại release

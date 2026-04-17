# Phase 1: Requirement Analysis & Planning
## Smart Flashcard Mobile Application

> **Document Version**: 1.0
> **Date**: 2026-03-12
> **Status**: Ready for Review
> **Next Phase**: Phase 2 — System Architecture Design

---

## 1. Target Users

### 1.1 University Students

| Aspect | Detail |
|---|---|
| **Profile** | Ages 18–25, studying across all disciplines (medicine, law, engineering, languages, economics). Heavy exam schedules, large volumes of theoretical knowledge. |
| **Learning Goals** | Memorize lecture content, definitions, formulas, medical terms, legal articles. Prepare for midterms and finals efficiently. Retain knowledge long-term across semesters. |
| **Pain Points** | ① Anki is powerful but has a steep learning curve and intimidating UI. ② Manual flashcard creation is extremely time-consuming for 500+ card decks. ③ No AI assistance to auto-generate cards from lecture notes or textbooks. ④ Difficult to identify weak areas without analytics. ⑤ Existing apps lack Vietnamese language support in feedback/UI. |
| **Expected Benefits** | Fast AI-powered card generation from lecture PDFs/notes. Clean, modern UI that doesn't require a tutorial. SM-2 ensures exam-ready retention. Adaptive system detects weak topics and helps before exams. |

### 1.2 High School Students

| Aspect | Detail |
|---|---|
| **Profile** | Ages 15–18, preparing for national exams (thi THPT Quốc gia). Subjects: History, Geography, Biology, Chemistry, Literature, English. |
| **Learning Goals** | Memorize dates, events, formulas, vocabulary, literary quotes. Study consistently over months for high-stakes exams. |
| **Pain Points** | ① Limited attention span — apps must be engaging, not boring. ② Don't know *how* to study efficiently (no awareness of spaced repetition). ③ Need guidance — "what should I study today?" ④ Parents/teachers can't monitor progress. ⑤ Most study apps are in English. |
| **Expected Benefits** | Daily study queue removes decision fatigue ("just press Start"). Vietnamese UI and feedback buttons ("Học lại", "Khó", "Tốt", "Dễ"). AI Tutor explains difficult concepts in simple Vietnamese. Quiz mode makes review feel like a game. |

### 1.3 Foreign Language Learners

| Aspect | Detail |
|---|---|
| **Profile** | Ages 16–40, learning English, Japanese, Korean, Chinese, or other languages. Includes IELTS/TOEIC/JLPT exam candidates and casual learners. |
| **Learning Goals** | Build vocabulary (2,000–10,000 words). Learn pronunciation, example sentences, and usage context. Achieve target exam scores. |
| **Pain Points** | ① Vocabulary apps lack context — just word + translation, no example sentences. ② No audio/pronunciation support in many apps. ③ No way to import vocab lists from textbooks or PDFs. ④ Forgetting words after initial study — no spaced repetition. ⑤ Can't ask "how do I use this word in a sentence?" |
| **Expected Benefits** | Cards with front (word) + back (meaning) + example sentence + image + TTS audio. AI generates contextual example sentences on demand. AI Tutor answers grammar/usage questions in real-time. SM-2 ensures long-term vocabulary retention. |

---

## 2. Core Features

### 2.1 Flashcard System

**Purpose**: The fundamental learning unit. Each card represents one concept, word, or fact.

| Component | Description |
|---|---|
| **Front** | The question, term, or word to recall. Supports plain text and basic formatting. |
| **Back** | The answer, definition, or translation. Supports plain text and basic formatting. |
| **Example Text** | Optional contextual example (e.g., a sentence using the vocabulary word). |
| **Image** | Optional image to provide visual association (e.g., a picture of the word's meaning). |
| **Audio** | Optional TTS audio for pronunciation (especially for language learning). |
| **Deck** | Cards are organized into decks (folders). Each deck has a name, description, and cover image. |

**Value**: Supports both simple vocabulary cards and complex theoretical knowledge cards (definitions, formulas, concepts).

---

### 2.2 SM-2 Spaced Repetition Algorithm

**Purpose**: Scientifically optimizes review timing to maximize long-term retention while minimizing study time.

**How it works**:
- After viewing a card, the user rates their recall quality.
- The algorithm calculates when the card should be shown again.
- Well-known cards appear less frequently; difficult cards appear more often.

**Vietnamese Feedback Buttons**:

| Button | SM-2 Quality | Meaning |
|---|---|---|
| **Học lại** | q = 0 | Complete blackout. Card is reset and shown again soon. |
| **Khó** | q = 2 | Recalled with serious difficulty. Short interval. |
| **Tốt** | q = 3 | Recalled correctly with some hesitation. Normal progression. |
| **Dễ** | q = 5 | Instant, perfect recall. Longest interval. |

**SM-2 Formula**:
- `EF' = EF + (0.1 - (5 - q) × (0.08 + (5 - q) × 0.02))`
- `EF = max(1.3, EF')`
- If `q < 3`: reset `n = 0`, `I = 1 day`
- If `q ≥ 3`: `n++`, `I(1) = 1`, `I(2) = 6`, `I(n) = I(n-1) × EF`

**Value**: Proven algorithm used by millions of learners worldwide. Transforms passive review into active, efficient learning.

---

### 2.3 Daily Study Limits

**Purpose**: Prevent burnout and ensure sustainable learning habits.

| Limit | Value | Rationale |
|---|---|---|
| **New cards/day** | 40 | Prevents overwhelming users with too much new material. |
| **Review cards/day** | 150 | Caps review workload to keep sessions under 30–45 minutes. |

**Behavior**:
- The app generates a **daily study queue** combining new + due review cards.
- New cards are introduced first, followed by review cards.
- Users can adjust these limits in settings.
- Cards beyond the limit roll over to the next day.

---

### 2.4 AI Flashcard Generation

**Purpose**: Eliminate the #1 friction point — manual card creation.

| Input Source | Process | Output |
|---|---|---|
| **User-typed text** | User pastes notes/paragraphs → AI extracts key concepts | Draft flashcards (front/back pairs) |
| **PDF file** | User uploads PDF → backend extracts text → AI processes | Draft flashcards |
| **DOCX file** | User uploads DOCX → backend extracts text → AI processes | Draft flashcards |

**Critical UX rule**: AI generates **drafts only**. Users must review, edit, and explicitly save cards. This ensures quality and gives users ownership of their learning material.

**Additional AI generation**:
- Generate **example sentences** for individual cards.
- Generate **images** (if relevant to content).
- Generate **TTS audio** for pronunciation.

**Value**: Reduces deck creation time from hours to minutes. Removes the biggest barrier to using flashcard apps.

---

### 2.5 AI Tutor Mode

**Purpose**: An always-available study companion that explains concepts and answers questions.

| Feature | Description |
|---|---|
| **Chat interface** | Conversational AI, similar to ChatGPT, embedded in the app. |
| **Languages** | Supports Vietnamese and English. |
| **Teaching style** | Friendly, patient, uses simple explanations and analogies. |
| **Context-aware** | Can reference the user's flashcards and decks for personalized help. |

**Example interactions**:
- "Giải thích từ 'ambiguous' cho em" ("Explain the word 'ambiguous' for me")
- "Cho em ví dụ về thì hiện tại hoàn thành" ("Give me examples of present perfect tense")
- "So sánh mitosis và meiosis" ("Compare mitosis and meiosis")

**Value**: Fills the gap between flashcard review and deep understanding. Students can get explanations without leaving the app.

---

### 2.6 Adaptive Learning System

**Purpose**: AI goes beyond fixed SM-2 scheduling to personalize learning based on individual behavior.

| Trigger | AI Action |
|---|---|
| Card failed ≥ 3 times | Explain content in simpler terms |
| Card failed ≥ 3 times | Generate additional examples |
| Card failed ≥ 3 times | Suggest splitting card into smaller, simpler cards |
| Consistently slow response time | Flag card as needing extra attention |
| High accuracy over time | Suggest extending intervals (boost confidence) |
| Study session analysis | Weekly learning insights and recommendations |

**Value**: Makes the app feel intelligent and responsive to the user, not just a dumb timer.

---

### 2.7 Cloud Sync

**Purpose**: Users can access their data from any device and never lose progress.

| Feature | Description |
|---|---|
| **Sync direction** | Bidirectional — local ↔ cloud |
| **Offline support** | App works fully offline; changes sync when connectivity resumes. |
| **Conflict resolution** | Last-write-wins with timestamp comparison. |
| **Data scope** | Decks, flashcards, SM-2 progress, review history, AI chat history. |

**Value**: Peace of mind that learning progress is never lost. Future-proofs for multi-device usage.

---

## 3. Functional Requirements

### 3.1 Authentication (AUTH)

| ID | Requirement |
|---|---|
| AUTH-01 | The system shall allow users to register with email and password. |
| AUTH-02 | The system shall allow users to log in with email and password. |
| AUTH-03 | The system shall issue a JWT token upon successful login. |
| AUTH-04 | The system shall automatically refresh expired tokens without requiring re-login. |
| AUTH-05 | The system shall allow users to log out, clearing local token storage. |
| AUTH-06 | The system shall display appropriate error messages for invalid credentials. |
| AUTH-07 | The system shall allow users to use the app in offline mode without authentication for local-only data. |
| AUTH-08 | The system shall hash passwords on the server side before storage. |

### 3.2 Flashcards & Decks (CARD)

| ID | Requirement |
|---|---|
| CARD-01 | The user shall be able to create a new deck with a name, optional description, and optional cover image. |
| CARD-02 | The user shall be able to view a list of all their decks with card counts. |
| CARD-03 | The user shall be able to edit a deck's name, description, and cover image. |
| CARD-04 | The user shall be able to delete a deck. Deleting a deck shall delete all cards within it. |
| CARD-05 | The user shall be able to create a flashcard with: front (required), back (required), example text (optional), image (optional), audio (optional). |
| CARD-06 | The user shall be able to view all cards within a specific deck. |
| CARD-07 | The user shall be able to edit any field of an existing flashcard. |
| CARD-08 | The user shall be able to delete individual flashcards. |
| CARD-09 | The user shall be able to search cards within a deck by front or back text. |
| CARD-10 | The system shall assign default SM-2 values (repetition=0, interval=1, EF=2.5) to newly created cards. |
| CARD-11 | The user shall be able to play TTS audio for card front/back text via the device's TTS engine. |
| CARD-12 | The system shall display the card count and due card count for each deck. |

### 3.3 Study & Review (STUDY)

| ID | Requirement |
|---|---|
| STUDY-01 | The system shall generate a daily study queue containing: up to 40 new cards and up to 150 due review cards. |
| STUDY-02 | The system shall determine "due" cards by comparing `nextReviewDate` to today's date. |
| STUDY-03 | The study screen shall display the card front first; the user taps to reveal the back. |
| STUDY-04 | After revealing the back, the system shall display four response buttons: "Học lại" (q=0), "Khó" (q=2), "Tốt" (q=3), "Dễ" (q=5). |
| STUDY-05 | Upon user response, the system shall recalculate SM-2 values (repetition, interval, easeFactor, nextReviewDate) using the SM-2 algorithm. |
| STUDY-06 | The system shall log each review in a review history (card ID, quality, timestamp, response time). |
| STUDY-07 | The study screen shall show progress (e.g., "15/40 cards remaining"). |
| STUDY-08 | The user shall be able to end a study session early; remaining cards stay in the queue. |
| STUDY-09 | If a card is rated "Học lại", it shall reappear later in the same study session. |
| STUDY-10 | The system shall never show more than the configured daily limit of new or review cards. |
| STUDY-11 | The user shall be able to adjust daily limits (new cards/day, review cards/day) in settings. |

### 3.4 AI Features (AI)

| ID | Requirement |
|---|---|
| AI-01 | The user shall be able to paste text and request AI-generated flashcard drafts. |
| AI-02 | The user shall be able to upload a PDF file and request AI-generated flashcard drafts. |
| AI-03 | The user shall be able to upload a DOCX file and request AI-generated flashcard drafts. |
| AI-04 | AI-generated flashcards shall be presented as drafts; the user must review and explicitly save them. |
| AI-05 | The user shall be able to edit AI-generated drafts before saving (modify front, back, examples, or delete individual cards). |
| AI-06 | The user shall be able to request AI to generate an example sentence for a specific flashcard. |
| AI-07 | The user shall be able to request AI to generate an image for a specific flashcard. |
| AI-08 | The user shall be able to request AI-generated TTS audio for a card. |
| AI-09 | The user shall be able to open an AI Tutor chat and ask questions in Vietnamese or English. |
| AI-10 | The AI Tutor shall respond in a friendly, easy-to-understand teaching style. |
| AI-11 | The AI Tutor shall maintain conversation history within a session. |
| AI-12 | The system shall generate multiple-choice questions from the user's flashcard pool for quiz mode. |
| AI-13 | The system shall track AI usage count per user per day. |
| AI-14 | The system shall enforce daily AI usage limits for free-tier users. |
| AI-15 | The system shall display remaining AI credits to the user. |
| AI-16 | When a card's fail count reaches ≥ 3, the system shall offer AI-powered adaptive assistance (simpler explanation, more examples, card-splitting suggestions). |

### 3.5 Sync & Cloud (SYNC)

| ID | Requirement |
|---|---|
| SYNC-01 | The app shall work fully offline using local Room database. |
| SYNC-02 | When online, the system shall sync local changes to the cloud (SQL Server via API). |
| SYNC-03 | When online, the system shall pull remote changes to the local database. |
| SYNC-04 | The system shall resolve sync conflicts using last-write-wins based on timestamps. |
| SYNC-05 | The system shall queue local changes made offline and sync them when connectivity resumes. |
| SYNC-06 | The system shall display a sync status indicator (synced / syncing / offline). |
| SYNC-07 | The user shall be able to manually trigger a sync. |
| SYNC-08 | The system shall sync: decks, flashcards (including SM-2 data), review logs, and AI chat history. |

---

## 4. Non-Functional Requirements

### 4.1 Performance

| ID | Requirement |
|---|---|
| NFR-P01 | Card flip and transition animations shall render at 60 fps with no visible jank. |
| NFR-P02 | The daily study queue shall be generated in < 500ms for up to 10,000 cards. |
| NFR-P03 | AI-generated flashcard drafts shall be returned within 15 seconds for text input ≤ 5,000 characters. |
| NFR-P04 | The app shall launch to the home screen in < 2 seconds on mid-range devices. |
| NFR-P05 | All local database operations (CRUD) shall complete in < 100ms. |
| NFR-P06 | The app shall function fully offline with zero dependency on network for core study features. |

### 4.2 Security

| ID | Requirement |
|---|---|
| NFR-S01 | All API communication shall use HTTPS (TLS 1.2+). |
| NFR-S02 | Passwords shall be hashed with bcrypt (cost factor ≥ 12) on the server. |
| NFR-S03 | JWT tokens shall expire after 1 hour; refresh tokens after 30 days. |
| NFR-S04 | Sensitive data (tokens, credentials) shall be stored in Android Keystore / EncryptedSharedPreferences. |
| NFR-S05 | API endpoints shall validate and sanitize all input to prevent SQL injection and XSS. |
| NFR-S06 | Users shall only be able to access their own data (server-side authorization). |

### 4.3 Scalability

| ID | Requirement |
|---|---|
| NFR-SC01 | The architecture shall support adding premium features without refactoring core modules. |
| NFR-SC02 | The backend shall support 10,000+ concurrent users. |
| NFR-SC03 | The database schema shall support future features (collaborative decks, shared libraries, leaderboards). |
| NFR-SC04 | AI provider shall be abstracted behind an interface to allow switching providers without app changes. |

### 4.4 Reliability

| ID | Requirement |
|---|---|
| NFR-R01 | The app shall not crash on network failures; all API errors shall be handled gracefully with user-friendly messages. |
| NFR-R02 | Local data shall persist across app updates. |
| NFR-R03 | Room database shall use schema migrations (not destructive rebuild) for all schema changes. |
| NFR-R04 | The app shall auto-recover from interrupted sync operations. |

### 4.5 Usability & Accessibility

| ID | Requirement |
|---|---|
| NFR-U01 | The app shall support both light and dark themes. |
| NFR-U02 | All interactive elements shall have minimum touch target size of 48dp. |
| NFR-U03 | The app shall use Vietnamese as the default language. |
| NFR-U04 | All screens shall be usable with one hand on standard phone sizes (< 6.7"). |
| NFR-U05 | Typography shall use a minimum of 14sp for body text and 12sp for captions. |
| NFR-U06 | Color contrast shall meet WCAG 2.1 AA standards. |

---

## 5. Free vs Premium Features

### Feature Matrix

| Feature | Free Tier | Premium Tier (Future) |
|---|---|---|
| Create decks & flashcards | ✅ Unlimited | ✅ Unlimited |
| SM-2 spaced repetition | ✅ Full | ✅ Full |
| Daily study queue | ✅ Full (40 new / 150 review) | ✅ Customizable limits |
| Manual card creation & editing | ✅ Full | ✅ Full |
| TTS (device engine) | ✅ Full | ✅ Full |
| Cloud sync | ✅ 1 device | ✅ Multi-device |
| AI flashcard generation (text) | ⚠️ 20 requests/day | ✅ Unlimited |
| AI flashcard generation (PDF) | ⚠️ 5 files/day | ✅ Unlimited |
| AI flashcard generation (DOCX) | ⚠️ 5 files/day | ✅ Unlimited |
| AI example generation | ⚠️ 20 requests/day | ✅ Unlimited |
| AI image generation | ❌ Not available | ✅ Unlimited |
| AI Tutor chat | ⚠️ 10 messages/day | ✅ Unlimited |
| AI adaptive learning | ⚠️ Basic (fail detection only) | ✅ Full (scheduling adjustments) |
| Quiz mode (MCQ) | ✅ Full | ✅ Full |
| Learning statistics | ✅ Basic (today's stats) | ✅ Advanced (trends, forecasts, heatmaps) |
| Export/import decks | ✅ CSV only | ✅ CSV, Anki (.apkg), PDF |
| Multiple decks | ✅ Up to 20 decks | ✅ Unlimited |
| Priority support | ❌ | ✅ |

### AI Usage Limits (Free Tier)

| AI Feature | Daily Limit |
|---|---|
| Flashcard generation (text input) | 20 requests |
| Flashcard generation (PDF/DOCX) | 5 files |
| Example sentence generation | 20 requests |
| Image generation | Not available |
| AI Tutor messages | 10 messages |
| **Total AI operations** | **~55 per day** |

> [!NOTE]
> Limits reset daily at midnight (user's local time). Unused credits do not roll over.

---

## 6. Screen List & Wireframe Descriptions

### 6.1 Splash Screen

| Aspect | Detail |
|---|---|
| **Purpose** | App branding and initialization. |
| **Components** | App logo (centered), app name "SmartFlash", loading indicator. |
| **Actions** | Auto-navigates to Home (if logged in) or Login (if not) after 1.5s. |

### 6.2 Login Screen

| Aspect | Detail |
|---|---|
| **Purpose** | User authentication. |
| **Components** | App logo (top), email input field, password input field (with show/hide toggle), "Đăng nhập" (Login) button, "Quên mật khẩu?" link, "Đăng ký" (Register) link, "Dùng offline" (Use offline) button. |
| **Actions** | Tap Login → authenticate → navigate to Home. Tap Register → navigate to Register. Tap Use Offline → navigate to Home (local-only mode). |

### 6.3 Register Screen

| Aspect | Detail |
|---|---|
| **Purpose** | New account creation. |
| **Components** | Display name input, email input, password input (with strength indicator), confirm password input, "Đăng ký" (Register) button, "Đã có tài khoản? Đăng nhập" link. |
| **Actions** | Tap Register → create account → auto-login → navigate to Home. |

### 6.4 Home Screen (Dashboard)

| Aspect | Detail |
|---|---|
| **Purpose** | Central hub showing today's learning summary and quick actions. |
| **Components** | **Top bar**: avatar, greeting ("Chào [name]!"), settings icon. **Study summary card**: cards due today (new + review count), estimated study time, streak counter (days in a row). **Quick start button**: "Bắt đầu học" (Start studying) — large, prominent CTA. **Recent decks section**: horizontal scrollable row of recent decks. **Bottom navigation bar**: Home, Decks, AI Tutor, Stats, Settings (5 tabs). |
| **Actions** | Tap "Start studying" → navigate to Study Session (auto-selects a deck or combined queue). Tap a deck → Deck Detail. Tap bottom nav items to switch tabs. |

### 6.5 Deck List Screen

| Aspect | Detail |
|---|---|
| **Purpose** | Browse and manage all decks. |
| **Components** | **Search bar** at top. **Grid of deck cards**: each showing name, card count, due card count, cover image (or default color). **FAB** (Floating Action Button): "+" to create a new deck. **Sort/filter options**: by name, date created, cards due. |
| **Actions** | Tap a deck → Deck Detail. Long-press a deck → context menu (edit, delete, share). Tap FAB → Create Deck dialog/screen. |

### 6.6 Deck Detail Screen

| Aspect | Detail |
|---|---|
| **Purpose** | View and manage cards within a deck. |
| **Components** | **Header**: deck name, description, card count, due count, "Học ngay" (Study now) button. **Card list**: scrollable list of cards showing front text preview and SM-2 status (new / learning / review / mastered). **Search bar** for filtering cards. **FAB**: "+" to add a card manually. **Toolbar actions**: AI Generate (from text/file), Edit deck, Delete deck. |
| **Actions** | Tap "Study now" → Study Session for this deck. Tap a card → Flashcard Editor (edit mode). Tap FAB → Flashcard Editor (create mode). Tap AI Generate → AI Generate Screen. |

### 6.7 Flashcard Editor Screen

| Aspect | Detail |
|---|---|
| **Purpose** | Create or edit a single flashcard. |
| **Components** | **Front field**: large text input with placeholder "Mặt trước (câu hỏi / từ vựng)". **Back field**: large text input with placeholder "Mặt sau (câu trả lời)". **Example field**: text input with placeholder "Ví dụ (tùy chọn)". **Image section**: add image button (camera / gallery / AI generate). **Audio section**: record audio button, TTS generate button. **AI action buttons**: "Tạo ví dụ bằng AI" (Generate example with AI), "Tạo hình ảnh bằng AI" (Generate image with AI). **Save button** (top right). **Preview toggle**: see how the card will look during study. |
| **Actions** | Fill fields → Tap Save → card is created/updated. Tap AI buttons → AI generates content → auto-fills into field (user can edit). Tap image → gallery picker or AI image modal. |

### 6.8 Study Session Screen

| Aspect | Detail |
|---|---|
| **Purpose** | Core learning experience — reviewing cards with SM-2. |
| **Components** | **Progress bar** (top): "12/40 thẻ" (12/40 cards). **Flashcard area** (center): large card showing front text. Tap to flip (3D animation) revealing back + example + image. **TTS button**: speaker icon to hear pronunciation. **SM-2 response buttons** (bottom, 4 buttons in a row): "Học lại" (red), "Khó" (orange), "Tốt" (green), "Dễ" (blue). Each button shows the next review interval below it (e.g., "< 1 phút", "10 phút", "1 ngày", "4 ngày"). **Exit button** (top left): end session early. |
| **Actions** | Tap card → flip animation reveals back. Tap a response button → SM-2 calculates next review → next card appears. Tap TTS → audio plays. Tap Exit → confirm dialog → return to deck. Session ends when all cards are reviewed → summary screen. |

### 6.9 Study Session Summary Screen

| Aspect | Detail |
|---|---|
| **Purpose** | Post-session feedback and stats. |
| **Components** | Congratulations message. Cards studied count. Accuracy breakdown (% Dễ, % Tốt, % Khó, % Học lại). Time spent. Next review forecast (e.g., "15 thẻ cần ôn lại vào ngày mai"). "Tiếp tục học" (Continue studying) button. "Về trang chủ" (Go home) button. |
| **Actions** | Tap Continue → start another session. Tap Go Home → navigate to Home. |

### 6.10 AI Generate Screen

| Aspect | Detail |
|---|---|
| **Purpose** | Bulk generate flashcard drafts using AI. |
| **Components** | **Input section**: large text area for pasting content, OR file upload buttons (PDF, DOCX). **Target deck selector**: dropdown to choose which deck to add cards to. **Language selector**: Vietnamese / English. **"Tạo thẻ" (Generate cards)** button. **Results section** (after generation): list of draft cards, each showing front/back with edit and delete icons. **"Lưu tất cả" (Save all)** button. **"Lưu đã chọn" (Save selected)** button with checkboxes. |
| **Actions** | Paste text or upload file → Tap Generate → loading indicator → drafts appear. Edit individual drafts inline. Delete unwanted drafts. Select and save chosen drafts to the deck. |

### 6.11 AI Tutor Screen

| Aspect | Detail |
|---|---|
| **Purpose** | Interactive AI chat for learning assistance. |
| **Components** | **Chat area**: scrollable message list with user bubbles (right) and AI bubbles (left). **Input bar** (bottom): text input + send button. **Suggested prompts** (when chat is empty): "Giải thích từ này", "Cho ví dụ", "So sánh A và B". **AI credit counter**: "Còn 7/10 tin nhắn hôm nay" (7/10 messages remaining today). **Language indicator**: current language (VN/EN). |
| **Actions** | Type message → Tap send → AI responds (streaming text). Tap suggested prompt → auto-fills input. Chat scrolls to latest message. |

### 6.12 Quiz Mode Screen

| Aspect | Detail |
|---|---|
| **Purpose** | Multiple-choice quiz generated from flashcards. |
| **Components** | **Question card**: displays the front of a flashcard. **4 answer choices** (A, B, C, D): one correct answer + 3 distractors from the same deck. **Progress indicator**: question number / total. **Timer** (optional): countdown per question. **Feedback animation**: correct (green flash) or incorrect (red flash + correct answer shown). |
| **Actions** | Tap an answer → feedback animation → next question. After all questions → Quiz Summary (score, accuracy, time). |

### 6.13 Statistics Screen

| Aspect | Detail |
|---|---|
| **Purpose** | Learning analytics and progress tracking. |
| **Components** | **Today's summary**: cards studied, accuracy, time spent. **Streak counter**: consecutive days studied (with calendar heatmap). **Weekly/monthly chart**: cards reviewed per day (bar chart). **Card status distribution**: pie chart (new / learning / review / mastered). **Forecast**: "Ngày mai cần ôn X thẻ" (Tomorrow you need to review X cards). |
| **Actions** | Tap chart segments for details. Switch between daily/weekly/monthly views. |

### 6.14 Settings Screen

| Aspect | Detail |
|---|---|
| **Purpose** | App configuration and account management. |
| **Components** | **Account section**: display name, email, avatar, "Đăng xuất" (Logout). **Study settings**: daily new card limit (slider, default 40), daily review limit (slider, default 150). **Appearance**: theme toggle (light/dark/system). **Language**: app language (Vietnamese/English). **AI settings**: show usage stats, clear AI chat history. **Sync**: last sync time, manual sync button, sync status. **About**: version, privacy policy, terms of service. |
| **Actions** | Adjust sliders → values saved immediately. Toggle theme → instant preview. Tap Logout → confirm dialog → navigate to Login. Tap Sync → trigger manual sync. |

---

## 7. Risks & Assumptions

### Assumptions

| # | Assumption |
|---|---|
| A1 | Users have Android devices running API level 26+ (Android 8.0+), covering ~95% of active devices. |
| A2 | AI features require internet connectivity; core study features work offline. |
| A3 | The backend (ASP.NET Core or Node.js) with SQL Server will be developed separately or in parallel. |
| A4 | AI provider (OpenAI/Gemini) API is available and pricing is acceptable for free-tier limits. |
| A5 | Users are willing to create an account for cloud sync; offline-only mode needs no account. |
| A6 | SM-2 algorithm is sufficient for the initial release; more advanced algorithms (FSRS, etc.) may be added later. |
| A7 | Text extraction from PDF/DOCX is handled server-side (not on-device) to reduce APK size and processing load. |

### Risks

| # | Risk | Impact | Mitigation |
|---|---|---|---|
| R1 | **AI API costs** exceed budget for free-tier users. | High — could make free tier unsustainable. | Strict daily limits, caching common requests, consider on-device small models for basic generation. |
| R2 | **AI response quality** is inconsistent for non-English languages (Vietnamese). | Medium — poor flashcard drafts reduce trust and adoption. | Prompt engineering with Vietnamese examples, user review step before saving, feedback loop. |
| R3 | **SQL Server hosting cost** and management complexity. | Medium — SQL Server licensing is expensive. | Consider Azure SQL (managed service) or plan migration to PostgreSQL if cost is prohibitive. |
| R4 | **Sync conflicts** cause data loss. | High — users losing SM-2 progress is unacceptable. | Conservative last-write-wins policy, sync conflict logging, periodic full backups. |
| R5 | **Scope creep** — too many features for the initial release. | High — delayed launch, incomplete features. | Strict MVP scope: Flashcards + SM-2 + basic AI + local storage first. Cloud and advanced AI in v1.1. |
| R6 | **PDF/DOCX parsing** accuracy varies by document format. | Low-Medium — some documents may not parse correctly. | Clear user messaging ("Best results with text-heavy documents"), fallback to manual text input. |
| R7 | **Device TTS quality** varies across manufacturers and languages. | Low — some devices have poor Vietnamese TTS. | Use AI-generated TTS as premium alternative; fall back gracefully if TTS unavailable. |

### Validation Checklist

| Check | Status |
|---|---|
| All user groups have defined goals, pain points, and benefits | ✅ |
| All 7 core features are defined with clear purpose and value | ✅ |
| Functional requirements are testable and grouped by module | ✅ |
| Non-functional requirements cover performance, security, scalability, reliability, usability | ✅ |
| Free vs premium boundary is clearly defined | ✅ |
| All screens have described purpose, components, and actions | ✅ |
| Risks are identified with impact and mitigation strategies | ✅ |
| No database schema or code included (per scope instructions) | ✅ |
| Document is sufficient to proceed directly to Phase 2 (Architecture) | ✅ |

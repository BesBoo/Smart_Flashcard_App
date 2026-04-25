# 1. Survey: LIệt kê các ứng dụng hiện có trên thị trường, mô tả và ưu nhược điểm của các ứng dụng đó
## 1.1 Anki
### Mô tả
Ứng dụng học flashcard mã nguồn mở, sử dụng thuật toán SM-2 (Spaced Repetition).
Cho phép tạo bộ thẻ (deck), đồng bộ qua cloud (AnkiWeb).
Hỗ trợ plugin mở rộng mạnh mẽ.
### Ưu điểm
- Thuật toán học hiệu quả, đã được kiểm chứng.
- Tùy biến cao (plugin, add-ons).
- Miễn phí (trên PC, Android).
### Nhược điểm
- UI/UX lỗi thời, khó sử dụng với người mới.
- Không có AI hỗ trợ tạo nội dung.
- Không thân thiện với người dùng phổ thông.
## 1.2 Quizlet
### Mô tả
- Nền tảng học tập phổ biến với flashcard, quiz, game.
- Có thư viện deck lớn do cộng đồng chia sẻ.
- Tích hợp một số tính năng AI (gần đây).
### Ưu điểm
- Giao diện đẹp, dễ sử dụng.
- Nhiều chế độ học (flashcard, trắc nghiệm, game).
- Nội dung phong phú từ cộng đồng.
### Nhược điểm
- Spaced repetition không tối ưu như Anki.
- Nhiều tính năng bị giới hạn trong bản trả phí.
- AI còn ở mức cơ bản, chưa cá nhân hóa sâu.
## 1.3 Memrise
### Mô tả
Ứng dụng học ngôn ngữ, sử dụng flashcard kết hợp video và audio.
Tập trung vào từ vựng và giao tiếp.
### Ưu điểm
- Nội dung học sinh động (video người bản xứ).
- Trải nghiệm học thú vị, gamification tốt.
- Phù hợp cho người học ngoại ngữ.
### Nhược điểm
- Không linh hoạt cho các lĩnh vực khác ngoài ngôn ngữ.
- Không cho phép tùy biến sâu như Anki.
- Thuật toán học không minh bạch.
# 2. Đề xuất ứng dụng của nhóm (Smart Flashcard App)
## 2.1 Mô tả hệ thống
Ứng dụng đề xuất là một hệ thống học flashcard thông minh trên Android với các đặc điểm:
- Sử dụng thuật toán SM-2 + Adaptive Scheduler
- Tích hợp AI (LLM) để:
- Tạo flashcard từ tài liệu (text, PDF, DOCX)
- Sinh ví dụ, hình ảnh, câu hỏi trắc nghiệm
- AI Tutor hỗ trợ học tập
- Hỗ trợ offline-first + cloud sync
Cá nhân hóa quá trình học dựa trên hành vi người dùng
## 2.2 Điểm khác biệt (Innovation / Contribution)
### 1. AI-first Flashcard System
Khác với các ứng dụng truyền thống:
- Anki: Không có AI
- Quizlet:  AI cơ bản
- Ứng dụng đề xuất:
Người dùng có thể:
- Upload PDF → tự động tạo flashcard
- Học → AI phân tích điểm yếu → gợi ý cải thiện
### 2.Adaptive Learning
Ý tưởng chính của em là không chỉ dùng mỗi SM-2 truyền thống, mà em mở rộng nó thành một hệ thống học thích ứng.
Cụ thể, ngoài việc chấm điểm đúng sai như bình thường, hệ thống còn theo dõi thêm:
- Thời gian người dùng trả lời nhanh hay chậm
- Số lần trả lời sai của từng flashcard
Từ đó, hệ thống sẽ điều chỉnh lịch học linh hoạt hơn.
Ví dụ:
- Nếu trả lời đúng nhưng chậm → vẫn xem là chưa chắc → cho ôn lại sớm hơn
- Nếu trả lời nhanh và đúng → giãn khoảng cách học ra xa hơn
Nếu một flashcard bị sai nhiều lần (ví dụ ≥ 3 lần), thì hệ thống coi đó là “điểm yếu” và AI sẽ can thiệp:
- Giải thích lại nội dung dễ hiểu hơn
- Tạo ví dụ minh họa
- Hoặc tạo câu hỏi trắc nghiệm để luyện thêm

---

# 3. Tính năng nâng cao: Trích xuất từ vựng tự động từ PDF/DOCX

## 3.1 Mô tả tính năng
Khi người dùng upload một file PDF hoặc DOCX chứa một đoạn văn (bài báo, bài giảng, truyện ngắn...), AI sẽ:

1. **Đọc toàn bộ đoạn văn** từ file
2. **Duyệt và trích xuất các từ vựng quan trọng/khó** → `frontText`
3. **Tự động tạo `backText`** (nghĩa, phiên âm, loại từ) cho mỗi từ vựng
4. **Tự động tạo `exampleText`** — câu ví dụ lấy từ chính đoạn văn gốc hoặc tạo mới

## 3.2 Đánh giá tính khả thi

| Yếu tố | Đánh giá |
|---|---|
| **Trích xuất text từ file** | ✅ Đã có — PdfPig (PDF) + OpenXml (DOCX) |
| **AI phân tích đoạn văn** | ✅ Khả thi — Gemini có khả năng NLP mạnh |
| **Trích xuất từ vựng** | ✅ Khả thi — Gemini hiểu ngữ cảnh, phân biệt từ phổ thông vs từ vựng nâng cao |
| **Tạo nghĩa + ví dụ** | ✅ Khả thi — Gemini tạo ví dụ rất tốt |
| **Giới hạn** | ⚠️ Max ~8000 ký tự/lần (giới hạn token) |

**Kết luận: Hoàn toàn khả thi** — chỉ cần thiết kế prompt phù hợp cho Gemini.

## 3.3 Hướng thực hiện

### Bước 1: Thêm endpoint mới trên Backend

Tạo endpoint `POST /api/ai/flashcards/extract-vocab` với logic:

```
File upload → Trích xuất text → Gửi prompt đặc biệt cho Gemini → Parse JSON → Trả về drafts
```

### Bước 2: Thiết kế Gemini Prompt

Prompt cần hướng dẫn Gemini:
- Đọc đoạn văn và xác định ngôn ngữ
- Lọc ra các từ vựng quan trọng, từ khó, từ chuyên ngành
- Bỏ qua các từ quá phổ thông (a, the, is, và, của...)
- Với mỗi từ vựng:
  - `frontText`: từ vựng gốc
  - `backText`: nghĩa (dịch sang ngôn ngữ target, ví dụ tiếng Việt), kèm loại từ (n/v/adj...)
  - `exampleText`: ưu tiên lấy câu có chứa từ đó từ đoạn văn gốc, nếu không có thì tạo mới

**Ví dụ prompt:**
```
You are a vocabulary extraction expert. Read the following text passage and extract 
important/difficult vocabulary words.

For each word:
- frontText: the original word/phrase as it appears in the text
- backText: Vietnamese translation + part of speech (n/v/adj/adv)
- exampleText: the original sentence from the text that contains this word. 
  If the word appears in multiple sentences, pick the most illustrative one.

RULES:
1. Only extract meaningful vocabulary (nouns, verbs, adjectives, adverbs)
2. Skip very common words (the, a, is, are, and, or...)
3. Extract 10-20 words maximum
4. If the text is in English, translate backText to Vietnamese
5. exampleText MUST be in the same language as frontText

Text passage:
---
{extracted_text}
---

Return JSON array only...
```

### Bước 3: Cho phép user chọn ngôn ngữ đích (target language)

Trên Android, thêm dropdown để user chọn:
- "Dịch sang Tiếng Việt" (mặc định)
- "Dịch sang Tiếng Anh"
- "Giữ nguyên ngôn ngữ gốc"

### Bước 4: Cập nhật Android UI

Trong `AiGenerateScreen`, khi user chọn file PDF/DOCX:
- Hiển thị 2 chế độ:
  - **"Tạo flashcard từ nội dung"** (chế độ hiện tại)
  - **"Trích xuất từ vựng"** (chế độ mới) ← mặc định khi upload file
- Khi chọn "Trích xuất từ vựng" → gọi endpoint mới

### Bước 5: Cho phép user review và chỉnh sửa

Sau khi AI trích xuất:
- Hiển thị danh sách từ vựng dạng checkbox
- User có thể bỏ chọn từ không muốn
- User có thể sửa nghĩa/ví dụ
- Click "Lưu" → chỉ lưu các từ đã chọn

## 3.4 Ví dụ minh họa

**Input (PDF chứa đoạn văn tiếng Anh):**
```
Climate change is one of the most pressing environmental challenges facing our planet today. 
Rising temperatures have led to the melting of polar ice caps, causing sea levels to surge. 
Scientists warn that without significant intervention, the consequences could be catastrophic 
for coastal communities around the world.
```

**Output (AI tự động trích xuất):**

| # | frontText | backText | exampleText |
|---|---|---|---|
| 1 | pressing | (adj) cấp bách, cấp thiết | Climate change is one of the most pressing environmental challenges. |
| 2 | melting | (n) sự tan chảy | Rising temperatures have led to the melting of polar ice caps. |
| 3 | polar ice caps | (n) chỏm băng ở cực | Rising temperatures have led to the melting of polar ice caps. |
| 4 | surge | (v) tăng vọt | ...causing sea levels to surge. |
| 5 | intervention | (n) sự can thiệp | ...without significant intervention, the consequences could be catastrophic. |
| 6 | catastrophic | (adj) thảm khốc | ...the consequences could be catastrophic for coastal communities. |
| 7 | coastal | (adj) ven biển | ...catastrophic for coastal communities around the world. |

## 3.5 Các file cần thay đổi

### Backend (C#)
- `GeminiService.cs` — Thêm method `ExtractVocabularyFromTextAsync()` với prompt mới
- `AiController.cs` — Thêm endpoint `POST /api/ai/flashcards/extract-vocab`

### Android (Kotlin)
- `AiApi.kt` — Thêm API call mới
- `AiRepository.kt` / `AiRepositoryImpl.kt` — Thêm method `extractVocabFromPdf/Docx()`
- `AiGenerateViewModel.kt` — Thêm mode "Trích xuất từ vựng"
- `AiGenerateScreen.kt` — Thêm UI chọn chế độ + review từ vựng

---

# 4. Tính năng cộng tác: Share deck giữa các user

## 4.1 Mô tả tính năng
Cho phép người dùng chia sẻ một deck cho user khác thông qua email hoặc mã chia sẻ.
User nhận được có thể tham gia deck để học ngay mà không cần tự tạo lại nội dung.

## 4.2 Vai trò và quyền truy cập
- Chủ deck (Owner): toàn quyền chỉnh sửa deck, thêm/sửa/xóa flashcard, thu hồi quyền chia sẻ.
- Người được chia sẻ (Viewer): được học và xem nội dung deck.
- Tùy chọn nâng cao (Editor): được thêm/sửa flashcard nhưng không được xóa deck hoặc chuyển quyền sở hữu.

## 4.3 Luồng sử dụng đề xuất
1. Owner mở màn hình chi tiết deck và bấm "Share".
2. Owner nhập email người nhận hoặc tạo link/mã chia sẻ.
3. Hệ thống gửi lời mời tham gia deck.
4. User nhận lời mời, bấm "Join" để thêm deck vào thư viện cá nhân.
5. Mọi cập nhật nội dung deck từ Owner được đồng bộ cho các user đã tham gia.

## 4.4 Giá trị mang lại
- Giúp học nhóm nhanh hơn, đặc biệt cho lớp học hoặc team ôn thi.
- Tránh trùng lặp công sức tạo deck giống nhau.
- Tăng mức độ gắn kết cộng đồng học tập trong ứng dụng.



# 📱 MemoHop — Smart Flashcard App

> **MemoHop** là ứng dụng học từ vựng thông minh kết hợp giữa thuật toán Spaced Repetition (Lặp lại ngắt quãng - SM-2) và Trí tuệ nhân tạo (AI) giúp bạn tăng tốc độ ghi nhớ kiến thức lên gấp 3 lần.

---

## ✨ Tính Năng Nổi Bật

- **🧠 Thuật Toán SM-2 (Spaced Repetition):** Tối ưu hóa thời gian ôn tập tự động dựa trên mức độ ghi nhớ (4 mức: Học lại, Khó, Tốt, Dễ).
- **🤖 Tích hợp Trợ lý AI (Gemini):**
  - Tự động tạo Flashcard từ văn bản thô, file PDF, hoặc DOCX.
  - AI tự động trích xuất từ vựng quan trọng từ tài liệu.
  - Tự động sinh ví dụ và hình ảnh minh họa sinh động.
  - **AI Tutor:** Trò chuyện giải đáp thắc mắc về từ vựng trực tiếp.
- **📝 Bài Kiểm Tra Tương Tác (Quiz):** Sinh câu hỏi trắc nghiệm và tự luận (2 phần) để ôn tập sâu hơn.
- **☁️ Đồng Bộ Hóa Đám Mây (Cloud Sync):** Hoạt động hoàn hảo ở chế độ ngoại tuyến (Offline-first) với Room Database và tự động đồng bộ ngầm (Delta Sync) lên Server thông qua WorkManager.
- **📊 Thống Kê Học Tập:** Biểu đồ trực quan, theo dõi chuỗi ngày học (Streak), tỉ lệ chính xác và dự báo khối lượng thẻ cần ôn mỗi ngày.
- **🎨 Giao Diện Hiện Đại:** Sử dụng Jetpack Compose với hoạt ảnh lật thẻ 3D mượt mà, hỗ trợ giao diện Sáng/Tối (Dark Mode) và typography chuẩn.

---

## 📸 Giao Diện Ứng Dụng (Screenshots)
<div align="center">
  <img width="453" height="932" alt="image" src="https://github.com/user-attachments/assets/9c2026fe-853a-475f-ac59-f2306524e5ce" alt="Home Screen" />
    &nbsp;&nbsp;
  <img width="458" height="925" alt="image" src="https://github.com/user-attachments/assets/b2fcb03f-a084-470c-8458-a97890d178c1" alt="Study Session" />
    &nbsp;&nbsp;
  <img width="485" height="940" alt="image" src="https://github.com/user-attachments/assets/5043f6f3-b60a-4144-bc8f-5482b0c9a4a0" alt="Stats Screen" />
</div>
<br>

---

## 🛠️ Công Nghệ & Kiến Trúc Sử Dụng

Dự án được xây dựng tuân thủ mô hình **Clean Architecture** (Data → Domain → Presentation).

### 1. Android Client (Kotlin)
- **UI Framework:** Jetpack Compose (Compose Navigation, Custom Animations).
- **Dependency Injection:** Dagger Hilt.
- **Local Database:** Room Database (Hỗ trợ cấu trúc Offline-first).
- **Network & API:** Retrofit, OkHttp.
- **Asynchronous/Concurrency:** Kotlin Coroutines & Flow.
- **Background Processing:** WorkManager (Background Sync 30 phút/lần).
- **Local Preferences:** Jetpack DataStore (TokenManager).

### 2. Backend API (.NET)
- **Framework:** ASP.NET Core Web API (.NET 9).
- **Cơ sở dữ liệu:** Microsoft SQL Server (Entity Framework).
- **Xác thực:** JWT Authentication (Refresh Token, Login, Register).
- **AI Integration:** Kết nối Google Gemini API (Gemini-2.5-flash) cho cả xử lý Text và Image.

---

## 📁 Cấu Trúc Dự Án

```text
MemoHop/
├── app/                  # Mã nguồn Android (Client)
│   ├── src/main/java/... # Source code (Chia theo Clean Architecture)
│   │   ├── data/         # Repositories, Room DAOs, Retrofit APIs, SyncManager
│   │   ├── domain/       # UseCases, Models, SM2Engine, AdaptiveScheduler
│   │   ├── presentation/ # Jetpack Compose UI (Screens, Components, Theme)
│   │   └── di/           # Hilt Modules
│   └── build.gradle.kts
├── backend/              # Mã nguồn Backend API (ASP.NET Core .NET 9)
├── database/             # Các scripts Migration cho Database SQL Server
├── docs/                 # Tài liệu dự án
├── STORE_LISTING.md      # Nội dung mô tả chuẩn bị phát hành lên Google Play
└── TODO.md               # Roadmap & danh sách Checklist công việc
```

---

## 🚀 Hướng Dẫn Cài Đặt (Local Development)

### 1. Thiết lập Backend (.NET 9)
1. Mở thư mục `backend/` bằng Visual Studio hoặc JetBrains Rider.
2. Cấu hình chuỗi kết nối SQL Server trong `appsettings.json` (hoặc `appsettings.Development.json`).
3. Chạy script SQL Server để cấu trúc các bảng (`Users`, `Decks`, `Flashcards`, `ReviewLogs`, `SyncMetadata`...) hoặc sử dụng Update-Database.
4. Thêm API Key của Gemini AI vào cấu hình backend.
5. Khởi động API Server.

### 2. Thiết lập Android Client
1. Clone dự án và mở bằng **Android Studio**.
2. Chờ Android Studio đồng bộ Gradle (Sync Project with Gradle Files).
3. Đảm bảo Backend Server đang chạy. (Lưu ý: Nếu test trên máy ảo Android Emulator, `BASE_URL` của API cần trỏ về `http://10.0.2.2:<port>` thay vì localhost).
4. Nhấn **Run** để khởi chạy ứng dụng.

### 3. Build & Release
- Ứng dụng đã được cấu hình ProGuard/R8 minification và Resource Shrinking.
- Keystore Release đã được cấu hình tại `app/release.keystore` (alias: `memohop`).
- Build bản release APK hoặc AAB bằng lệnh:
  ```bash
  ./gradlew assembleRelease
  ./gradlew bundleRelease
  ```

---

## 🗺️ Roadmap Phát Triển (Post-Launch)

- **v1.1 (Tháng 1):** Hỗ trợ Premium subscription, thêm mô hình AI (Claude, GPT-4), Widget học trên home screen, Push Notification.
- **v1.2 (Tháng 2-3):** Tính năng Public Decks, Leaderboard, Gamification, và Import từ Anki/Quizlet.
- **v2.0 (Tháng 6):** Lớp học nhóm (Group Class), Giáo viên tạo đề kiểm tra, Phân tích điểm yếu người dùng qua AI.

---

**Tác giả:** Trần Duy Đức (tranduyduc9679@gmail.com)

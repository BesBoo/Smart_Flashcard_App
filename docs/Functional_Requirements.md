# FUNCTIONAL REQUIREMENTS SPECIFICATION
## Ứng dụng Smart Flashcard


### 1. Các module chính

| Module | Mô tả |
|---|---|
| **Authentication** | Đăng ký, đăng nhập, quản lý JWT token |
| **Flashcard & Deck** | Tạo, sửa, xóa bộ thẻ và thẻ ghi nhớ |
| **Study & Review** | Phiên học với thuật toán SM-2, hàng đợi học hàng ngày |
| **AI Features** | Tạo thẻ bằng AI, AI Tutor, Quiz, trích xuất từ vựng |
| **Polysemy Analysis** | Phân tích từ đa nghĩa, gợi ý nhiều nghĩa khi tạo thẻ |
| **Deck Sharing** | Chia sẻ bộ thẻ giữa các người dùng, thư viện ảnh cộng đồng |
| **Cloud Sync** | Đồng bộ dữ liệu offline-first giữa thiết bị và server |
| **Statistics** | Thống kê học tập, biểu đồ tiến độ |
| **Admin** | Quản trị hệ thống (dành cho quản trị viên) |
| **Utilities Hub** | Trung tâm tiện ích học tập: Smart Review, Flashcard Quiz, Memo Chat |
| **Pet Companion** | Thú cưng ảo đồng hành học tập, cơ chế đói/no và thưởng cá theo tiến độ |
| **Account Recovery & SSO** | Quên mật khẩu bằng OTP email, đăng nhập Google, cập nhật email |
| **Reminder & Study Settings** | Cài đặt giới hạn học/ngày, nhắc học hằng ngày, quyền thông báo |
| **External Import & Sheet Sync** | Nhập thẻ từ file Excel, liên kết và đồng bộ Google Sheet |
| **Content Moderation** | Báo cáo nội dung, xử lý vi phạm và thông báo vi phạm cho chủ bộ thẻ |


## 2. Yêu cầu chức năng


### 2.1 Xác thực người dùng (AUTH)

| Mã | Yêu cầu |
|---|---|
| AUTH-01 | Hệ thống cho phép người dùng đăng ký tài khoản bằng email và mật khẩu. |
| AUTH-02 | Hệ thống cho phép người dùng đăng nhập bằng email và mật khẩu. |
| AUTH-03 | Hệ thống cấp JWT token khi đăng nhập thành công. | 
| AUTH-04 | Hệ thống tự động refresh token hết hạn mà không yêu cầu đăng nhập lại. | 
| AUTH-05 | Người dùng có thể đăng xuất, xóa token lưu trữ cục bộ. | 
| AUTH-06 | Hệ thống hiển thị thông báo lỗi phù hợp khi thông tin đăng nhập không hợp lệ. | 
| AUTH-07 | Người dùng có thể sử dụng ứng dụng ở chế độ offline mà không cần xác thực. | 
| AUTH-08 | Mật khẩu được mã hóa (hash) phía server trước khi lưu trữ. | 

**Hướng dẫn test trên Android:**

| Test | Các bước thao tác |
|---|---|
| Đăng ký | Mở app → Màn hình đăng nhập → Nhấn **"Đăng ký"** → Nhập email, mật khẩu, xác nhận mật khẩu → Nhấn **"Đăng ký"** → Kiểm tra chuyển về màn hình đăng nhập |
| Đăng nhập | Màn hình đăng nhập → Nhập email, mật khẩu → Nhấn **"Đăng nhập"** → Kiểm tra chuyển đến màn hình Home |
| Đăng nhập sai | Nhập email/mật khẩu sai → Nhấn **"Đăng nhập"** → Kiểm tra thông báo lỗi hiển thị |
| Đăng xuất | Tab **Cài đặt** (icon ⚙️) → Nhấn **"Đăng xuất"** → Kiểm tra chuyển về màn hình đăng nhập |

### 2.2 Quản lý Bộ thẻ & Thẻ ghi nhớ (CARD)

| Mã | Yêu cầu |
|---|---|
| CARD-01 | Người dùng có thể tạo bộ thẻ (deck) với tên, mô tả (tùy chọn), và ảnh bìa (tùy chọn). | 
| CARD-02 | Người dùng có thể xem danh sách tất cả bộ thẻ kèm số lượng thẻ. | 
| CARD-03 | Người dùng có thể chỉnh sửa tên, mô tả, ảnh bìa của bộ thẻ. | 
| CARD-04 | Người dùng có thể xóa bộ thẻ. Xóa bộ thẻ sẽ xóa tất cả thẻ bên trong. |
| CARD-05 | Người dùng có thể tạo thẻ ghi nhớ với: mặt trước (bắt buộc), mặt sau (bắt buộc), ví dụ (tùy chọn), hình ảnh (tùy chọn), audio (tùy chọn). | 
| CARD-06 | Người dùng có thể xem tất cả thẻ trong một bộ thẻ cụ thể. | 
| CARD-07 | Người dùng có thể chỉnh sửa bất kỳ trường nào của thẻ đã tồn tại. |
| CARD-08 | Người dùng có thể xóa từng thẻ riêng lẻ. | 
| CARD-09 | Người dùng có thể tìm kiếm thẻ trong bộ thẻ theo nội dung mặt trước hoặc mặt sau. | 
| CARD-10 | Hệ thống tự động gán giá trị SM-2 mặc định (repetition=0, interval=1, EF=2.5) cho thẻ mới. | 
| CARD-11 | Người dùng có thể phát audio TTS cho nội dung mặt trước/mặt sau của thẻ. | 
| CARD-12 | Hệ thống hiển thị số lượng thẻ tổng và số thẻ đến hạn ôn tập cho mỗi bộ thẻ. | 

**Hướng dẫn test trên Android:**

| Test | Các bước thao tác |
|---|---|
| Tạo bộ thẻ | Tab **Bộ thẻ** (icon 📚) → Nhấn nút **"+"** (góc dưới phải) → Nhập tên deck, mô tả (tùy chọn) → Nhấn **"Tạo"** → Kiểm tra deck mới xuất hiện trong danh sách |
| Xem danh sách deck | Tab **Bộ thẻ** → Kiểm tra danh sách hiển thị tên, số thẻ, số thẻ đến hạn |
| Sửa bộ thẻ | Tab **Bộ thẻ** → Nhấn vào deck → Nhấn icon **✏️** (sửa) → Thay đổi tên/mô tả → Nhấn **"Lưu"** |
| Xóa bộ thẻ | Tab **Bộ thẻ** → Nhấn vào deck → Nhấn icon **🗑️** (xóa) → Xác nhận xóa → Kiểm tra deck biến mất |
| Tạo thẻ ghi nhớ | Tab **Bộ thẻ** → Nhấn vào deck → Nhấn nút **"+"** (thêm thẻ) → Nhập mặt trước, mặt sau, ví dụ (tùy chọn) → Nhấn **"Tạo thẻ"** |
| Sửa thẻ | Trong deck → Nhấn vào thẻ cần sửa → Chỉnh sửa nội dung → Nhấn **✓** (lưu) |
| Xóa thẻ | Trong deck → Vuốt thẻ sang trái hoặc nhấn icon xóa → Xác nhận xóa |
| Phát TTS | Khi tạo/sửa thẻ → Nhấn icon **🔊** bên cạnh mặt trước hoặc mặt sau → Kiểm tra âm thanh phát |


### 2.3 Học tập & Ôn tập (STUDY)

| Mã | Yêu cầu |
|---|---|
| STUDY-01 | Hệ thống tạo hàng đợi học hàng ngày gồm: tối đa 40 thẻ mới và tối đa 150 thẻ ôn tập. |
| STUDY-02 | Hệ thống xác định thẻ "đến hạn" bằng cách so sánh `nextReviewDate` với ngày hiện tại. |
| STUDY-03 | Màn hình học hiển thị mặt trước trước; người dùng chạm để lật xem mặt sau (hiệu ứng lật 3D). |
| STUDY-04 | Sau khi lật thẻ, hệ thống hiển thị 4 nút phản hồi: "Học lại" (q=0), "Khó" (q=2), "Tốt" (q=3), "Dễ" (q=5). |
| STUDY-05 | Khi người dùng phản hồi, hệ thống tính toán lại các giá trị SM-2 (repetition, interval, easeFactor, nextReviewDate) bằng thuật toán SM-2. |
| STUDY-06 | Hệ thống ghi lại mỗi lần ôn tập vào lịch sử (card ID, quality, timestamp, response time). |
| STUDY-07 | Màn hình học hiển thị tiến độ (ví dụ: "15/40 thẻ còn lại"). |
| STUDY-08 | Người dùng có thể kết thúc phiên học sớm; các thẻ còn lại vẫn ở trong hàng đợi. |
| STUDY-09 | Nếu thẻ được đánh giá "Học lại", thẻ đó sẽ xuất hiện lại trong cùng phiên học. |
| STUDY-10 | Hệ thống không bao giờ hiển thị quá giới hạn hàng ngày đã cấu hình. |
| STUDY-11 | Người dùng có thể điều chỉnh giới hạn hàng ngày (thẻ mới/ngày, thẻ ôn tập/ngày) trong cài đặt. |

**Hướng dẫn test trên Android:**

| Test | Các bước thao tác |
|---|---|
| Bắt đầu học (toàn bộ) | Tab **Trang chủ** → Nhấn **"Bắt đầu học"** → Hệ thống tải hàng đợi thẻ đến hạn → Bắt đầu phiên học |
| Bắt đầu học (theo deck) | Tab **Bộ thẻ** → Nhấn vào deck → Nhấn **"Học ngay"** → Bắt đầu phiên học với thẻ trong deck đó |
| Lật thẻ | Trong phiên học → Đọc mặt trước → Nhấn vào thẻ để lật (hiệu ứng lật 3D) → Xem mặt sau |
| Phản hồi | Sau khi lật thẻ → Chọn 1 trong 4 nút: **"Học lại"** / **"Khó"** / **"Tốt"** / **"Dễ"** → Thẻ tiếp theo hiển thị |
| Kết thúc sớm | Trong phiên học → Nhấn nút **"←"** (quay lại) → Xác nhận kết thúc phiên |

### 2.4 Thuật toán SM-2 & Học thích ứng (SM2)

| Mã | Yêu cầu |
|---|---|
| SM2-01 | Hệ thống triển khai thuật toán SM-2 với công thức: `EF' = EF + (0.1 - (5 - q) × (0.08 + (5 - q) × 0.02))`, `EF = max(1.3, EF')`. |
| SM2-02 | Nếu `q < 3`: reset `n = 0`, `interval = 1 ngày`. |
| SM2-03 | Nếu `q ≥ 3`: `n++`, `I(1) = 1`, `I(2) = 6`, `I(n) = I(n-1) × EF`. |
| SM2-04 | Ease Factor không bao giờ giảm dưới 1.3. |
| SM2-05 | Hệ thống theo dõi thời gian phản hồi của người dùng cho mỗi thẻ. |
| SM2-06 | Nếu thẻ bị sai liên tiếp ≥ 3 lần, hệ thống đánh dấu là "thẻ khó" và kích hoạt AI can thiệp. |
| SM2-07 | Nếu trả lời đúng nhưng thời gian phản hồi chậm, hệ thống rút ngắn khoảng cách ôn tập. |
| SM2-08 | Nếu trả lời nhanh và đúng liên tục, hệ thống giãn khoảng cách ôn tập. |

**Hướng dẫn test trên Android:**

| Test | Các bước thao tác |
|---|---|
| SM-2 cơ bản | Tạo thẻ mới → Học thẻ → Nhấn **"Dễ"** → Kiểm tra thẻ không xuất hiện lại hôm nay (interval tăng) |
| Thẻ khó (fail 3 lần) | Học 1 thẻ → Nhấn **"Học lại"** 3 lần liên tiếp → Kiểm tra hệ thống đề xuất AI hỗ trợ |

### 2.5 Tính năng AI (AI)

| Mã | Yêu cầu |
|---|---|
| AI-01 | Người dùng có thể dán văn bản và yêu cầu AI tạo bản nháp thẻ ghi nhớ. |
| AI-02 | Người dùng có thể tải lên file PDF và yêu cầu AI tạo bản nháp thẻ ghi nhớ. |
| AI-03 | Người dùng có thể tải lên file DOCX và yêu cầu AI tạo bản nháp thẻ ghi nhớ. |
| AI-04 | Thẻ do AI tạo được hiển thị dưới dạng bản nháp; người dùng phải xem xét và lưu thủ công. |
| AI-05 | Người dùng có thể chỉnh sửa bản nháp AI trước khi lưu (sửa mặt trước, mặt sau, ví dụ, hoặc xóa thẻ). |
| AI-06 | Người dùng có thể yêu cầu AI tạo câu ví dụ cho một thẻ cụ thể. |
| AI-07 | Người dùng có thể yêu cầu AI tạo hình ảnh minh họa cho một thẻ cụ thể (sử dụng Gemini image model). |
| AI-08 | Người dùng có thể phát audio TTS cho thẻ (sử dụng Google Translate TTS). |
| AI-09 | Người dùng có thể mở Memo và đặt câu hỏi bằng tiếng Việt hoặc tiếng Anh. |
| AI-10 | Memo Chat phản hồi theo phong cách thân thiện, dễ hiểu, sử dụng ví dụ và phép so sánh. |
| AI-11 | Memo Chat duy trì lịch sử hội thoại trong phiên chat. |
| AI-12 | Trong quá trình trò chuyện với Memo Chat, nếu phát hiện từ vựng mới thì hệ thống sẽ đề xuất và người dùng có thể lưu về trong quá trình chat |
| AI-13 | Hệ thống theo dõi số lần sử dụng AI mỗi ngày cho từng người dùng. |
| AI-14 | Hệ thống tạo câu hỏi trắc nghiệm từ thẻ ghi nhớ cho chế độ Quiz (1 đáp án đúng + 3 đáp án nhiễu). |
| AI-15 | Khi thẻ bị sai ≥ 3 lần, hệ thống đề xuất AI hỗ trợ (giải thích đơn giản hơn, thêm ví dụ). |

**Hướng dẫn test trên Android:**

| Test | Các bước thao tác |
|---|---|
| AI tạo thẻ từ text | Tab **Bộ thẻ** → Nhấn vào deck → Nhấn **"AI tạo thẻ"** (icon ✨) → Chọn tab **"Văn bản"** → Dán đoạn văn bản → Nhấn **"Tạo thẻ"** → Xem bản nháp → Chỉnh sửa nếu cần → Nhấn **"Lưu tất cả"** |
| AI tạo thẻ từ PDF | Tab **Bộ thẻ** → Nhấn vào deck → Nhấn **"AI tạo thẻ"** → Chọn tab **"PDF"** → Chọn file PDF từ thiết bị → Nhấn **"Tạo thẻ"** → Xem và lưu bản nháp |
| AI tạo thẻ từ DOCX | Tương tự PDF, chọn tab **"DOCX"** → Chọn file DOCX |
| AI tạo ví dụ | Tạo/sửa thẻ → Nhấn icon **✨** bên cạnh ô "Ví dụ" → Chờ AI tạo câu ví dụ → Câu ví dụ tự động điền vào |
| AI tạo hình ảnh | Tạo/sửa thẻ → Phần "Hình ảnh minh họa" → Nhấn **"Chọn ảnh"** → Chọn **"AI tạo ảnh"** → Chờ ảnh được tạo |
| AI Tutor chat | Tab **AI Tutor** (icon 💬) → Nhập câu hỏi vào ô chat → Nhấn **"Gửi"** → Xem phản hồi AI → Tiếp tục hỏi để kiểm tra lịch sử hội thoại |
| Quiz | Tab **Bộ thẻ** → Nhấn vào deck (cần ≥ 4 thẻ) → Nhấn **"Quiz"** → Trả lời câu hỏi trắc nghiệm → Kiểm tra điểm |
| Trích xuất từ vựng | Tab **Bộ thẻ** → Nhấn vào deck → Nhấn **"AI tạo thẻ"** → Chọn tab **"Trích xuất từ vựng"** → Upload file PDF/DOCX → Nhấn **"Trích xuất"** → Xem danh sách từ vựng → Chọn/bỏ chọn → Nhấn **"Lưu"** |

### 2.6 Trích xuất từ vựng tự động (VOCAB)

| Mã | Yêu cầu |
|---|---|
| VOCAB-01 | Người dùng có thể tải lên file PDF/DOCX và chọn chế độ "Trích xuất từ vựng". |
| VOCAB-02 | AI đọc đoạn văn, trích xuất các từ vựng quan trọng/khó/chuyên ngành. |
| VOCAB-03 | Với mỗi từ vựng, AI tạo: `frontText` (từ gốc), `backText` (nghĩa + loại từ), `exampleText` (câu ví dụ). |
| VOCAB-04 | AI bỏ qua các từ quá phổ thông (a, the, is, và, của...). |
| VOCAB-05 | Người dùng có thể chọn ngôn ngữ đích cho bản dịch (Tiếng Việt, Tiếng Anh, hoặc giữ nguyên). |
| VOCAB-06 | Người dùng có thể xem lại, chỉnh sửa, chọn/bỏ chọn từ vựng trước khi lưu. |
| VOCAB-07 | Hệ thống trích xuất tối đa 10–20 từ vựng mỗi lần. |

**Hướng dẫn test trên Android:**

| Test | Các bước thao tác |
|---|---|
| Trích xuất từ vựng | Tab **Bộ thẻ** → Nhấn vào deck → Nhấn **"AI tạo thẻ"** (icon ✨) → Chọn chế độ **"Trích xuất từ vựng"** → Tải lên file PDF hoặc DOCX → Nhấn **"Trích xuất"** → Xem danh sách từ vựng → Chọn/bỏ chọn từ → Chỉnh sửa nếu cần → Nhấn **"Lưu"** |

### 2.7 Phân tích từ đa nghĩa (POLY)

| Mã | Yêu cầu |
|---|---|
| POLY-01 | Khi người dùng nhập từ vào mặt trước (frontText) của thẻ, hệ thống cung cấp nút "Phân tích từ" để phân tích từ đa nghĩa. |
| POLY-02 | Hệ thống sử dụng AI (Gemini) để phân tích từ và trả về danh sách các nghĩa khác nhau (senses) của từ đó. |
| POLY-03 | Mỗi nghĩa (sense) bao gồm: loại từ (part of speech), định nghĩa ngắn gọn, bản dịch tiếng Việt, và câu ví dụ minh họa. |
| POLY-04 | Hệ thống hiển thị kết quả phân tích dưới dạng danh sách các nghĩa có thể chọn được. |
| POLY-05 | Người dùng có thể chọn một nghĩa cụ thể để tự động điền vào mặt sau (backText) và ví dụ (exampleText) của thẻ. |
| POLY-06 | Người dùng có thể lưu nhiều nghĩa cùng lúc — mỗi nghĩa được lưu thành một thẻ riêng biệt trong cùng bộ thẻ. |
| POLY-07 | Hệ thống hiển thị trạng thái đã lưu/chưa lưu cho mỗi nghĩa để tránh tạo thẻ trùng lặp. |
| POLY-08 | Tính năng phân tích từ đa nghĩa được tính vào giới hạn sử dụng AI hàng ngày của người dùng. |

**Hướng dẫn test trên Android:**

| Test | Các bước thao tác |
|---|---|
| Phân tích từ đa nghĩa | Tab **Bộ thẻ** → Nhấn vào deck → Nhấn **"+"** (thêm thẻ) → Nhập từ vào ô **"Mặt trước"** (ví dụ: "light") → Nhấn icon **📖 Phân tích từ** → Chờ AI phân tích → Xem danh sách các nghĩa |
| Chọn 1 nghĩa | Sau khi phân tích → Nhấn vào 1 nghĩa trong danh sách → Mặt sau và ví dụ tự động điền → Nhấn **"Tạo thẻ"** |
| Lưu nhiều nghĩa | Sau khi phân tích → Nhấn nút **"Lưu"** trên từng nghĩa muốn lưu → Mỗi nghĩa tạo thành 1 thẻ riêng → Nghĩa đã lưu hiển thị trạng thái ✓ |

### 2.8 Chia sẻ bộ thẻ & Thư viện ảnh cộng đồng (SHARE)

| Mã | Yêu cầu |
|---|---|
| SHARE-01 | Người dùng có thể chia sẻ (share) một bộ thẻ của mình cho các người dùng khác bằng cách bật chế độ chia sẻ. |
| SHARE-02 | Bộ thẻ được chia sẻ sẽ hiển thị trong danh sách deck của người nhận với nhãn "Được chia sẻ". |
| SHARE-03 | Người nhận có thể xem và học các thẻ trong bộ thẻ được chia sẻ. |
| SHARE-04 | Chỉ chủ sở hữu (owner) bộ thẻ mới có quyền chỉnh sửa và xóa thẻ trong bộ thẻ được chia sẻ. |
| SHARE-05 | Người dùng có thể hủy chia sẻ bộ thẻ bất kỳ lúc nào; bộ thẻ sẽ bị xóa khỏi danh sách của người nhận. |
| SHARE-06 | Khi người dùng tạo thẻ có hình ảnh và lưu, hệ thống tự động upload ảnh lên server và đăng ký vào thư viện ảnh cộng đồng (shared images). |
| SHARE-07 | Khi người dùng nhập từ vào mặt trước (frontText) khi tạo thẻ mới, hệ thống tự động tìm kiếm và gợi ý các hình ảnh từ thư viện cộng đồng khớp với từ khóa đó. |
| SHARE-08 | Người dùng có thể chọn một hình ảnh gợi ý từ cộng đồng để chèn vào thẻ của mình mà không cần tải ảnh lên. |
| SHARE-09 | Hệ thống theo dõi số lần sử dụng (usage count) của mỗi ảnh cộng đồng để xếp hạng ảnh phổ biến. |
| SHARE-10 | Ảnh cộng đồng chỉ bao gồm ảnh đã được upload lên server (URL dạng http); ảnh lưu cục bộ trên thiết bị không được chia sẻ trực tiếp. |

**Hướng dẫn test trên Android:**

| Test | Các bước thao tác |
|---|---|
| Chia sẻ deck | Tab **Bộ thẻ** → Nhấn vào deck → Nhấn icon **"Chia sẻ"** (share) → Bật chế độ chia sẻ → Sao chép mã/link chia sẻ |
| Tham gia deck được chia sẻ | Tab **Bộ thẻ** → Nhấn **"Tham gia deck"** → Nhập mã chia sẻ → Nhấn **"Tham gia"** → Kiểm tra deck xuất hiện với nhãn *"Được chia sẻ"* |
| Hủy chia sẻ | Tab **Bộ thẻ** → Nhấn vào deck đã chia sẻ → Nhấn icon **"Chia sẻ"** → Tắt chế độ chia sẻ |
| Thư viện ảnh cộng đồng (đăng ảnh) | **User A**: Tạo thẻ mới → Nhập từ "light" vào mặt trước → Chọn ảnh từ thư viện → Nhấn **"Tạo thẻ"** → Ảnh tự động upload lên thư viện cộng đồng |
| Thư viện ảnh cộng đồng (nhận gợi ý) | **User B**: Đăng nhập tài khoản khác → Tạo thẻ mới → Nhập từ "light" vào mặt trước → Chờ ~1 giây → Phần **"Ảnh gợi ý từ cộng đồng"** xuất hiện bên dưới ô hình ảnh → Nhấn chọn ảnh gợi ý → Ảnh được chèn vào thẻ |

### 2.9 Đồng bộ dữ liệu đám mây (SYNC)

| Mã | Yêu cầu |
|---|---|
| SYNC-01 | Ứng dụng hoạt động đầy đủ ở chế độ offline sử dụng Room database. |
| SYNC-02 | Khi có mạng, hệ thống đồng bộ thay đổi cục bộ lên cloud (SQL Server qua API). |
| SYNC-03 | Khi có mạng, hệ thống kéo thay đổi từ server về thiết bị. |
| SYNC-04 | Hệ thống giải quyết xung đột đồng bộ theo nguyên tắc last-write-wins (dựa trên timestamp). |
| SYNC-05 | Hệ thống xếp hàng các thay đổi offline và đồng bộ khi có kết nối trở lại. |
| SYNC-06 | Hệ thống hiển thị chỉ báo trạng thái đồng bộ (đã đồng bộ / đang đồng bộ / offline). |
| SYNC-07 | Người dùng có thể kích hoạt đồng bộ thủ công. |
| SYNC-08 | Hệ thống đồng bộ: bộ thẻ, thẻ ghi nhớ (bao gồm dữ liệu SM-2), nhật ký ôn tập, lịch sử chat AI. |
| SYNC-09 | Đồng bộ nền tự động chạy định kỳ mỗi 30 phút (sử dụng WorkManager). |

**Hướng dẫn test trên Android:**

| Test | Các bước thao tác |
|---|---|
| Đồng bộ thủ công | Tab **Cài đặt** → Nhấn **"Đồng bộ ngay"** → Kiểm tra chỉ báo trạng thái (đang đồng bộ → đã đồng bộ) |
| Kiểm tra offline | Tắt Wi-Fi/dữ liệu → Tạo thẻ mới → Bật lại Wi-Fi → Đồng bộ thủ công → Kiểm tra thẻ đã được đồng bộ lên server |

### 2.10 Thống kê học tập (STATS)

| Mã | Yêu cầu |
|---|---|
| STATS-01 | Hiển thị tóm tắt hôm nay: số thẻ đã học, độ chính xác, thời gian học. |
| STATS-02 | Hiển thị chuỗi ngày học liên tiếp (streak) với calendar heatmap. |
| STATS-03 | Hiển thị biểu đồ thẻ đã ôn theo tuần/tháng (bar chart). |
| STATS-04 | Hiển thị phân bố trạng thái thẻ: mới / đang học / ôn tập / thuộc (pie chart). |
| STATS-05 | Hiển thị dự báo: "Ngày mai cần ôn X thẻ". |

**Hướng dẫn test trên Android:**

| Test | Các bước thao tác |
|---|---|
| Xem thống kê | Tab **Thống kê** (icon 📊) → Kiểm tra hiển thị: tóm tắt hôm nay, chuỗi streak, biểu đồ tuần/tháng, phân bố trạng thái thẻ, dự báo ngày mai |

### 2.11 Quản trị hệ thống (ADMIN)

#### 2.9.1 Xác thực & Phân quyền Admin

| Mã | Yêu cầu |
|---|---|
| ADMIN-01 | Chỉ người dùng có role = "admin" trong JWT claim mới có thể truy cập các chức năng quản trị. |
| ADMIN-02 | Hệ thống từ chối truy cập (HTTP 403 Forbidden) khi người dùng không có quyền admin cố truy cập API quản trị. |
| ADMIN-03 | Giao diện ứng dụng chỉ hiển thị mục "Admin Dashboard" trong menu điều hướng khi người dùng đăng nhập với role admin. |

#### 2.9.2 Dashboard — Thống kê tổng quan

| Mã | Yêu cầu |
|---|---|
| ADMIN-04 | Quản trị viên có thể xem tổng số người dùng đã đăng ký trong hệ thống. |
| ADMIN-05 | Quản trị viên có thể xem tổng số bộ thẻ (decks) đã được tạo trên toàn hệ thống. |
| ADMIN-06 | Quản trị viên có thể xem tổng số thẻ ghi nhớ (flashcards) đã được tạo trên toàn hệ thống. |
| ADMIN-07 | Quản trị viên có thể xem tổng số lượt ôn tập (reviews) đã thực hiện trên toàn hệ thống. |
| ADMIN-08 | Dashboard hiển thị các thống kê dưới dạng thẻ (card) trực quan với icon và số liệu nổi bật. |

#### 2.9.3 Quản lý người dùng

| Mã | Yêu cầu |
|---|---|
| ADMIN-09 | Quản trị viên có thể xem danh sách tất cả người dùng với thông tin: tên hiển thị, email, role, trạng thái (active/banned), ngày tạo tài khoản. |
| ADMIN-10 | Quản trị viên có thể tìm kiếm người dùng theo tên hoặc email. |
| ADMIN-11 | Danh sách người dùng hỗ trợ phân trang (page, pageSize) để xử lý số lượng lớn. |
| ADMIN-12 | Quản trị viên có thể ban (cấm) một người dùng, ngăn người dùng đó đăng nhập vào hệ thống. |
| ADMIN-13 | Quản trị viên có thể unban (bỏ cấm) một người dùng đã bị ban trước đó. |
| ADMIN-14 | Quản trị viên có thể thay đổi role của người dùng (ví dụ: từ "user" sang "admin" hoặc ngược lại). |
| ADMIN-15 | Hệ thống hiển thị dialog xác nhận trước khi thực hiện các hành động ban/unban/thay đổi role. |

#### 2.9.4 Nhật ký sử dụng AI

| Mã | Yêu cầu |
|---|---|
| ADMIN-16 | Quản trị viên có thể xem danh sách nhật ký sử dụng AI của tất cả người dùng. |
| ADMIN-17 | Mỗi bản ghi nhật ký AI hiển thị: tên người dùng, loại tính năng AI đã sử dụng, thời gian sử dụng. |
| ADMIN-18 | Danh sách nhật ký AI hỗ trợ phân trang (page, pageSize). |

#### 2.9.5 Báo cáo nội dung

| Mã | Yêu cầu |
|---|---|
| ADMIN-19 | Quản trị viên có thể xem danh sách các báo cáo (reports) từ người dùng về nội dung vi phạm. |
| ADMIN-20 | Mỗi báo cáo hiển thị: người báo cáo, nội dung bị báo cáo, lý do, trạng thái (pending/approved/rejected), ngày tạo. |
| ADMIN-21 | Quản trị viên có thể phê duyệt (approve) một báo cáo — xác nhận nội dung vi phạm và thực hiện hành động tương ứng. |
| ADMIN-22 | Quản trị viên có thể từ chối (reject) một báo cáo — xác nhận nội dung không vi phạm. |
| ADMIN-23 | Danh sách báo cáo hỗ trợ phân trang (page, pageSize). |

**Hướng dẫn test trên Android (cần tài khoản admin):**

| Test | Các bước thao tác |
|---|---|
| Truy cập Admin Dashboard | Đăng nhập tài khoản admin → Menu điều hướng hiển thị mục **"Admin Dashboard"** → Nhấn vào → Xem thống kê tổng quan (số user, deck, thẻ, lượt ôn) |
| Quản lý người dùng | Admin Dashboard → Nhấn **"Quản lý người dùng"** → Xem danh sách user → Tìm kiếm user → Nhấn vào user → Chọn **Ban/Unban** hoặc **Đổi role** → Xác nhận |
| Nhật ký AI | Admin Dashboard → Nhấn **"Nhật ký AI"** → Xem danh sách log AI → Cuộn xuống để tải thêm (phân trang) |
| Báo cáo nội dung | Admin Dashboard → Nhấn **"Báo cáo"** → Xem danh sách báo cáo → Nhấn vào báo cáo → Chọn **"Phê duyệt"** hoặc **"Từ chối"** |

---

### 2.12 Tiện ích học tập (UTIL)

| Mã | Yêu cầu |
|---|---|
| UTIL-01 | Ứng dụng có tab **Tiện ích** trong thanh điều hướng chính để gom các công cụ hỗ trợ học tập. |
| UTIL-02 | Màn hình Utilities Hub hiển thị tổng quan nhanh gồm: số thẻ đến hạn hôm nay, tổng số thẻ và streak hiện tại. |
| UTIL-03 | Smart Review cho phép chọn một hoặc nhiều deck (hoặc chọn tất cả deck) để tạo bộ câu hỏi ôn tập. |
| UTIL-04 | Smart Review cho phép cấu hình số câu hỏi trong khoảng 5-20 câu và dùng AI tạo câu hỏi dạng điền từ/biến thể ngữ cảnh. |
| UTIL-05 | Smart Review yêu cầu tối thiểu 3 thẻ hợp lệ, hiển thị tiến độ theo từng câu và trang kết quả cuối phiên. |
| UTIL-06 | Flashcard Quiz cho phép chọn deck và số câu 5-20; câu hỏi trắc nghiệm được tạo cục bộ từ dữ liệu thẻ hiện có. |
| UTIL-07 | Flashcard Quiz yêu cầu tối thiểu 4 thẻ để tạo đáp án nhiễu và hiển thị điểm số cuối phiên. |
| UTIL-08 | Memo Chat hỗ trợ trò chuyện nhiều lượt, hiển thị trạng thái đang trả lời và giữ ngữ cảnh hội thoại trong phiên chat. |
| UTIL-09 | Khi AI trả về danh sách từ vựng gợi ý, người dùng có thể lưu từng từ trực tiếp thành flashcard vào deck đã chọn. |
| UTIL-10 | Bong bóng chat nổi có thể bật/tắt, kéo thả và tự hút về cạnh màn hình; chạm vào bong bóng để mở overlay chat. |

**Hướng dẫn test trên Android:**

| Test | Các bước thao tác |
|---|---|
| Mở Utilities Hub | Tab **Tiện ích** -> Kiểm tra hiển thị 3 tool chính: Smart Review, Memo Chat, Flashcard Quiz |
| Smart Review | Tab **Tiện ích** -> Chọn **Smart Review** -> Chọn deck + số câu -> **Bắt đầu** -> Trả lời đến hết -> Kiểm tra màn hình kết quả |
| Flashcard Quiz | Tab **Tiện ích** -> Chọn **Flashcard Quiz** -> Chọn deck + số câu -> **Bắt đầu** -> Trả lời -> Kiểm tra điểm cuối phiên |
| Memo Chat + lưu từ | Tab **Tiện ích** -> Chọn **Memo Chat** -> Gửi câu hỏi -> Chọn icon lưu ở 1 từ gợi ý -> Chọn deck -> Kiểm tra thẻ được tạo |
| Bong bóng chat nổi | Tab **Cài đặt** -> Bật **Bong bóng chat** -> Quay về tab chính -> Kéo bong bóng, thả -> Chạm bong bóng -> Kiểm tra chat overlay mở |

### 2.13 Thú cưng ảo & động lực học tập (PET)

| Mã | Yêu cầu |
|---|---|
| PET-01 | Hệ thống hiển thị thú cưng ảo dạng overlay trên các màn hình tab chính, phía trên thanh điều hướng dưới. |
| PET-02 | Thú cưng hoạt động theo state machine animation (idle, đi bộ, chơi, ngủ, thức dậy, ăn cá, ăn mừng). |
| PET-03 | Trạng thái đói được lưu cục bộ (SharedPreferences), giá trị trong khoảng 0-100. |
| PET-04 | Mức đói tăng theo thời gian: +3 điểm mỗi 3 giờ không học (tối đa 100). |
| PET-05 | Mỗi thẻ được trả lời trong phiên học làm giảm đói 5 điểm; cứ mỗi 5 thẻ sẽ tích lũy 1 phần thưởng cá. |
| PET-06 | Khi quay lại tab chính, cá thưởng tồn đọng được tiêu thụ và phát animation mèo đi tới ăn cá (nhiều cá có thể kèm animation ăn mừng). |
| PET-07 | Hệ thống có bong bóng nhắc học theo ngữ cảnh (đói/đến hạn), có cooldown để tránh hiển thị quá dày. |
| PET-08 | Chạm vào thú cưng kích hoạt animation tương tác với cơ chế cooldown; thao tác chạm bị vô hiệu trên các màn hình làm bài để tránh che nút trả lời. |

**Hướng dẫn test trên Android:**

| Test | Các bước thao tác |
|---|---|
| Hiển thị pet overlay | Đăng nhập -> Vào tab **Trang chủ**/**Bộ thẻ** -> Kiểm tra pet xuất hiện phía trên bottom bar |
| Giảm đói khi học | Bắt đầu phiên học -> Trả lời nhiều thẻ -> Quay về tab chính -> Kiểm tra mèo nhận cá thưởng/animation ăn cá |
| Bubble nhắc học | Để app nghỉ một thời gian hoặc còn nhiều thẻ đến hạn -> Mở lại tab chính -> Kiểm tra bubble gợi ý học |
| Tương tác chạm pet | Ở tab chính chạm vào pet -> Kiểm tra animation phản hồi; vào màn học -> kiểm tra chạm pet không cản thao tác trả lời |

### 2.14 Khôi phục tài khoản, SSO & cập nhật email (ACCOUNT)

| Mã | Yêu cầu |
|---|---|
| ACCOUNT-01 | Người dùng có thể gửi yêu cầu quên mật khẩu bằng email và nhận OTP qua email. |
| ACCOUNT-02 | OTP đặt lại mật khẩu có 6 chữ số, thời hạn ngắn (server quản lý hết hạn và chỉ dùng một lần). |
| ACCOUNT-03 | Người dùng có thể đặt lại mật khẩu bằng email + OTP + mật khẩu mới. |
| ACCOUNT-04 | Ứng dụng hỗ trợ đăng nhập Google bằng ID Token; nếu email chưa tồn tại thì tự tạo tài khoản mới. |
| ACCOUNT-05 | Người dùng đã đăng nhập có thể cập nhật email tài khoản; hệ thống kiểm tra email mới không trùng với tài khoản khác. |
| ACCOUNT-06 | Sau khi cập nhật email thành công, ứng dụng buộc đăng xuất và yêu cầu đăng nhập lại bằng thông tin mới. |

**Hướng dẫn test trên Android:**

| Test | Các bước thao tác |
|---|---|
| Quên mật khẩu thành công | Màn đăng nhập -> **Quên mật khẩu** -> Nhập email -> **Gửi OTP** -> Nhập OTP + mật khẩu mới -> Xác nhận thành công |
| OTP sai/hết hạn | Thực hiện quên mật khẩu -> Nhập OTP sai hoặc OTP cũ -> Kiểm tra hiển thị lỗi hợp lệ |
| Đăng nhập Google | Màn đăng nhập -> **Đăng nhập Google** -> Chọn tài khoản Google -> Kiểm tra vào app thành công |
| Cập nhật email | Tab **Cài đặt** -> **Thay đổi email** -> Nhập email mới -> Lưu -> Kiểm tra app tự đăng xuất và quay về màn đăng nhập |

### 2.15 Cài đặt học tập & nhắc học hằng ngày (SET)

| Mã | Yêu cầu |
|---|---|
| SET-01 | Người dùng có thể cấu hình giới hạn **thẻ mới/ngày** trong khoảng 5-1000. |
| SET-02 | Người dùng có thể cấu hình giới hạn **thẻ ôn tập/ngày** trong khoảng 10-5000. |
| SET-03 | Ứng dụng hỗ trợ bật/tắt chế độ giao diện tối từ màn hình cài đặt. |
| SET-04 | Người dùng có thể bật/tắt bong bóng chat nổi toàn ứng dụng. |
| SET-05 | Người dùng có thể bật/tắt nhắc học hằng ngày và chọn giờ nhắc cụ thể (giờ, phút). |
| SET-06 | Trên Android 13+, khi bật nhắc học hệ thống phải yêu cầu quyền thông báo (`POST_NOTIFICATIONS`) nếu chưa cấp. |
| SET-07 | Nhắc học được lên lịch bằng AlarmManager tại thời điểm chính xác khi có thể, và có cơ chế fallback khi thiết bị không cho exact alarm. |
| SET-08 | Sau khi hiển thị thông báo, hệ thống tự lên lịch lại cho ngày hôm sau. |
| SET-09 | Sau khi thiết bị khởi động lại, nếu nhắc học đang bật thì hệ thống tự khôi phục lịch nhắc. |
| SET-10 | Màn hình Home hiển thị trạng thái đồng bộ nền và cho phép người dùng bấm đồng bộ thủ công. |

**Hướng dẫn test trên Android:**

| Test | Các bước thao tác |
|---|---|
| Đổi hạn mức học/ngày | Tab **Cài đặt** -> Kéo slider thẻ mới/thẻ ôn -> Mở phiên học -> Kiểm tra số lượng thẻ lấy theo cấu hình mới |
| Bật nhắc học | Tab **Cài đặt** -> Bật **Nhắc nhở ôn tập** -> Chọn giờ -> Chờ đến giờ -> Kiểm tra notification xuất hiện |
| Tắt nhắc học | Tab **Cài đặt** -> Tắt **Nhắc nhở ôn tập** -> Kiểm tra không còn thông báo ở các chu kỳ tiếp theo |
| Khôi phục sau reboot | Bật nhắc học -> Khởi động lại thiết bị -> Chờ khung giờ nhắc -> Kiểm tra thông báo vẫn hoạt động |
| Đồng bộ thủ công | Tab **Trang chủ** -> Bấm nút đồng bộ -> Kiểm tra trạng thái đổi từ đang đồng bộ sang thành công/thất bại |

### 2.16 Nhập liệu ngoài hệ thống & đồng bộ Google Sheet (IMPORT)

| Mã | Yêu cầu |
|---|---|
| IMPORT-01 | Người dùng có thể nhập flashcard từ file Excel (`.xlsx`/`.xls`) trong màn chi tiết deck. |
| IMPORT-02 | File Excel được đọc theo cấu trúc: cột A = mặt trước, cột B = mặt sau, cột C = ví dụ (tùy chọn). |
| IMPORT-03 | Các dòng trống (A và B cùng rỗng) được bỏ qua; hệ thống hiển thị kết quả nhập (đã thêm / tổng dòng / dòng bỏ qua). |
| IMPORT-04 | Chủ deck có thể liên kết một URL Google Sheet với deck để dùng làm nguồn dữ liệu đồng bộ. |
| IMPORT-05 | Chủ deck có thể hủy liên kết Google Sheet bất kỳ lúc nào. |
| IMPORT-06 | Khi đồng bộ Google Sheet, hệ thống tải CSV từ sheet public, thêm thẻ mới và bỏ qua bản ghi trùng chính xác (front+back). |
| IMPORT-07 | Hệ thống trả về thống kê đồng bộ (added, skipped, totalRows) và cập nhật lại danh sách thẻ trong deck sau khi sync. |
| IMPORT-08 | Nếu Google Sheet chưa publish/public hoặc không truy cập được, hệ thống trả thông báo lỗi rõ ràng để người dùng xử lý. |

**Hướng dẫn test trên Android:**

| Test | Các bước thao tác |
|---|---|
| Nhập Excel | Vào 1 deck -> **Nhập từ file Excel** -> Chọn file đúng định dạng -> Kiểm tra số thẻ được thêm và thông báo kết quả |
| Liên kết Google Sheet | Vào 1 deck -> **Liên kết Google Sheet** -> Dán link -> **Liên kết** -> Kiểm tra trạng thái đã liên kết |
| Đồng bộ Google Sheet | Deck đã liên kết -> Bấm **Đồng bộ** -> Kiểm tra số thẻ thêm mới/bỏ qua hiển thị đúng |
| Hủy liên kết | Deck đã liên kết -> Bấm **Hủy liên kết** -> Kiểm tra nút quay về trạng thái chưa liên kết |
| Lỗi sheet private | Liên kết/sync với sheet chưa public -> Kiểm tra thông báo yêu cầu publish/public sheet |

### 2.17 Chia sẻ nâng cao & kiểm duyệt nội dung (MOD)

| Mã | Yêu cầu |
|---|---|
| MOD-01 | Hệ thống tạo mã chia sẻ ngắn cho deck và đảm bảo tính duy nhất của mã. |
| MOD-02 | Chủ deck có thể đặt quyền mặc định cho người tham gia: `read` hoặc `edit`. |
| MOD-03 | Người dùng có thể xem preview deck theo mã chia sẻ (tên deck, chủ sở hữu, số thẻ) trước khi tham gia. |
| MOD-04 | Người dùng có thể tham gia/rời deck chia sẻ; hệ thống lưu trạng thái subscription và quyền truy cập tương ứng. |
| MOD-05 | Chủ deck có thể xem danh sách subscriber, đổi quyền từng subscriber, hoặc kick subscriber khỏi deck. |
| MOD-06 | Subscriber quyền `read` chỉ được xem/học; subscriber quyền `edit` được phép thêm/sửa/xóa thẻ trong deck chia sẻ. |
| MOD-07 | Người dùng (không phải chủ deck) có thể gửi báo cáo vi phạm nội dung trực tiếp từ màn hình deck được chia sẻ. |
| MOD-08 | Admin có thể duyệt báo cáo với 2 hành động `approve`/`reject`; khi `approve`, nội dung vi phạm bị ẩn/xóa mềm khỏi hệ thống. |
| MOD-09 | Chủ sở hữu nội dung vi phạm nhận thông báo vi phạm trong app và có thể xác nhận đã đọc để không hiển thị lặp lại. |

**Hướng dẫn test trên Android:**

| Test | Các bước thao tác |
|---|---|
| Tạo mã và tham gia deck | **User A**: mở deck -> chia sẻ -> lấy mã. **User B**: vào **Nhập mã chia sẻ** -> xem preview -> tham gia -> kiểm tra deck xuất hiện |
| Quản lý subscriber | **User A**: mở dialog chia sẻ -> đổi quyền 1 subscriber read/edit -> kiểm tra quyền thực thi ở tài khoản subscriber |
| Read vs Edit permission | **User B (read)**: mở deck chia sẻ -> thử thêm/sửa/xóa thẻ (phải bị chặn). **User C (edit)**: thao tác tương tự (được phép) |
| Rời/kick khỏi deck | **Subscriber**: rời deck -> deck biến mất khỏi thư viện. **Owner**: kick subscriber -> subscriber mất quyền truy cập |
| Báo cáo và thông báo vi phạm | **Subscriber**: báo cáo deck -> **Admin** duyệt approve -> **Owner** đăng nhập lại -> nhận dialog thông báo vi phạm |


## 3. Ma trận tính năng Free / Premium

| Tính năng | Gói miễn phí | Gói Premium (Tương lai) |
|---|---|---|
| Tạo bộ thẻ & thẻ ghi nhớ |  Không giới hạn |  Không giới hạn |
| Hàng đợi học hàng ngày |  Đầy đủ (40 mới / 150 ôn tập) |  Tùy chỉnh giới hạn |
| Tạo và chỉnh sửa thẻ thủ công |  Đầy đủ |  Đầy đủ |
| TTS (engine thiết bị) | Đầy đủ | Đầy đủ |
| Đồng bộ cloud |  1 thiết bị |  Đa thiết bị |
| AI tạo thẻ (từ text) |  20 lần/ngày |  Không giới hạn |
| AI tạo thẻ (từ PDF/DOCX) |  5 file/ngày | Không giới hạn |
| AI tạo ví dụ |  20 lần/ngày |  Không giới hạn |
| AI tạo hình ảnh |  Không có |  Không giới hạn |
| AI Tutor chat |  10 tin nhắn/ngày |  Không giới hạn |
| Phân tích từ đa nghĩa |  5 lần/ngày |  Không giới hạn |
| Chia sẻ bộ thẻ |  Đầy đủ |  Đầy đủ |
| Thư viện ảnh cộng đồng |  Đầy đủ |  Đầy đủ |
| Chế độ Quiz |  Đầy đủ |  Đầy đủ |
| Thống kê học tập | Cơ bản |  Nâng cao |


> **Lưu ý**: Giới hạn AI được reset hàng ngày vào lúc nửa đêm (giờ địa phương). Lượt chưa dùng không được cộng dồn.

---

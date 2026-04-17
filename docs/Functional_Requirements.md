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
| AI-09 | Người dùng có thể mở AI Tutor chat và đặt câu hỏi bằng tiếng Việt hoặc tiếng Anh. |
| AI-10 | AI Tutor phản hồi theo phong cách thân thiện, dễ hiểu, sử dụng ví dụ và phép so sánh. |
| AI-11 | AI Tutor duy trì lịch sử hội thoại trong phiên chat. |
| AI-12 | Hệ thống tạo câu hỏi trắc nghiệm từ thẻ ghi nhớ cho chế độ Quiz (1 đáp án đúng + 3 đáp án nhiễu). |
| AI-13 | Hệ thống theo dõi số lần sử dụng AI mỗi ngày cho từng người dùng. |
| AI-14 | Hệ thống giới hạn sử dụng AI hàng ngày cho người dùng miễn phí. |
| AI-15 | Hệ thống hiển thị số lần AI còn lại cho người dùng. |
| AI-16 | Khi thẻ bị sai ≥ 3 lần, hệ thống đề xuất AI hỗ trợ (giải thích đơn giản hơn, thêm ví dụ). |

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


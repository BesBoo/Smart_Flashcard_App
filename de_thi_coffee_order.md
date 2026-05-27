# ĐỀ THI THỰC HÀNH - Android Java (90 phút)

## 1. Bối cảnh bài toán

Xây dựng ứng dụng quản lý đơn hàng quán cà phê ở mức tối giản. Mỗi bản ghi gồm tên khách hàng, số điện thoại, loại đồ uống, phân loại khách, tổng tiền, trạng thái thành viên và trạng thái đã giao.

## 2. Dữ liệu cần quản lý

| Trường | Yêu cầu |
|--------|---------|
| Database | `coffee_lite.db` |
| Bảng | `records(id, name, phone, category, type, amount, flag, active)` |
| Ý nghĩa bản ghi | tên khách hàng, số điện thoại, loại đồ uống, phân loại khách, tổng tiền, thành viên thân thiết và trạng thái đã giao |
| Category trên Spinner | Espresso, Latte, Cappuccino, Smoothie |
| Radio type | DineIn / TakeAway |
| Amount field | Total price |
| CheckBox flag | Loyalty member |
| Switch active | Delivered |

## 3. Yêu cầu chức năng

1. Tạo project Java có MainActivity và AndroidManifest.xml.
2. Thiết kế giao diện XML bằng LinearLayout, có TextView tiêu đề, 3 EditText, Spinner, RadioGroup, CheckBox, Switch, 3 Button Add/Delete/Clear, GridView và ListView.
3. Spinner hiển thị danh sách category bằng ArrayAdapter.
4. Add: kiểm tra name, phone, amount không rỗng; amount phải là số; thêm vào SQLite và cập nhật ListView.
5. ListView hiển thị dạng: id - name - category - amount. Click item để chọn bản ghi cần xóa.
6. Delete: chỉ xóa khi đã chọn item; phải hỏi xác nhận bằng AlertDialog.
7. GridView có 3 filter: ALL, ACTIVE, HIGH. HIGH nghĩa là amount >= ngưỡng trong ExamConfig.
8. Long click ListView mở màn hình gọi điện bằng ACTION_DIAL với số điện thoại của item.
9. ActionBar menu gồm About, Save Last Category, Load Last Category, Open Website.
10. SharedPreferences lưu và đọc category gần nhất; mọi thao tác chính có Toast.

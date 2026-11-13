# HƯỚNG DẪN THAO TÁC GIAO DIỆN QUẢN TRỊ (USER MANUAL)
## Nền tảng Tài chính, Kế toán & Hóa đơn điện tử Đa kênh (Fashion Platform)

---

## 1. Hướng dẫn Truy cập Giao diện Web Portal

Hệ thống được tích hợp sẵn giao diện Web Portal trực quan phục vụ công việc của Kế toán viên, Kế toán trưởng và Quản trị viên.

- **Địa chỉ truy cập**: [http://localhost:8088/](http://localhost:8088/)
- **Yêu cầu môi trường**: Trình duyệt Chrome, Edge, Firefox hoặc Safari bất kỳ.
- **Thanh chuyển đổi vai trò (Header)**:
  - Góc trên cùng bên phải có menu **"Đăng nhập là"**:
    - **Kế toán viên (`ROLE_FINANCE_ACCOUNTANT`)**: Lập hóa đơn, tạo bút toán, kiểm tra công nợ.
    - **Kế toán trưởng (`ROLE_FINANCE_MANAGER`)**: Phê duyệt cấu hình thuế (Maker-Checker), đóng/mở kỳ kế toán.
    - **Quản trị viên (`ROLE_ADMIN`)**: Quản trị hệ thống và pháp nhân.

---

## 2. Hướng dẫn Thao tác Từng Phân Hệ (Sprints 1 -> 5)

### 🧾 Phân hệ 1: Hóa đơn điện tử E-Invoice (Sprint 5)

> Tuân thủ chuẩn **Nghị định 123/2020/NĐ-CP** và **Thông tư 78/2021/TT-BTC**.

1. **Xem danh sách hóa đơn**:
   - Chọn menu bên trái: **"Hóa đơn điện tử (S5)"**.
   - Bảng hiển thị danh sách các hóa đơn gần đây kèm số HĐ, ký hiệu mẫu số, tên khách hàng, mã số thuế, tổng tiền và trạng thái.
2. **Lập hóa đơn mới**:
   - Nhấn nút xanh **"+ Lập Hóa Đơn Mới"** ở góc trên bên phải.
   - Hộp thoại xuất hiện:
     - **Khách hàng**: Chọn loại khách *Cá nhân* hoặc *Doanh nghiệp*. Nếu là doanh nghiệp, nhập Tên Công ty và Mã số thuế (10 hoặc 14 chữ số hợp lệ).
     - **Mặt hàng**: Nhập tên sản phẩm (VD: Đầm dạ hội, Áo sơ mi), đơn vị tính, số lượng, đơn giá.
     - **Thuế suất GTGT**: Chọn `8%` (ưu đãi) hoặc `10%` (chuẩn) và chiết khấu (nếu có).
   - Nhấn **"Tạo Hóa Đơn Nháp"**.
   - Hệ thống tự động tính toán tiền trước thuế, tiền thuế và tổng thanh toán, lưu ở trạng thái `DRAFT`.
3. **Kiểm tra quy chuẩn dữ liệu (Validate)**:
   - Trên dòng hóa đơn nháp, nhấn nút **"Kiểm tra"**.
   - Hệ thống chạy thuật toán thẩm định: kiểm tra MST doanh nghiệp, số lượng, đơn giá không âm và tính cân đối tiền tệ. Khi hợp lệ, trạng thái chuyển sang `PENDING_ISSUANCE`.
4. **Phát hành hóa đơn chính thức (Issue)**:
   - Nhấn nút **"Phát hành"**.
   - Hệ thống chuyển lệnh tới Provider HĐĐT, ký số điện tử, cấp **Số hóa đơn** (VD: `0000001`), gán Ký hiệu (`C26TDD`), Mẫu số (`1/001`) và **Mã Cơ quan Thuế** (VD: `TCT-1736484E`). Trạng thái chuyển sang `ISSUED`.
5. **Xem chi tiết & Chữ ký số**:
   - Nhấn nút **"Chi tiết"** để xem toàn bộ cấu trúc dữ liệu JSON, snapshot người bán/người mua và chứng từ lưu trữ.

---

### 📒 Phân hệ 2: Sổ cái & Quản lý Quỹ tiền (Sprint 4)

1. **Xem số dư Quỹ tiền mặt & Ngân hàng**:
   - Chọn menu **"Sổ cái & Quỹ tiền (S4)"**.
   - Khối **Quỹ tiền mặt & Ngân hàng** hiển thị số dư thực tế của Quỹ tiền mặt cửa hàng (TK 111) và Tài khoản ngân hàng VCB (TK 112).
2. **Tra cứu Cây tài khoản Kế toán (Chart of Accounts - COA)**:
   - Khối bên phải hiển thị toàn bộ danh mục tài khoản chuẩn (111, 112, 131, 331, 511, 632, 641, 642).
3. **Thử nghiệm Kiểm soát Cân đối Bút toán Kép (Double-entry Balance)**:
   - **Thử tạo bút toán lệch**: Nhấn nút đỏ *"Thử tạo bút toán Lệch"*, hệ thống lập tức cảnh báo lỗi vi phạm nguyên tắc Nợ != Có và chặn ghi sổ.
   - **Ghi sổ bút toán cân bằng**: Nhập số tiền Nợ TK 111 = Có TK 511 (VD: 500.000đ) và nhấn *"Ghi sổ Bút toán Cân bằng"*.

---

### 💳 Phân hệ 3: Giao dịch & Chi phí Đa Kênh (Sprint 3)

1. **Mô phỏng Nhận đơn hàng Đa kênh**:
   - Chọn menu **"Giao dịch & Chi phí (S3)"**.
   - Màn hình cung cấp 4 nút mô phỏng:
     - 🟠 **Đơn hàng Shopee**: Tự động bóc tách tiền bán hàng, khấu trừ phí sàn Shopee và sinh công nợ phải thu từ Shopee.
     - ⚫ **Đơn hàng TikTok Shop**: Tự động bóc tách doanh thu và khấu trừ phí nền tảng TikTok.
     - 🔵 **Website Trực tuyến**: Ghi nhận thanh toán cổng VNPAY/Chuyển khoản trực tiếp vào TK ngân hàng.
     - 🟢 **Cửa hàng POS (Offline)**: Ghi nhận thu tiền mặt tại quầy vào Quỹ tiền mặt (TK 111).

---

### 🏢 Phân hệ 4: Pháp nhân & Cấu hình Thuế (Sprint 2)

- Chọn menu **"Pháp nhân & Thuế (S2)"**.
- Hiển thị danh mục các thực thể pháp nhân (Công ty thời trang, Chi nhánh) kèm Mã số thuế, Địa chỉ trụ sở và trạng thái hoạt động `ACTIVE`.

---

### ⚙️ Phân hệ 5: Hạ tầng & Trạng thái Hệ thống (Sprint 1)

- Chọn menu **"Hạ tầng & Hệ thống (S1)"**.
- Giám sát trạng thái 3 trụ cột hạ tầng cốt lõi:
  - Database: **PostgreSQL 16** (Đã chạy các script migration từ V1 đến V5).
  - Message Broker: **RabbitMQ 3.13** (Transactional Outbox & Idempotent Consumer).
  - Object Storage: **MinIO / AWS S3** (Lưu trữ file XML hóa đơn gốc và bản PDF thể hiện).

---

## 3. Quy trình Bổ sung cho các Sprint Tiếp theo (Sprint 6 -> 12)

Kể từ Sprint 6 trở đi, mỗi khi một sprint hoàn thành:
1. **Frontend UI** tương ứng sẽ được tích hợp ngay vào Web Portal để người dùng có thể thao tác và kiểm chứng bằng mắt.
2. **Tài liệu Hướng dẫn Thao tác (`USER_MANUAL.md`)** sẽ được cập nhật thêm các bước thực hiện của phân hệ mới.

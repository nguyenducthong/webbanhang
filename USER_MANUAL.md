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

## 2. Hướng dẫn Thao tác Từng Phân Hệ (Sprints 1 -> 8)

### ⚖️ Phân hệ 1: Đối soát Sàn TMĐT & Đơn Vị Vận Chuyển COD (Sprint 7 & Sprint 8)

> Hệ thống phân tích dữ liệu sao kê quyết toán đa kênh (**Shopee Pay, TikTok Shop, Giao Hàng Nhanh COD, ViettelPost**), thuật toán khớp tự động (Matching Engine), hàng đợi xử lý ngoại lệ (Exception Queue) và **Báo cáo Biến động Độ lệch Phí Sàn (Fee Variance Report)**.

1. **Xem Bảng Thống Kê KPI Đối Soát**:
   - Chọn menu bên trái: **"Đối soát Sàn & COD (S7 & S8)"**.
   - Thẻ thống kê thời gian thực hiển thị:
     - **Tỷ lệ Khớp Tự động (%)**: Tỷ lệ đơn hàng khớp chính xác cả mã đơn và số tiền thực nhận (`EXACT_MATCH`).
     - **Số Đơn Khớp Hoàn Toàn**: Các đơn có chênh lệch `<= 0.0001 VND` được đánh dấu `RESOLVED` tự động.
     - **Số Đơn Phát Hiện Lệch**: Đơn bị chênh lệch phí sàn (`FEE_MISMATCH`) hoặc sàn có mà nội bộ thiếu (`UNMATCHED_EXTERNAL`).
     - **Tổng Giá Trị Tiền Lệch**: Tổng số tiền chênh lệch cần Kế toán trưởng phê duyệt bù trừ hoặc khiếu nại sàn.
2. **Nạp Sao Kê Quyết Toán Đa Kênh (CSV Ingestion)**:
   - Hệ thống cung cấp sẵn 3 nút nạp dữ liệu mẫu thực tế:
     - 🟠 **Mẫu Shopee**: Bóc tách báo cáo ví Shopee Pay (`order_sn`, `payout_amount`, `fee`, `payout_date`).
     - ⚫ **Mẫu TikTok Shop (Sprint 8)**: Bóc tách cơ cấu quyết toán đa thành phần gồm: Tiền khách trả, Trợ giá voucher sàn, Chiết khấu voucher shop, Phí hoa hồng nền tảng (Commission), Phí xử lý thanh toán và Phí tiếp thị liên kết (Affiliate).
     - 🟢 **Mẫu COD GHN (Sprint 8)**: Bóc tách sao kê chuyển khoản tiền thu hộ COD từ Giao Hàng Nhanh (`waybill_code`, `order_code`, `cod_amount`, `shipping_fee`, `insurance_fee`, `return_fee`, `net_payout`).
   - Nhấn nút **"Khởi Chạy Đối Soát & Khớp Lệnh"**: Hệ thống tự động nhận diện Parser tương ứng theo kênh, bóc tách dòng chi tiết và đối chiếu mã đơn với `finance_transactions`.
3. **Quản Lý Danh Sách Đợt Đối Soát (Batches)**:
   - Danh sách bảng đợt đối soát hiển thị Mã đợt (VD: `REC-SP-...`, `REC-TT-...`, `REC-COD-...`), Tên tệp, Kênh bán, Tiến độ khớp (thanh phần trăm trực quan), Tổng tiền sàn báo và Trạng thái đợt (`COMPLETED`, `DISCREPANCY_FOUND`).
   - Nhấn nút **"Chi tiết"** trên bất kỳ dòng đợt nào để tải bảng chi tiết các đơn hàng bên dưới.
4. **Phê Duyệt Xử Lý Chênh Lệch (Exception Resolution Queue)**:
   - Kế toán viên / Kế toán trưởng kiểm tra bảng **"Chi Tiết Các Dòng Đối Soát"**.
   - Các dòng có trạng thái `CHỜ KẾ TOÁN DUYỆT` (PENDING) sẽ hiển thị nút màu xanh **"Duyệt Xử Lý"**.
   - Nhấn **"Duyệt Xử Lý"** -> Nhập lý do phê duyệt (VD: *"Chấp thuận bù trừ chi phí voucher sàn tháng 11"* hoặc *"Xác nhận ghi nhận cước vận chuyển vượt cân nặng"*).
   - Hệ thống cập nhật trạng thái dòng thành `RESOLVED`, lưu người duyệt (`performedBy`) và thời gian duyệt.
   - Khi tất cả các dòng bất thường trong đợt đã được duyệt xử lý, đợt đối soát sẽ tự động chuyển trạng thái sang **`COMPLETED`**.
5. **Báo Cáo Biến Động Độ Lệch Phí Sàn (Marketplace Fee Variance Report - Sprint 8)**:
   - Nằm ngay dưới bảng chi tiết đối soát.
   - Bảng tổng hợp đối soát chi phí từng kênh so với biểu phí hợp đồng:
     - **Shopee**: Biểu phí chuẩn 7.5%
     - **TikTok Shop**: Biểu phí chuẩn 8.5%
     - **COD GHN**: Biểu phí chuẩn 3.0%
   - Cột **"Tỷ Lệ Phí (Thực Tế vs Chuẩn)"**: Tính toán tỷ lệ phần trăm thực tế sàn thu trên doanh thu gộp.
   - Cột **"Đánh Giá"**:
     - `NORMAL` (Màu xanh): Tỷ lệ phí thực tế bám sát hợp đồng (chênh lệch `<= 1%`).
     - `HIGH_VARIANCE` (Cảnh báo màu đỏ): Tỷ lệ phí thực tế vượt ngưỡng (chênh lệch `> 1%`). Hệ thống tự động đưa ra khuyến nghị kiểm tra lại việc sàn trừ nhầm cước vượt cân hoặc tính trùng voucher.

---

### 🔌 Phân hệ 2: Tích hợp Nhà cung cấp HĐĐT & Webhook CQT (Sprint 6)

> Tích hợp Production Gateway (VNPT Invoice), Token Caching, Khóa bi quan chống cấp trùng và Webhook phản hồi từ Cơ quan Thuế (CQT).

1. **Giám sát Kết nối Nhà cung cấp & Token Caching**:
   - Chọn menu bên trái: **"Tích hợp Provider & Webhook (S6)"**.
   - Thẻ **VNPT Invoice Gateway**: Hiển thị trạng thái kết nối `Active`, API Endpoint (`https://api-einvoice.vnpt.vn/v2`) và cơ chế làm mới Token tự động (24h).
   - Nhấn nút **"Ping Kiểm tra Kết nối"**: Kiểm tra trực tiếp độ trễ gateway và hiệu lực của Bearer Token trong cache.
2. **Cứu hộ Hóa đơn Treo / Mất mạng (Stuck & Unknown Recovery Console)**:
   - Khối **"Cứu hộ Hóa đơn Treo"** tự động liệt kê các hóa đơn đang ở trạng thái bất thường (`UNKNOWN_RESULT` hoặc `SUBMITTED` quá thời gian chờ).
   - Nhấn nút **"Đồng bộ ngay" (Force Sync)** trên từng dòng: Hệ thống sẽ gọi trực tiếp sang VNPT/CQT để tra cứu kết quả. Nếu NCC đã cấp số, hóa đơn sẽ tự động chuyển thành `ISSUED` mà **tuyệt đối không bị phát hành lần 2**.
   - Đồng thời, Background Job chạy ngầm `@Scheduled` mỗi 1 phút cũng tự động xử lý cứu hộ các hóa đơn này.
3. **Bộ Giả lập Webhook Cơ quan Thuế (Tax Authority Webhook Simulator)**:
   - Dùng để kiểm tra phản ứng của hệ thống khi Cục Thuế trả kết quả bất đồng bộ:
     - Nhập **Mã Hóa Đơn (UUID)** cần gửi kết quả.
     - Chọn **Sự kiện từ CQT**:
       - *CQT Chấp thuận*: Hệ thống tự động gán mã CQT (VD: `TCT-VNPT-8899AA`), chuyển trạng thái hóa đơn sang `ISSUED`.
       - *CQT Từ chối*: Hệ thống ghi lại lý do từ chối và đổi trạng thái sang `REJECTED`.
     - Nhấn nút **"Gửi Webhook Kèm Chữ Ký HMAC-SHA256"**: Hệ thống sẽ tự động băm chữ ký điện tử với `webhook_secret`, gửi tới endpoint `/api/v1/invoices/webhook/VNPT`, kiểm tra bảo mật chống giả mạo và ghi log.
4. **Xem Nhật ký Webhook Logs**:
   - Bảng dưới cùng hiển thị thời gian, nhà cung cấp, loại sự kiện, trạng thái xử lý (`SUCCESS` / `FAILED`) và chuỗi chữ ký số HMAC-SHA256 được lưu trữ phục vụ đối soát.

---

### 🧾 Phân hệ 2: Hóa đơn điện tử E-Invoice Core (Sprint 5)

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

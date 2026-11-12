# Fashion E-commerce Platform — Finance, E-Invoice, Reconciliation & Reporting

Bộ tài liệu thiết kế kỹ thuật để triển khai các module Finance, E-Invoice, Reconciliation và Reporting cho nền tảng bán thời trang đa kênh.

## Quyết định đầu vào

- Loại hình kinh doanh: `UNDECIDED` — chưa chốt; không hard-code thông tin pháp nhân/thuế.
- Xuất hóa đơn: tự động theo `invoice_issuance_rules`, có kiểm tra điều kiện và xử lý ngoại lệ.
- Kênh bán: `WEBSITE`, `SHOPEE`, `TIKTOK`, `OFFLINE`.
- Backend: Java 21 + Spring Boot 3.x.
- Database: PostgreSQL.
- Kiến trúc: Modular Monolith, Outbox + RabbitMQ cho xử lý bất đồng bộ.
- Chứng từ: S3-compatible storage (MinIO / R2 / S3).

## Danh mục

1. `01-business-requirements.md` — phạm vi nghiệp vụ, quy tắc và trạng thái.
2. `02-architecture-and-erd.md` — kiến trúc, luồng sự kiện, ERD Mermaid.
3. `03-database-ddl.sql` — PostgreSQL DDL khởi tạo cho các bảng cốt lõi.
4. `04-openapi.yaml` — OpenAPI 3.1 skeleton cho các API chính.
5. `05-implementation-roadmap.md` — sprint, task và tiêu chí nghiệm thu.
6. `06-codex-gemini-prompt.md` — prompt triển khai cho Codex/Gemini.

## Cách sử dụng

- Đọc requirements và architecture trước.
- Review DDL với người phụ trách kế toán/nghiệp vụ trước khi chạy production.
- Dùng OpenAPI làm hợp đồng API ban đầu; cập nhật schema khi chốt DTO.
- Chạy migration trong môi trường local/dev trước; không chạy trực tiếp trên production.
- Tích hợp nhà cung cấp hóa đơn chỉ sau khi có hợp đồng, tài liệu API hiện hành, credential và môi trường UAT.

## Các giả định chưa được chốt

- Loại hình pháp lý, phương pháp tính thuế, quy tắc/thời điểm lập hóa đơn.
- Nhà cung cấp hóa đơn và loại hóa đơn/mẫu/ký hiệu.
- Phương pháp tính giá vốn (FIFO hoặc bình quân gia quyền).
- Hệ thống kế toán ngoài cần tích hợp.
- API/quyền truy cập thực tế của Shopee và TikTok.
- Cổng thanh toán, ngân hàng và hãng vận chuyển.

Các nội dung trên phải được cấu hình hoặc xác nhận trước production; không được suy diễn từ thiết kế kỹ thuật.

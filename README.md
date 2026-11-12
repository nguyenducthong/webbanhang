# Fashion E-Commerce Platform — Finance, E-Invoice, Reconciliation & Reporting

Hệ thống quản lý Thương mại điện tử Thời trang đa kênh (Website, Shopee, TikTok Shop, POS Offline) tích hợp chặt chẽ với module Tài chính, Hóa đơn điện tử, Đối soát và Báo cáo quản trị.

---

## 1. Yêu cầu môi trường

- **Java**: 21 LTS (Oracle JDK hoặc Eclipse Temurin)
- **Maven**: 3.8+
- **Docker & Docker Compose**: Docker 24+
- **PostgreSQL**: 16 (chạy qua Docker Compose)
- **RabbitMQ**: 3 Management (chạy qua Docker Compose)
- **MinIO**: S3-compatible Object Storage (chạy qua Docker Compose)

---

## 2. Khởi động môi trường hạ tầng (Local Docker)

Khởi động PostgreSQL, RabbitMQ và MinIO:

```powershell
docker compose up -d
docker compose ps
```

### Thông tin truy cập dịch vụ:

| Dịch vụ | URL / Port | Tài khoản | Mật khẩu |
| :--- | :--- | :--- | :--- |
| **PostgreSQL 16** | `localhost:5432` / db: `fashion_db` | `postgres` | `postgres_password` |
| **RabbitMQ Web UI** | `http://localhost:15672` | `admin` | `admin_password` |
| **RabbitMQ AMQP** | `localhost:5672` | `admin` | `admin_password` |
| **MinIO Web Console** | `http://localhost:9001` | `minioadmin` | `minioadmin_password` |
| **MinIO S3 API** | `http://localhost:9000` | `minioadmin` | `minioadmin_password` |

---

## 3. Biên dịch và Chạy kiểm thử tự động

```powershell
# Thiết lập JAVA_HOME trỏ tới JDK 21 nếu cần
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"

# Biên dịch và chạy toàn bộ unit/integration test
mvn clean test
```

---

## 4. Chạy ứng dụng Spring Boot

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
mvn spring-boot:run -pl fashion-app
```

Kiểm tra trạng thái hệ thống:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/system/health"
```

---

## 5. Cấu trúc Module

- `fashion-common`: Value Object `Money` (BigDecimal scale=4), Currency, User Context, Audit DTO, Base Exceptions.
- `fashion-infrastructure`: Transactional Outbox Relay, Idempotent Consumer, MinIO Object Storage, Audit Log Service, Flyway Migrations.
- `fashion-app`: Spring Boot Application Bootstrap, Context Filter, Health Endpoints.
- `fashion_finance_documentation/`: Bộ tài liệu kỹ thuật, kiến trúc, DDL, OpenAPI và Roadmap 12 Sprints.


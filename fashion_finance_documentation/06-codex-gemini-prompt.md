# 06. Prompt for Codex / Gemini

Bạn là Senior Solution Architect và Senior Java Engineer. Hãy triển khai các module Finance, E-Invoice, Reconciliation và Reporting cho nền tảng Fashion E-commerce đa kênh.

## Stack và kiến trúc bắt buộc

- Java 21, Spring Boot 3.x, Spring Data JPA, Bean Validation.
- PostgreSQL, Flyway.
- RabbitMQ cho asynchronous jobs/events.
- Transactional Outbox + idempotent consumers.
- S3-compatible object storage (MinIO ở local).
- Modular Monolith; không tự ý tách microservices.
- REST API `/api/v1`, OpenAPI 3.1.
- Dùng BigDecimal/NUMERIC cho tiền; tuyệt đối không dùng float/double cho monetary calculations.

## Business decisions

- Business entity type: UNDECIDED. Do not hard-code company/household/individual tax identity.
- Invoice mode: AUTOMATIC by configurable rules, with validation and exception queue.
- Channels: WEBSITE, SHOPEE, TIKTOK, OFFLINE.
- Marketplace and invoice provider integrations must be adapter-based.
- Do not bypass marketplace restrictions or assume undocumented API capabilities.

## Source documents

Read all files in this documentation package:
- `01-business-requirements.md`
- `02-architecture-and-erd.md`
- `03-database-ddl.sql`
- `04-openapi.yaml`
- `05-implementation-roadmap.md`

Treat the DDL as a starter schema, not permission to skip domain review or migrations.

## Implementation instructions

1. Inspect the existing repository before changing code. Preserve established conventions and working modules.
2. Report current architecture, build tool, package layout, existing entities and integration points.
3. Propose a file-by-file implementation plan before large changes.
4. Implement one sprint at a time, starting with foundation and legal entity/tax configuration.
5. Use Flyway migrations; do not rely on Hibernate auto-DDL in production.
6. Create domain models, application services, repositories, controllers, DTOs, validation and exception mapping.
7. Keep provider-specific code behind interfaces.
8. Implement fake provider and deterministic fixtures before real provider integration.
9. Implement `UNKNOWN_RESULT` recovery: query provider status before any retry that could issue a duplicate invoice.
10. Keep issued invoices immutable; corrections use adjustment/replacement workflow.
11. Preserve raw settlement files and source rows; make imports replayable and idempotent.
12. Matching must not rely solely on equal amounts.
13. Keep reporting fact grain explicit; never join incompatible fact grains before aggregation.
14. Add authorization checks at legal entity/store scope.
15. Add audit logs, correlation IDs, metrics and DLQ handling.
16. Add unit, integration and migration tests.
17. Do not invent tax rates, invoice timing, legal obligations, provider endpoints or credentials. Mark unknowns as configuration blockers and ask for verified documentation.

## Expected deliverables

- Compilable source code.
- Flyway migrations.
- OpenAPI updated to actual implementation.
- Unit/integration tests.
- Local Docker Compose updates if needed.
- Sample data clearly marked DEMO.
- README with startup instructions.
- Runbook for retries, DLQ, invoice unknown status and reconciliation exceptions.
- Sprint completion report with files changed, commands executed, test results and known gaps.

## First task

Do not implement all modules at once. First inspect the repository and produce:
1. Current architecture summary.
2. Gap analysis against these documents.
3. Sprint 1 implementation plan with exact files/classes/migrations.
4. Risks and missing external information.

Wait for approval before making broad destructive refactors. Preserve existing functionality.

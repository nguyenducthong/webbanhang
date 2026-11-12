# 05. Implementation Roadmap

## Delivery principles

- Each sprint ends with runnable code, migration, tests and updated API docs.
- No production invoice issuance until legal entity, tax treatment, provider UAT and accountant approval are complete.
- Build provider-neutral contracts before vendor-specific integration.
- Build import/reconciliation as replayable jobs.
- Add observability and audit from the first finance sprint.

## Sprint plan

### Sprint 1 — Foundation
- Create modules and package boundaries.
- Configure PostgreSQL, Flyway, RabbitMQ, object storage.
- Add Outbox publisher and processed-event table.
- Add RBAC permissions and audit foundation.
- Add Money value object using BigDecimal.
- Deliver: local Docker Compose, health checks, migration pipeline.

### Sprint 2 — Legal Entity and Tax Configuration
- CRUD legal entity and effective-dated addresses.
- Tax configuration model with approval workflow.
- Legal entity scoping in repository/service layer.
- Deliver: admin APIs and tests; seed only demo/sandbox data.

### Sprint 3 — Finance Transactions and Costs
- Ingest order/payment/refund events idempotently.
- Cost records and allocation.
- Receivables/payables.
- Deliver: transaction and cost APIs; reconciliation of duplicate event tests.

### Sprint 4 — Ledger and Cash
- Chart of accounts configurable per legal entity.
- Journal entry draft/post/reversal.
- Cash/bank account and transaction.
- Period close/reopen permissions.
- Deliver: balanced-entry validation and immutable posted entries.

### Sprint 5 — E-Invoice Core
- Invoice aggregate, item snapshots and state machine.
- Issuance rules with effective dates.
- Provider interface and fake provider.
- Issuance job/idempotency.
- Deliver: complete sandbox flow using fake provider.

### Sprint 6 — First E-Invoice Provider
- Select provider after contract and API review.
- Implement auth, issue, status query, documents and webhook if supported.
- Verify signature and replay protection.
- Implement UNKNOWN_RESULT recovery.
- Deliver: provider UAT evidence and duplicate-issuance tests.

### Sprint 7 — Reconciliation Core
- Upload original source file to object storage.
- Parser framework and normalized external transaction.
- Batch lifecycle and matching engine.
- Exception queue and manual resolution.
- Deliver: import replay and matching tests.

### Sprint 8 — Marketplace and COD Reconciliation
- Implement Shopee/TikTok settlement adapters only for authorized APIs/files.
- Map fee/refund/shipping/subsidy transaction types.
- COD carrier settlement import.
- Deliver: sample settlement fixtures and variance reports.

### Sprint 9 — Bank and Offline Reconciliation
- Bank statement import/API adapter.
- POS shift close, expected vs counted cash.
- Match bank transfer/QR/card/cash.
- Deliver: store-level cash close and bank matching.

### Sprint 10 — Reporting Facts
- Define metric dictionary and fact grain.
- Consume events into fact tables.
- Rebuild/backfill jobs.
- Deliver: sales, payment, invoice and cost facts.

### Sprint 11 — Dashboards and Exports
- Executive dashboard.
- Sales by channel/product.
- P&L management report and cashflow.
- Invoice and reconciliation reports.
- Async CSV/XLSX/PDF export and access control.
- Deliver: verified metrics against fixture dataset.

### Sprint 12 — UAT, Security and Production Readiness
- RBAC/tenant isolation tests.
- Load and retry tests.
- Backup/restore drill.
- Monitoring, alerts, DLQ replay runbook.
- Accountant sign-off on invoice and finance policies.
- Deliver: go-live checklist and rollback plan.

## Definition of Done

A feature is done only when:
1. Flyway migration is included and rollback/recovery approach documented.
2. API contract and validation errors are documented.
3. Unit tests and integration tests pass.
4. Idempotency and duplicate delivery are tested where relevant.
5. Audit trail exists for high-risk changes.
6. Logs contain correlation IDs but no secrets/PII beyond policy.
7. Permission checks are tested.
8. Metrics and alerts are defined for asynchronous jobs.
9. User-facing error and retry paths are implemented.
10. UAT evidence is attached for external provider/channel integrations.

## UAT scenario checklist

- Duplicate order/payment/refund event.
- Partial payment and multiple refunds.
- Marketplace settlement with fee and subsidy lines.
- Settlement amount mismatch.
- Duplicate import file.
- One settlement row matching multiple internal records.
- Provider timeout after accepting issuance.
- Provider rejection and corrected retry.
- Issued invoice followed by return/refund.
- Period close then attempted edit.
- Cross-store/cross-entity report access.
- Export URL expiry and permission revocation.

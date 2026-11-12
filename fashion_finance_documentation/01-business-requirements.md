# 01. Business Requirements

## 1. Mục tiêu

Xây dựng lớp nghiệp vụ tài chính và kiểm soát đa kênh cho nền tảng thời trang, bao gồm:
- Finance: giao dịch, chi phí, giá vốn, công nợ, dòng tiền, bút toán.
- E-Invoice: quản lý cấu hình, tạo nháp, phát hành tự động qua nhà cung cấp, tra cứu, lưu chứng từ, điều chỉnh/thay thế.
- Reconciliation: nhập dữ liệu ngoài, matching, xử lý chênh lệch, phê duyệt kỳ.
- Reporting: dashboard và báo cáo quản trị từ fact/aggregate tables.

## 2. Kênh và nguồn dữ liệu

| Channel | Source system | Typical data |
|---|---|---|
| WEBSITE | Commerce Core | Orders, payments, refunds |
| SHOPEE | Marketplace connector / settlement import | Orders, fees, settlement, refunds |
| TIKTOK | Marketplace connector / settlement import | Orders, fees, settlement, refunds |
| OFFLINE | POS | Sales, cash, QR/bank, card, shift close |
| COD | Carrier integration / carrier settlement | COD collected and remitted |
| BANK | Bank statement/import/API | Cash received, fees, transfers |
| E-INVOICE PROVIDER | Provider API/webhook | Issuance result, invoice number, documents |

External channel data is evidence and must be traceable to original external IDs and source files.

## 3. Domain boundaries

### Commerce Core owns
- Order, order item, customer, product, SKU, discounts, payment intent, refund, return, shipment.
- Canonical order totals and order lifecycle.

### Finance owns
- Financial transactions derived from commerce and external settlement.
- Cost records and allocations.
- Receivables/payables.
- Cash transactions.
- Journal entries and accounting periods.

### E-Invoice owns
- Seller legal entity snapshot and buyer snapshot on invoice.
- Invoice draft, issuance job, provider request/response, official identifiers, status and documents.
- Adjustment/replacement relationships.

### Reconciliation owns
- Reconciliation source, batch, raw external transactions, internal candidates, matches, exceptions, sign-off.

### Reporting owns
- Facts, dimensions, aggregate snapshots and export jobs.
- It must not become the source of truth for transactional data.

## 4. Finance business rules

1. Order amount is not automatically recognized revenue.
2. Customer payment, platform receivable, COD receivable and bank cash received are distinct.
3. Marketplace settlement is decomposed into sales proceeds, seller-funded discount, platform-funded discount (if provided), fees, shipping deductions, refunds, adjustments and net payout.
4. Finance transaction ingestion must be idempotent.
5. Posted journal entries are immutable; corrections use reversal and replacement entries.
6. Journal entry debits must equal credits in the same currency/base-currency policy.
7. Costs must support allocation to order, SKU, channel, campaign or store.
8. COGS calculation method is configurable and requires accounting approval.
9. Period close blocks normal edits; reopening requires elevated permission and audit record.
10. Do not infer accounting treatment from marketplace settlement amounts.

## 5. E-Invoice business rules

1. Automatic issuance is rule-driven, not a blind trigger at order creation.
2. Rules are scoped by legal entity, channel, transaction type, event, effective date and conditions.
3. A draft captures immutable snapshots of seller, buyer, item, discount, tax and total values.
4. Issuance uses an idempotency key stable across retries.
5. On timeout/unknown provider response, query provider status before issuing again.
6. Store original provider XML and related documents in object storage; keep metadata and content hash in PostgreSQL.
7. An issued invoice is never physically deleted through normal application flows.
8. Returns/cancellations after issuance follow legally approved adjustment/replacement workflows.
9. Provider capabilities differ. Unsupported operations must be disabled, not simulated.
10. Invoice issuance timing, invoice type, tax rate and buyer mandatory fields require accounting/tax approval for the actual legal entity.

### Invoice status model

`DRAFT -> VALIDATING -> PENDING_ISSUANCE -> SUBMITTED -> ISSUED`

Exceptional states:
- `REJECTED`
- `UNKNOWN_RESULT`
- `ADJUSTMENT_PENDING`, `ADJUSTED`
- `REPLACEMENT_PENDING`, `REPLACED`
- `CANCELLED` (only where provider and applicable rules support the operation)

`UNKNOWN_RESULT` is not a retry permission. It requires provider status inquiry first.

## 6. Reconciliation business rules

### Batch lifecycle

`CREATED -> IMPORTED -> NORMALIZED -> MATCHING -> REVIEW_REQUIRED -> APPROVED`

Alternative terminal/exception states:
- `FAILED`
- `REJECTED`
- `CLOSED`

### Match statuses

- `MATCHED`
- `PARTIAL_MATCH`
- `UNMATCHED`
- `DUPLICATE`
- `AMOUNT_MISMATCH`
- `MANUAL_MATCH`

### Matching priority

1. Exact external transaction ID.
2. Exact settlement ID + order ID + transaction type.
3. Order ID + transaction type + currency + amount.
4. Grouped/partial matching using approved rules.
5. Manual match with reason and actor.

Never match solely by equal amount. Preserve source row and matching evidence.

## 7. Reporting definitions

Every metric must have an approved definition, grain, inclusion/exclusion rules and timezone.

| Metric | Initial definition |
|---|---|
| Order count | Count distinct canonical order IDs in selected status scope |
| Gross sales | Sum eligible item gross amounts before seller-funded discounts; exclude cancelled orders |
| Seller discount | Discount funded by seller |
| Platform subsidy | Discount funded by marketplace, separately tracked |
| Net sales | Gross sales less seller discounts and approved returns/allowances; tax treatment configurable |
| Cash received | Settled cash transaction amount, not order amount |
| Platform receivable | Confirmed amount due from marketplace but not yet received |
| COD receivable | COD due from carrier but not yet remitted |
| Gross profit | Net sales less COGS |
| Contribution profit | Gross profit less configured variable costs |
| Invoice issued value | Sum of issued invoice totals; not a substitute for recognized revenue |

Do not join facts with incompatible grains before aggregation. Example: one order can have multiple payments, refunds and settlement rows.

## 8. Offline POS requirements

- POS order references store, terminal, cashier and shift.
- Payment methods: cash, bank transfer/QR, card/POS terminal, other configured tender.
- Shift close records expected cash, counted cash, variance and cashier approval.
- Refund and exchange must reference original order/receipt.
- Offline sales feed the same canonical order/finance/invoice pipeline as online sales.

## 9. Security and audit

- RBAC by legal entity, store and channel.
- Maker-checker for invoice issuance settings, journal posting, adjustment and period close.
- Encrypt provider credentials; never log secrets.
- Verify provider webhook signatures and timestamps; protect against replay.
- Export jobs enforce row-level access and expire download links.
- Keep audit history for changes to rules, tax config, amounts, status and approvals.
- Retention policy must be confirmed with legal/accounting advisers.

## 10. Production go-live blockers

- Legal entity and seller information approved.
- Tax/accounting treatment and invoice timing approved.
- Provider contract, current API docs, credentials and UAT passed.
- Marketplace seller API permissions/settlement format confirmed.
- Bank/payment/carrier data source confirmed.
- Costing method approved.
- Backup/restore and operational alerting tested.

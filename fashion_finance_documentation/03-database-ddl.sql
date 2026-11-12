-- PostgreSQL starter schema. Review and split into Flyway migrations before production.
-- Requires PostgreSQL 14+.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE legal_entities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_code VARCHAR(50) NOT NULL UNIQUE,
    entity_name VARCHAR(255) NOT NULL,
    entity_type VARCHAR(40) NOT NULL DEFAULT 'UNDECIDED',
    tax_code VARCHAR(50),
    business_registration_no VARCHAR(100),
    address TEXT,
    country_code CHAR(2) NOT NULL DEFAULT 'VN',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID,
    CONSTRAINT ck_legal_entity_type CHECK
      (entity_type IN ('UNDECIDED','INDIVIDUAL','HOUSEHOLD_BUSINESS','COMPANY','OTHER')),
    CONSTRAINT ck_legal_entity_status CHECK (status IN ('DRAFT','ACTIVE','INACTIVE'))
);

CREATE TABLE tax_configurations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    tax_type VARCHAR(50) NOT NULL,
    tax_method VARCHAR(50),
    config_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    effective_from DATE NOT NULL,
    effective_to DATE,
    approved_by UUID,
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_tax_config_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);
CREATE INDEX ix_tax_config_entity_effective
    ON tax_configurations(legal_entity_id, effective_from, effective_to);

CREATE TABLE accounting_periods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    period_code VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    closed_at TIMESTAMPTZ,
    closed_by UUID,
    UNIQUE (legal_entity_id, period_code),
    CHECK (end_date >= start_date),
    CHECK (status IN ('OPEN','CLOSING','CLOSED','REOPENED'))
);

CREATE TABLE accounting_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    account_code VARCHAR(50) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    parent_id UUID REFERENCES accounting_accounts(id),
    account_type VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (legal_entity_id, account_code)
);

CREATE TABLE finance_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    transaction_code VARCHAR(60) NOT NULL,
    transaction_type VARCHAR(40) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_id VARCHAR(150) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'VND',
    occurred_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    UNIQUE (legal_entity_id, source_type, source_id, transaction_type),
    UNIQUE (legal_entity_id, transaction_code),
    CHECK (amount >= 0),
    CHECK (channel IN ('WEBSITE','SHOPEE','TIKTOK','OFFLINE','OTHER')),
    CHECK (status IN ('PENDING','CONFIRMED','POSTED','REVERSED','CANCELLED'))
);
CREATE INDEX ix_finance_tx_occurred ON finance_transactions(legal_entity_id, occurred_at);
CREATE INDEX ix_finance_tx_channel ON finance_transactions(legal_entity_id, channel, occurred_at);

CREATE TABLE finance_transaction_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES finance_transactions(id),
    line_type VARCHAR(50) NOT NULL,
    reference_type VARCHAR(50),
    reference_id VARCHAR(150),
    amount NUMERIC(19,4) NOT NULL,
    description TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CHECK (amount >= 0)
);

CREATE TABLE cost_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    cost_code VARCHAR(60) NOT NULL,
    cost_type VARCHAR(50) NOT NULL,
    source_type VARCHAR(50),
    source_id VARCHAR(150),
    order_id UUID,
    product_id UUID,
    amount NUMERIC(19,4) NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'VND',
    occurred_at TIMESTAMPTZ NOT NULL,
    description TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (legal_entity_id, cost_code),
    CHECK (amount >= 0)
);
CREATE INDEX ix_cost_order ON cost_records(legal_entity_id, order_id);
CREATE INDEX ix_cost_occurred ON cost_records(legal_entity_id, occurred_at);

CREATE TABLE cost_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cost_record_id UUID NOT NULL REFERENCES cost_records(id),
    target_type VARCHAR(30) NOT NULL,
    target_id UUID NOT NULL,
    allocated_amount NUMERIC(19,4) NOT NULL,
    allocation_method VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (allocated_amount >= 0)
);

CREATE TABLE receivables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    counterparty_type VARCHAR(40) NOT NULL,
    counterparty_id VARCHAR(150) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_id VARCHAR(150) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    paid_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency_code CHAR(3) NOT NULL DEFAULT 'VND',
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (amount >= 0 AND paid_amount >= 0 AND paid_amount <= amount),
    CHECK (status IN ('OPEN','PARTIALLY_PAID','PAID','OVERDUE','WRITTEN_OFF'))
);
CREATE INDEX ix_receivables_status_due ON receivables(legal_entity_id, status, due_date);

CREATE TABLE payables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    counterparty_type VARCHAR(40) NOT NULL,
    counterparty_id VARCHAR(150) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_id VARCHAR(150) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    paid_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency_code CHAR(3) NOT NULL DEFAULT 'VND',
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (amount >= 0 AND paid_amount >= 0 AND paid_amount <= amount),
    CHECK (status IN ('OPEN','PARTIALLY_PAID','PAID','OVERDUE','DISPUTED'))
);

CREATE TABLE journal_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    period_id UUID NOT NULL REFERENCES accounting_periods(id),
    entry_no VARCHAR(60) NOT NULL,
    source_type VARCHAR(50),
    source_id VARCHAR(150),
    entry_date DATE NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    reversal_of UUID REFERENCES journal_entries(id),
    posted_at TIMESTAMPTZ,
    posted_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (legal_entity_id, entry_no),
    CHECK (status IN ('DRAFT','POSTED','REVERSED'))
);

CREATE TABLE journal_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id),
    account_id UUID NOT NULL REFERENCES accounting_accounts(id),
    debit_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    credit_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency_code CHAR(3) NOT NULL DEFAULT 'VND',
    description TEXT,
    CHECK (debit_amount >= 0 AND credit_amount >= 0),
    CHECK (NOT (debit_amount > 0 AND credit_amount > 0))
);
CREATE INDEX ix_journal_lines_entry ON journal_lines(journal_entry_id);

CREATE TABLE cash_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    account_name VARCHAR(150) NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    bank_name VARCHAR(150),
    account_number_masked VARCHAR(100),
    currency_code CHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE cash_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    cash_account_id UUID NOT NULL REFERENCES cash_accounts(id),
    transaction_type VARCHAR(30) NOT NULL,
    source_type VARCHAR(50),
    source_id VARCHAR(150),
    amount NUMERIC(19,4) NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'VND',
    occurred_at TIMESTAMPTZ NOT NULL,
    external_reference VARCHAR(150),
    description TEXT,
    CHECK (amount >= 0)
);
CREATE INDEX ix_cash_tx_occurred ON cash_transactions(legal_entity_id, occurred_at);

CREATE TABLE invoice_providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_code VARCHAR(40) NOT NULL UNIQUE,
    provider_name VARCHAR(150) NOT NULL,
    capabilities_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE'
);

CREATE TABLE invoice_provider_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES invoice_providers(id),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    environment VARCHAR(10) NOT NULL,
    credentials_encrypted BYTEA,
    config_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    UNIQUE (provider_id, legal_entity_id, environment),
    CHECK (environment IN ('SANDBOX','PRODUCTION'))
);

CREATE TABLE invoice_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    provider_id UUID NOT NULL REFERENCES invoice_providers(id),
    template_code VARCHAR(80) NOT NULL,
    serial VARCHAR(40),
    invoice_type VARCHAR(40),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    UNIQUE (legal_entity_id, provider_id, template_code, serial)
);

CREATE TABLE invoice_issuance_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    rule_name VARCHAR(150) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    conditions_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    provider_id UUID REFERENCES invoice_providers(id),
    template_id UUID REFERENCES invoice_templates(id),
    priority INTEGER NOT NULL DEFAULT 100,
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by UUID,
    approved_at TIMESTAMPTZ,
    CHECK (channel IN ('WEBSITE','SHOPEE','TIKTOK','OFFLINE','OTHER'))
);
CREATE INDEX ix_invoice_rules_lookup
    ON invoice_issuance_rules(legal_entity_id, channel, event_type, enabled, priority);

CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    invoice_code VARCHAR(60) NOT NULL,
    order_id UUID,
    channel VARCHAR(30) NOT NULL,
    provider_id UUID NOT NULL REFERENCES invoice_providers(id),
    provider_invoice_id VARCHAR(150),
    invoice_number VARCHAR(80),
    serial VARCHAR(40),
    template_code VARCHAR(80),
    tax_authority_code VARCHAR(150),
    seller_snapshot JSONB NOT NULL,
    buyer_snapshot JSONB NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'VND',
    subtotal NUMERIC(19,4) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    idempotency_key VARCHAR(150) NOT NULL,
    original_invoice_id UUID REFERENCES invoices(id),
    issued_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (legal_entity_id, invoice_code),
    UNIQUE (idempotency_key),
    CHECK (status IN ('DRAFT','VALIDATING','PENDING_ISSUANCE','SUBMITTED','ISSUED',
      'REJECTED','UNKNOWN_RESULT','ADJUSTMENT_PENDING','ADJUSTED',
      'REPLACEMENT_PENDING','REPLACED','CANCELLED'))
);
CREATE UNIQUE INDEX ux_invoice_provider_external
    ON invoices(provider_id, provider_invoice_id)
    WHERE provider_invoice_id IS NOT NULL;
CREATE INDEX ix_invoice_order ON invoices(legal_entity_id, order_id);
CREATE INDEX ix_invoice_status_issued ON invoices(legal_entity_id, status, issued_at);

CREATE TABLE invoice_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    product_id UUID,
    sku VARCHAR(100),
    item_name VARCHAR(500) NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    discount_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_rate NUMERIC(9,4),
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    line_total NUMERIC(19,4) NOT NULL,
    CHECK (quantity > 0)
);

CREATE TABLE invoice_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    document_type VARCHAR(30) NOT NULL,
    storage_key TEXT NOT NULL,
    content_hash VARCHAR(128) NOT NULL,
    file_size BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE invoice_issuance_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    idempotency_key VARCHAR(150) NOT NULL UNIQUE,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    last_error_code VARCHAR(100),
    last_error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE invoice_provider_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID REFERENCES invoices(id),
    provider_id UUID NOT NULL REFERENCES invoice_providers(id),
    request_id VARCHAR(150),
    request_payload_redacted JSONB,
    response_payload_redacted JSONB,
    http_status INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE invoice_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    action VARCHAR(80) NOT NULL,
    old_status VARCHAR(30),
    new_status VARCHAR(30),
    actor_id UUID,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE reconciliation_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_code VARCHAR(50) NOT NULL UNIQUE,
    source_name VARCHAR(150) NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    channel VARCHAR(30),
    config_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE reconciliation_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    source_id UUID NOT NULL REFERENCES reconciliation_sources(id),
    batch_code VARCHAR(60) NOT NULL,
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    file_storage_key TEXT,
    file_hash VARCHAR(128),
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    total_rows INTEGER NOT NULL DEFAULT 0,
    imported_by UUID,
    approved_by UUID,
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (legal_entity_id, batch_code),
    CHECK (period_end >= period_start)
);
CREATE INDEX ix_recon_batch_source_period
    ON reconciliation_batches(source_id, period_start, period_end, status);

CREATE TABLE reconciliation_external_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES reconciliation_batches(id),
    external_transaction_id VARCHAR(180),
    external_order_id VARCHAR(180),
    settlement_id VARCHAR(180),
    transaction_type VARCHAR(50) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    fee_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency_code CHAR(3) NOT NULL DEFAULT 'VND',
    occurred_at TIMESTAMPTZ,
    raw_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    row_fingerprint VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_recon_external_order ON reconciliation_external_transactions(batch_id, external_order_id);
CREATE INDEX ix_recon_external_txid ON reconciliation_external_transactions(batch_id, external_transaction_id);

CREATE TABLE reconciliation_internal_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES reconciliation_batches(id),
    internal_transaction_id UUID,
    order_id UUID,
    transaction_type VARCHAR(50) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'VND',
    occurred_at TIMESTAMPTZ,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE reconciliation_matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES reconciliation_batches(id),
    external_transaction_id UUID NOT NULL REFERENCES reconciliation_external_transactions(id),
    internal_transaction_id UUID NOT NULL REFERENCES reconciliation_internal_transactions(id),
    match_method VARCHAR(40) NOT NULL,
    matched_amount NUMERIC(19,4) NOT NULL,
    confidence NUMERIC(5,2),
    status VARCHAR(30) NOT NULL DEFAULT 'MATCHED',
    matched_by UUID,
    matched_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (matched_amount >= 0),
    CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 100))
);
CREATE INDEX ix_recon_matches_batch_status ON reconciliation_matches(batch_id, status);

CREATE TABLE reconciliation_exceptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES reconciliation_batches(id),
    exception_type VARCHAR(40) NOT NULL,
    external_transaction_id UUID REFERENCES reconciliation_external_transactions(id),
    internal_transaction_id UUID REFERENCES reconciliation_internal_transactions(id),
    difference_amount NUMERIC(19,4),
    reason TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    assigned_to UUID,
    resolved_by UUID,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE reconciliation_adjustments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exception_id UUID NOT NULL REFERENCES reconciliation_exceptions(id),
    adjustment_type VARCHAR(40) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by UUID,
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE reconciliation_signoffs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES reconciliation_batches(id),
    approved_by UUID NOT NULL,
    approved_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    note TEXT,
    UNIQUE (batch_id)
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(150) NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    schema_version INTEGER NOT NULL DEFAULT 1,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT
);
CREATE INDEX ix_outbox_unpublished ON outbox_events(occurred_at) WHERE published_at IS NULL;

CREATE TABLE processed_events (
    consumer_name VARCHAR(150) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer_name, event_id)
);

-- Reporting starter facts: adapt keys to the canonical Commerce schema.
CREATE TABLE fact_sales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    event_date DATE NOT NULL,
    order_id UUID NOT NULL,
    order_item_id UUID,
    product_id UUID,
    sku VARCHAR(100),
    channel VARCHAR(30) NOT NULL,
    store_id UUID,
    quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    gross_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    seller_discount_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    platform_subsidy_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    return_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    net_sales_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency_code CHAR(3) NOT NULL DEFAULT 'VND',
    source_event_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (legal_entity_id, source_event_id)
);
CREATE INDEX ix_fact_sales_date_channel ON fact_sales(legal_entity_id, event_date, channel);
CREATE INDEX ix_fact_sales_product_date ON fact_sales(legal_entity_id, product_id, event_date);

CREATE TABLE fact_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    transaction_date DATE NOT NULL,
    order_id UUID,
    payment_transaction_id UUID NOT NULL,
    channel VARCHAR(30) NOT NULL,
    payment_method VARCHAR(40),
    transaction_type VARCHAR(30) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'VND',
    source_event_id UUID NOT NULL,
    UNIQUE (legal_entity_id, source_event_id)
);
CREATE INDEX ix_fact_payments_date ON fact_payments(legal_entity_id, transaction_date, channel);

CREATE TABLE fact_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    invoice_id UUID NOT NULL UNIQUE,
    issue_date DATE,
    channel VARCHAR(30),
    status VARCHAR(30) NOT NULL,
    subtotal NUMERIC(19,4) NOT NULL,
    tax_amount NUMERIC(19,4) NOT NULL,
    total_amount NUMERIC(19,4) NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'VND'
);
CREATE INDEX ix_fact_invoice_date_status ON fact_invoices(legal_entity_id, issue_date, status);

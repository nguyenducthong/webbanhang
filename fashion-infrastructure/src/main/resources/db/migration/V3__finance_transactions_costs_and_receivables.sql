-- V3: Finance Transactions, Costs, Allocations and Receivables/Payables

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
    transaction_id UUID NOT NULL REFERENCES finance_transactions(id) ON DELETE CASCADE,
    line_type VARCHAR(50) NOT NULL,
    reference_type VARCHAR(50),
    reference_id VARCHAR(150),
    amount NUMERIC(19,4) NOT NULL,
    description TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CHECK (amount >= 0)
);
CREATE INDEX ix_tx_lines_tx_id ON finance_transaction_lines(transaction_id);

CREATE TABLE costs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    cost_code VARCHAR(60) NOT NULL,
    cost_category VARCHAR(50) NOT NULL,
    cost_type VARCHAR(50) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'VND',
    incurred_at TIMESTAMPTZ NOT NULL,
    vendor_id UUID,
    reference_no VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'RECORDED',
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    UNIQUE (legal_entity_id, cost_code),
    CHECK (amount >= 0),
    CHECK (status IN ('RECORDED','ALLOCATED','VOID'))
);
CREATE INDEX ix_costs_entity_incurred ON costs(legal_entity_id, incurred_at);

CREATE TABLE cost_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cost_id UUID NOT NULL REFERENCES costs(id) ON DELETE CASCADE,
    allocation_target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(150) NOT NULL,
    allocated_amount NUMERIC(19,4) NOT NULL,
    allocation_ratio NUMERIC(9,6),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (allocated_amount >= 0),
    CHECK (allocation_target_type IN ('ORDER','SKU','PRODUCT','CHANNEL','CAMPAIGN','STORE','OTHER'))
);
CREATE INDEX ix_cost_allocations_cost_id ON cost_allocations(cost_id);
CREATE INDEX ix_cost_allocations_target ON cost_allocations(allocation_target_type, target_id);

CREATE TABLE receivables_payables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    entry_type VARCHAR(20) NOT NULL,
    party_type VARCHAR(40) NOT NULL,
    party_id VARCHAR(150) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_id VARCHAR(150) NOT NULL,
    original_amount NUMERIC(19,4) NOT NULL,
    settled_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency_code CHAR(3) NOT NULL DEFAULT 'VND',
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (original_amount >= 0),
    CHECK (settled_amount >= 0),
    CHECK (settled_amount <= original_amount),
    CHECK (entry_type IN ('RECEIVABLE','PAYABLE')),
    CHECK (status IN ('OPEN','PARTIALLY_SETTLED','SETTLED','WRITTEN_OFF'))
);
CREATE INDEX ix_rp_entity_status ON receivables_payables(legal_entity_id, status);
CREATE INDEX ix_rp_party ON receivables_payables(party_type, party_id);


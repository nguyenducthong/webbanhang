-- V4: Accounting Periods, Chart of Accounts, Journal Entries & Cash Management

CREATE TABLE accounting_periods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    period_code VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    closed_at TIMESTAMPTZ,
    closed_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (legal_entity_id, period_code),
    CHECK (end_date >= start_date),
    CHECK (status IN ('OPEN','CLOSING','CLOSED','REOPENED'))
);
CREATE INDEX ix_period_entity_code ON accounting_periods(legal_entity_id, period_code);

CREATE TABLE accounting_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    account_code VARCHAR(50) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    parent_id UUID REFERENCES accounting_accounts(id),
    account_type VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (legal_entity_id, account_code),
    CHECK (account_type IN ('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE'))
);
CREATE INDEX ix_acc_entity_code ON accounting_accounts(legal_entity_id, account_code);

CREATE TABLE journal_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    period_id UUID NOT NULL REFERENCES accounting_periods(id),
    entry_no VARCHAR(60) NOT NULL,
    source_type VARCHAR(50),
    source_id VARCHAR(150),
    posting_date DATE NOT NULL,
    entry_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    reversed_by_id UUID REFERENCES journal_entries(id),
    reverses_id UUID REFERENCES journal_entries(id),
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    posted_at TIMESTAMPTZ,
    posted_by UUID,
    UNIQUE (legal_entity_id, entry_no),
    CHECK (entry_status IN ('DRAFT','POSTED','REVERSED'))
);
CREATE INDEX ix_je_entity_period ON journal_entries(legal_entity_id, period_id);
CREATE INDEX ix_je_posting_date ON journal_entries(legal_entity_id, posting_date);

CREATE TABLE journal_entry_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounting_accounts(id),
    debit_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    credit_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency_code CHAR(3) NOT NULL DEFAULT 'VND',
    description TEXT,
    CHECK (debit_amount >= 0),
    CHECK (credit_amount >= 0),
    CHECK (debit_amount > 0 OR credit_amount > 0)
);
CREATE INDEX ix_je_lines_entry_id ON journal_entry_lines(journal_entry_id);

CREATE TABLE cash_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    account_name VARCHAR(150) NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    bank_name VARCHAR(150),
    account_number_masked VARCHAR(100),
    currency_code CHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (account_type IN ('CASH_DRAWER','BANK_ACCOUNT','EWALLET')),
    CHECK (status IN ('ACTIVE','INACTIVE','CLOSED'))
);

CREATE TABLE cash_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    cash_account_id UUID NOT NULL REFERENCES cash_accounts(id),
    transaction_type VARCHAR(30) NOT NULL,
    source_type VARCHAR(50),
    source_id VARCHAR(150),
    amount NUMERIC(19,4) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    description TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    CHECK (amount >= 0),
    CHECK (transaction_type IN ('INFLOW','OUTFLOW','TRANSFER')),
    CHECK (status IN ('PENDING','CONFIRMED','REVERSED'))
);
CREATE INDEX ix_cash_tx_account ON cash_transactions(cash_account_id, occurred_at);

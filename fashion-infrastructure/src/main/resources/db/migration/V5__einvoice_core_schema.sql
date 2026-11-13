-- V5__einvoice_core_schema.sql
-- Electronic Invoicing System Schema (Decree 123/2020/ND-CP & Circular 78/2021/TT-BTC)

CREATE TABLE IF NOT EXISTS invoice_providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS invoice_provider_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    provider_id UUID NOT NULL REFERENCES invoice_providers(id),
    account_username VARCHAR(100) NOT NULL,
    account_password_encrypted VARCHAR(255),
    api_endpoint VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_provider_account_entity UNIQUE (legal_entity_id, provider_id)
);

CREATE TABLE IF NOT EXISTS invoice_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    template_code VARCHAR(50) NOT NULL,
    invoice_series VARCHAR(20) NOT NULL,
    invoice_type VARCHAR(50) NOT NULL DEFAULT 'VAT_INVOICE',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_template_series_entity UNIQUE (legal_entity_id, template_code, invoice_series)
);

CREATE TABLE IF NOT EXISTS invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    provider_account_id UUID REFERENCES invoice_provider_accounts(id),
    template_id UUID REFERENCES invoice_templates(id),
    finance_transaction_id UUID REFERENCES finance_transactions(id),
    invoice_type VARCHAR(50) NOT NULL DEFAULT 'VAT_INVOICE',
    invoice_status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    invoice_number VARCHAR(50),
    invoice_series VARCHAR(20),
    template_code VARCHAR(50),
    issue_date TIMESTAMP WITH TIME ZONE,
    tax_authority_code VARCHAR(100),
    seller_snapshot TEXT NOT NULL,
    buyer_snapshot TEXT NOT NULL,
    total_pretax_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_discount_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_tax_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    notes VARCHAR(500),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_invoices_legal_entity ON invoices(legal_entity_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(invoice_status);
CREATE INDEX IF NOT EXISTS idx_invoices_issue_date ON invoices(issue_date);
CREATE INDEX IF NOT EXISTS idx_invoices_tx_id ON invoices(finance_transaction_id);

CREATE TABLE IF NOT EXISTS invoice_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    line_number INT NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    unit VARCHAR(50),
    quantity NUMERIC(19, 4) NOT NULL DEFAULT 1,
    unit_price NUMERIC(19, 4) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    tax_rate NUMERIC(7, 4) NOT NULL DEFAULT 0,
    pretax_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_invoice_item_line UNIQUE (invoice_id, line_number)
);

CREATE TABLE IF NOT EXISTS invoice_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    s3_bucket VARCHAR(100) NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    file_hash VARCHAR(100),
    file_size BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_invoice_document_type UNIQUE (invoice_id, document_type)
);

CREATE TABLE IF NOT EXISTS invoice_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    reason TEXT,
    performed_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_invoice_audit_invoice ON invoice_audit_logs(invoice_id);

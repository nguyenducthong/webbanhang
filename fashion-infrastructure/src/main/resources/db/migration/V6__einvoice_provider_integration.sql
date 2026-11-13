-- V6__einvoice_provider_integration.sql
-- Production Provider Integration, Webhook Logs and Duplicate Protection

ALTER TABLE invoice_provider_accounts
    ADD COLUMN IF NOT EXISTS auth_token_cached TEXT,
    ADD COLUMN IF NOT EXISTS token_expiry TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS webhook_secret VARCHAR(255) DEFAULT 'sec_webhook_default_key_2026',
    ADD COLUMN IF NOT EXISTS certificate_serial VARCHAR(100);

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS invoice_webhook_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_code VARCHAR(50) NOT NULL,
    invoice_id UUID REFERENCES invoices(id) ON DELETE SET NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    signature VARCHAR(255),
    processed_status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_invoice_webhook_provider ON invoice_webhook_logs(provider_code);
CREATE INDEX IF NOT EXISTS idx_invoice_webhook_invoice ON invoice_webhook_logs(invoice_id);

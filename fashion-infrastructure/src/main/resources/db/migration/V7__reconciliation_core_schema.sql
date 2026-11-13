-- V7__reconciliation_core_schema.sql
-- Financial Reconciliation Core & Matching Engine Schema

CREATE TABLE IF NOT EXISTS reconciliation_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_code VARCHAR(50) NOT NULL UNIQUE,
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    source_type VARCHAR(50) NOT NULL DEFAULT 'SHOPEE',
    file_name VARCHAR(255) NOT NULL,
    s3_key VARCHAR(500),
    file_hash VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    total_records INT NOT NULL DEFAULT 0,
    matched_records INT NOT NULL DEFAULT 0,
    discrepancy_records INT NOT NULL DEFAULT 0,
    total_amount_external NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_amount_internal NUMERIC(19, 4) NOT NULL DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_reconciliation_batch_entity ON reconciliation_batches(legal_entity_id);
CREATE INDEX IF NOT EXISTS idx_reconciliation_batch_status ON reconciliation_batches(status);

CREATE TABLE IF NOT EXISTS reconciliation_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES reconciliation_batches(id) ON DELETE CASCADE,
    external_order_id VARCHAR(100) NOT NULL,
    internal_order_id VARCHAR(100),
    external_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    internal_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    discrepancy_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    fee_external NUMERIC(19, 4) NOT NULL DEFAULT 0,
    fee_internal NUMERIC(19, 4) NOT NULL DEFAULT 0,
    match_status VARCHAR(50) NOT NULL DEFAULT 'UNMATCHED_EXTERNAL',
    resolution_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    resolution_notes TEXT,
    resolved_by VARCHAR(100),
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reconciliation_item_batch ON reconciliation_items(batch_id);
CREATE INDEX IF NOT EXISTS idx_reconciliation_item_ext_order ON reconciliation_items(external_order_id);
CREATE INDEX IF NOT EXISTS idx_reconciliation_item_match ON reconciliation_items(match_status);
CREATE INDEX IF NOT EXISTS idx_reconciliation_item_res ON reconciliation_items(resolution_status);

-- ============================================================================
-- Flyway Migration V9: Bank & Offline POS Reconciliation Schema
-- ============================================================================

CREATE TABLE IF NOT EXISTS pos_shifts (
    id UUID PRIMARY KEY,
    legal_entity_id UUID NOT NULL REFERENCES legal_entities(id),
    store_code VARCHAR(50) NOT NULL,
    shift_code VARCHAR(50) NOT NULL UNIQUE,
    cashier_username VARCHAR(100) NOT NULL,
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    opening_float NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    total_cash_sales NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    total_card_sales NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    total_qr_sales NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    expected_cash_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    actual_counted_cash NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    difference_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    closing_notes TEXT,
    manager_approved_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pos_shift_entity ON pos_shifts(legal_entity_id);
CREATE INDEX IF NOT EXISTS idx_pos_shift_store ON pos_shifts(store_code);
CREATE INDEX IF NOT EXISTS idx_pos_shift_status ON pos_shifts(status);

CREATE TABLE IF NOT EXISTS pos_shift_cash_counts (
    id UUID PRIMARY KEY,
    shift_id UUID NOT NULL REFERENCES pos_shifts(id) ON DELETE CASCADE,
    denomination INT NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    total_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000
);

CREATE INDEX IF NOT EXISTS idx_pos_cash_count_shift ON pos_shift_cash_counts(shift_id);

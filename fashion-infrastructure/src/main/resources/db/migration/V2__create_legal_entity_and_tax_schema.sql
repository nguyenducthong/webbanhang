-- V2: Legal Entity and Tax Configuration Schema

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
    created_by UUID,
    approved_by UUID,
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_tax_config_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE INDEX ix_tax_config_entity_effective
    ON tax_configurations(legal_entity_id, effective_from, effective_to);

-- Seed demo legal entity for testing / local development
INSERT INTO legal_entities (id, entity_code, entity_name, entity_type, tax_code, address, status)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'LE-DEMO-FASHION',
    'Fashion Demo Store VN',
    'COMPANY',
    '0101234567',
    '123 Le Loi, District 1, Ho Chi Minh City',
    'ACTIVE'
) ON CONFLICT (entity_code) DO NOTHING;


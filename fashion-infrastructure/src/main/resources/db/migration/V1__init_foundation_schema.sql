-- V1: Foundation Schema - Outbox, Processed Events, RBAC & Audit Logs
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Transactional Outbox Table
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
CREATE INDEX ix_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);

-- 2. Processed Events Table (for Idempotent Consumers)
CREATE TABLE processed_events (
    consumer_name VARCHAR(150) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer_name, event_id)
);

-- 3. System Audit Logs
CREATE TABLE system_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    legal_entity_id UUID,
    store_id UUID,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(150),
    detail_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip_address VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_entity_time ON system_audit_logs(legal_entity_id, created_at);
CREATE INDEX ix_audit_resource ON system_audit_logs(resource_type, resource_id);

-- 4. RBAC Foundation
CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    legal_entity_id UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE app_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE app_user_roles (
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES app_roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE app_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE app_role_permissions (
    role_id UUID NOT NULL REFERENCES app_roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES app_permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Seed initial roles
INSERT INTO app_roles (role_code, role_name, description) VALUES
('ROLE_SUPER_ADMIN', 'Super Administrator', 'Full platform access'),
('ROLE_ACCOUNTANT', 'Accountant', 'Manage accounting periods, general ledger, and reconciliation'),
('ROLE_FINANCE_MANAGER', 'Finance Manager', 'Approve tax configs, reconciliation batches, and period closing'),
('ROLE_CASHIER', 'Cashier', 'POS shifts and cash sales');


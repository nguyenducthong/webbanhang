package com.fashion.platform.einvoice.domain.model;

public enum InvoiceStatus {
    DRAFT,
    VALIDATING,
    PENDING_ISSUANCE,
    SUBMITTED,
    ISSUED,
    UNKNOWN_RESULT,
    REJECTED,
    CANCELLED
}

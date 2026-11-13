package com.fashion.platform.reconciliation.domain.model;

public enum BatchStatus {
    DRAFT,
    PARSING,
    MATCHING,
    COMPLETED,
    DISCREPANCY_FOUND
}

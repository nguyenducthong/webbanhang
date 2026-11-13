package com.fashion.platform.finance.domain.entity;

import java.math.BigDecimal;

public class UnbalancedJournalEntryException extends RuntimeException {
    public UnbalancedJournalEntryException(BigDecimal totalDebit, BigDecimal totalCredit) {
        super(String.format("Journal entry is unbalanced: Total Debit (%s) does not equal Total Credit (%s)",
                totalDebit, totalCredit));
    }
}

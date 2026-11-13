package com.fashion.platform.reconciliation.domain.parser;

import java.math.BigDecimal;

public record ParsedTransaction(
        String orderId,
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        BigDecimal netAmount
) {
}

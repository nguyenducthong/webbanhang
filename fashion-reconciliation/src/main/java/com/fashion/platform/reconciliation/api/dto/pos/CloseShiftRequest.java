package com.fashion.platform.reconciliation.api.dto.pos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloseShiftRequest {
    private BigDecimal totalCashSales;
    private BigDecimal totalCardSales;
    private BigDecimal totalQrSales;
    private List<CashDenominationItem> cashCounts;
    private String closingNotes;
    private String performedBy;
}

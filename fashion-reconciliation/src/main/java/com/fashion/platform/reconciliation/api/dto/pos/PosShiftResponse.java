package com.fashion.platform.reconciliation.api.dto.pos;

import com.fashion.platform.reconciliation.domain.model.PosShift;
import com.fashion.platform.reconciliation.domain.model.PosShiftStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosShiftResponse {
    private UUID id;
    private UUID legalEntityId;
    private String storeCode;
    private String shiftCode;
    private String cashierUsername;
    private Instant openedAt;
    private Instant closedAt;
    private PosShiftStatus status;
    private BigDecimal openingFloat;
    private BigDecimal totalCashSales;
    private BigDecimal totalCardSales;
    private BigDecimal totalQrSales;
    private BigDecimal expectedCashAmount;
    private BigDecimal actualCountedCash;
    private BigDecimal differenceAmount;
    private String closingNotes;
    private String managerApprovedBy;

    public static PosShiftResponse fromEntity(PosShift shift) {
        return PosShiftResponse.builder()
                .id(shift.getId())
                .legalEntityId(shift.getLegalEntity().getId())
                .storeCode(shift.getStoreCode())
                .shiftCode(shift.getShiftCode())
                .cashierUsername(shift.getCashierUsername())
                .openedAt(shift.getOpenedAt())
                .closedAt(shift.getClosedAt())
                .status(shift.getStatus())
                .openingFloat(shift.getOpeningFloat())
                .totalCashSales(shift.getTotalCashSales())
                .totalCardSales(shift.getTotalCardSales())
                .totalQrSales(shift.getTotalQrSales())
                .expectedCashAmount(shift.getExpectedCashAmount())
                .actualCountedCash(shift.getActualCountedCash())
                .differenceAmount(shift.getDifferenceAmount())
                .closingNotes(shift.getClosingNotes())
                .managerApprovedBy(shift.getManagerApprovedBy())
                .build();
    }
}

package com.fashion.platform.reconciliation.api.dto.pos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenShiftRequest {
    private UUID legalEntityId;
    private String storeCode;
    private String shiftCode;
    private String cashierUsername;
    private BigDecimal openingFloat;
}

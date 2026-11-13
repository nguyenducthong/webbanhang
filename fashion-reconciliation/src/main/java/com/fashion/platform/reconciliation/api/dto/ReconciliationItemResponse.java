package com.fashion.platform.reconciliation.api.dto;

import com.fashion.platform.reconciliation.domain.model.MatchStatus;
import com.fashion.platform.reconciliation.domain.model.ReconciliationItem;
import com.fashion.platform.reconciliation.domain.model.ResolutionStatus;
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
public class ReconciliationItemResponse {
    private UUID id;
    private UUID batchId;
    private String externalOrderId;
    private String internalOrderId;
    private BigDecimal externalAmount;
    private BigDecimal internalAmount;
    private BigDecimal discrepancyAmount;
    private BigDecimal feeExternal;
    private BigDecimal feeInternal;
    private MatchStatus matchStatus;
    private ResolutionStatus resolutionStatus;
    private String resolutionNotes;
    private String resolvedBy;
    private Instant resolvedAt;
    private Instant createdAt;

    public static ReconciliationItemResponse fromEntity(ReconciliationItem item) {
        return ReconciliationItemResponse.builder()
                .id(item.getId())
                .batchId(item.getBatch().getId())
                .externalOrderId(item.getExternalOrderId())
                .internalOrderId(item.getInternalOrderId())
                .externalAmount(item.getExternalAmount())
                .internalAmount(item.getInternalAmount())
                .discrepancyAmount(item.getDiscrepancyAmount())
                .feeExternal(item.getFeeExternal())
                .feeInternal(item.getFeeInternal())
                .matchStatus(item.getMatchStatus())
                .resolutionStatus(item.getResolutionStatus())
                .resolutionNotes(item.getResolutionNotes())
                .resolvedBy(item.getResolvedBy())
                .resolvedAt(item.getResolvedAt())
                .createdAt(item.getCreatedAt())
                .build();
    }
}

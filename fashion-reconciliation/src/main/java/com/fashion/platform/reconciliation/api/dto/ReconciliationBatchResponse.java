package com.fashion.platform.reconciliation.api.dto;

import com.fashion.platform.reconciliation.domain.model.BatchStatus;
import com.fashion.platform.reconciliation.domain.model.ReconciliationBatch;
import com.fashion.platform.reconciliation.domain.model.ReconciliationSourceType;
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
public class ReconciliationBatchResponse {
    private UUID id;
    private String batchCode;
    private UUID legalEntityId;
    private ReconciliationSourceType sourceType;
    private String fileName;
    private String s3Key;
    private String fileHash;
    private BatchStatus status;
    private int totalRecords;
    private int matchedRecords;
    private int discrepancyRecords;
    private BigDecimal totalAmountExternal;
    private BigDecimal totalAmountInternal;
    private String notes;
    private Instant createdAt;
    private String createdBy;

    public static ReconciliationBatchResponse fromEntity(ReconciliationBatch batch) {
        return ReconciliationBatchResponse.builder()
                .id(batch.getId())
                .batchCode(batch.getBatchCode())
                .legalEntityId(batch.getLegalEntity().getId())
                .sourceType(batch.getSourceType())
                .fileName(batch.getFileName())
                .s3Key(batch.getS3Key())
                .fileHash(batch.getFileHash())
                .status(batch.getStatus())
                .totalRecords(batch.getTotalRecords())
                .matchedRecords(batch.getMatchedRecords())
                .discrepancyRecords(batch.getDiscrepancyRecords())
                .totalAmountExternal(batch.getTotalAmountExternal())
                .totalAmountInternal(batch.getTotalAmountInternal())
                .notes(batch.getNotes())
                .createdAt(batch.getCreatedAt())
                .createdBy(batch.getCreatedBy())
                .build();
    }
}

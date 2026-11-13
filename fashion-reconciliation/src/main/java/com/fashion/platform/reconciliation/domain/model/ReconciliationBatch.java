package com.fashion.platform.reconciliation.domain.model;

import com.fashion.platform.finance.domain.entity.LegalEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_batches",
       indexes = {
           @Index(name = "idx_reconciliation_batch_entity", columnList = "legal_entity_id"),
           @Index(name = "idx_reconciliation_batch_status", columnList = "status")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "batch_code", nullable = false, unique = true, length = 50)
    private String batchCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    @Builder.Default
    private ReconciliationSourceType sourceType = ReconciliationSourceType.SHOPEE;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "s3_key", length = 500)
    private String s3Key;

    @Column(name = "file_hash", length = 100)
    private String fileHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private BatchStatus status = BatchStatus.DRAFT;

    @Column(name = "total_records", nullable = false)
    @Builder.Default
    private int totalRecords = 0;

    @Column(name = "matched_records", nullable = false)
    @Builder.Default
    private int matchedRecords = 0;

    @Column(name = "discrepancy_records", nullable = false)
    @Builder.Default
    private int discrepancyRecords = 0;

    @Column(name = "total_amount_external", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalAmountExternal = BigDecimal.ZERO;

    @Column(name = "total_amount_internal", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalAmountInternal = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReconciliationItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addItem(ReconciliationItem item) {
        item.setBatch(this);
        this.items.add(item);
    }
}

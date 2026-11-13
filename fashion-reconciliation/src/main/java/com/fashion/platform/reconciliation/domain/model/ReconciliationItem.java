package com.fashion.platform.reconciliation.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_items",
       indexes = {
           @Index(name = "idx_reconciliation_item_batch", columnList = "batch_id"),
           @Index(name = "idx_reconciliation_item_ext_order", columnList = "external_order_id"),
           @Index(name = "idx_reconciliation_item_match", columnList = "match_status"),
           @Index(name = "idx_reconciliation_item_res", columnList = "resolution_status")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private ReconciliationBatch batch;

    @Column(name = "external_order_id", nullable = false, length = 100)
    private String externalOrderId;

    @Column(name = "internal_order_id", length = 100)
    private String internalOrderId;

    @Column(name = "external_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal externalAmount = BigDecimal.ZERO;

    @Column(name = "internal_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal internalAmount = BigDecimal.ZERO;

    @Column(name = "discrepancy_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal discrepancyAmount = BigDecimal.ZERO;

    @Column(name = "fee_external", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal feeExternal = BigDecimal.ZERO;

    @Column(name = "fee_internal", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal feeInternal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 50)
    @Builder.Default
    private MatchStatus matchStatus = MatchStatus.UNMATCHED_EXTERNAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_status", nullable = false, length = 50)
    @Builder.Default
    private ResolutionStatus resolutionStatus = ResolutionStatus.PENDING;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public void resolve(String performedBy, String notes) {
        this.resolutionStatus = ResolutionStatus.RESOLVED;
        this.resolvedBy = performedBy;
        this.resolutionNotes = notes;
        this.resolvedAt = Instant.now();
    }
}

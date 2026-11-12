package com.fashion.platform.finance.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tax_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 50)
    private TaxType taxType;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_method", length = 50)
    private TaxMethod taxMethod;

    @Column(name = "config_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private String configJson = "{}";

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public boolean isApproved() {
        return approvedAt != null && approvedBy != null;
    }

    public boolean isEffectiveAt(LocalDate date) {
        if (!isApproved()) {
            return false;
        }
        boolean afterOrOnFrom = !date.isBefore(effectiveFrom);
        boolean beforeOrOnTo = (effectiveTo == null) || !date.isAfter(effectiveTo);
        return afterOrOnFrom && beforeOrOnTo;
    }

    public void approve(UUID approverId) {
        if (this.createdBy != null && this.createdBy.equals(approverId)) {
            throw new IllegalStateException("Maker-Checker violation: Creator cannot approve their own tax configuration");
        }
        this.approvedBy = approverId;
        this.approvedAt = Instant.now();
    }
}


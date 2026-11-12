package com.fashion.platform.finance.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "costs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @Column(name = "cost_code", nullable = false, length = 60)
    private String costCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_category", nullable = false, length = 50)
    private CostCategory costCategory;

    @Column(name = "cost_type", nullable = false, length = 50)
    private String costType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "VND";

    @Column(name = "incurred_at", nullable = false)
    private Instant incurredAt;

    @Column(name = "vendor_id")
    private UUID vendorId;

    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CostStatus status = CostStatus.RECORDED;

    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "created_by")
    private UUID createdBy;

    @OneToMany(mappedBy = "cost", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CostAllocation> allocations = new ArrayList<>();

    public BigDecimal getTotalAllocatedAmount() {
        return allocations.stream()
                .map(CostAllocation::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void addAllocation(CostAllocation allocation) {
        BigDecimal newTotal = getTotalAllocatedAmount().add(allocation.getAllocatedAmount());
        if (newTotal.compareTo(this.amount) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Total allocated amount (%s) exceeds original cost amount (%s)", newTotal, this.amount));
        }
        allocations.add(allocation);
        allocation.setCost(this);
        if (newTotal.compareTo(this.amount) == 0) {
            this.status = CostStatus.ALLOCATED;
        }
    }
}


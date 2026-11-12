package com.fashion.platform.finance.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cost_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cost_id", nullable = false)
    private Cost cost;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_target_type", nullable = false, length = 50)
    private AllocationTargetType allocationTargetType;

    @Column(name = "target_id", nullable = false, length = 150)
    private String targetId;

    @Column(name = "allocated_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal allocatedAmount;

    @Column(name = "allocation_ratio", precision = 9, scale = 6)
    private BigDecimal allocationRatio;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}


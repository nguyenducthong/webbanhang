package com.fashion.platform.reconciliation.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pos_shift_cash_counts",
       indexes = {
           @Index(name = "idx_pos_cash_count_shift", columnList = "shift_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosShiftCashCount {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private PosShift shift;

    @Column(name = "denomination", nullable = false)
    private int denomination;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private int quantity = 0;

    @Column(name = "total_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;
}

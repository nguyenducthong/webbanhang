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
@Table(name = "pos_shifts",
       indexes = {
           @Index(name = "idx_pos_shift_entity", columnList = "legal_entity_id"),
           @Index(name = "idx_pos_shift_store", columnList = "store_code"),
           @Index(name = "idx_pos_shift_status", columnList = "status")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosShift {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @Column(name = "store_code", nullable = false, length = 50)
    private String storeCode;

    @Column(name = "shift_code", nullable = false, unique = true, length = 50)
    private String shiftCode;

    @Column(name = "cashier_username", nullable = false, length = 100)
    private String cashierUsername;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PosShiftStatus status = PosShiftStatus.OPEN;

    @Column(name = "opening_float", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal openingFloat = BigDecimal.ZERO;

    @Column(name = "total_cash_sales", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalCashSales = BigDecimal.ZERO;

    @Column(name = "total_card_sales", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalCardSales = BigDecimal.ZERO;

    @Column(name = "total_qr_sales", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalQrSales = BigDecimal.ZERO;

    @Column(name = "expected_cash_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal expectedCashAmount = BigDecimal.ZERO;

    @Column(name = "actual_counted_cash", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal actualCountedCash = BigDecimal.ZERO;

    @Column(name = "difference_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal differenceAmount = BigDecimal.ZERO;

    @Column(name = "closing_notes", columnDefinition = "TEXT")
    private String closingNotes;

    @Column(name = "manager_approved_by", length = 100)
    private String managerApprovedBy;

    @OneToMany(mappedBy = "shift", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PosShiftCashCount> cashCounts = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public void addCashCount(PosShiftCashCount count) {
        count.setShift(this);
        this.cashCounts.add(count);
    }
}

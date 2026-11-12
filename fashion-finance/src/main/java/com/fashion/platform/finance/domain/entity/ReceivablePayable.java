package com.fashion.platform.finance.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "receivables_payables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceivablePayable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private EntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", nullable = false, length = 40)
    private PartyType partyType;

    @Column(name = "party_id", nullable = false, length = 150)
    private String partyId;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "source_id", nullable = false, length = 150)
    private String sourceId;

    @Column(name = "original_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal originalAmount;

    @Column(name = "settled_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal settledAmount = BigDecimal.ZERO;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "VND";

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReceivableStatus status = ReceivableStatus.OPEN;

    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    public BigDecimal getRemainingAmount() {
        return originalAmount.subtract(settledAmount);
    }

    public void settle(BigDecimal amountToSettle) {
        if (amountToSettle.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Settlement amount must be positive");
        }
        BigDecimal newSettled = this.settledAmount.add(amountToSettle);
        if (newSettled.compareTo(this.originalAmount) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Total settled amount (%s) exceeds original amount (%s)", newSettled, this.originalAmount));
        }
        this.settledAmount = newSettled;
        if (this.settledAmount.compareTo(this.originalAmount) == 0) {
            this.status = ReceivableStatus.SETTLED;
        } else {
            this.status = ReceivableStatus.PARTIALLY_SETTLED;
        }
        this.updatedAt = Instant.now();
    }
}


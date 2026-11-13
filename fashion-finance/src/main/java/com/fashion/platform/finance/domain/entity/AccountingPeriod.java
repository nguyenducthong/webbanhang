package com.fashion.platform.finance.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "accounting_periods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @Column(name = "period_code", nullable = false, length = 20)
    private String periodCode;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PeriodStatus status = PeriodStatus.OPEN;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public boolean isOpen() {
        return status == PeriodStatus.OPEN || status == PeriodStatus.REOPENED;
    }

    public void close(UUID actorId) {
        if (this.status == PeriodStatus.CLOSED) {
            throw new IllegalStateException("Accounting period is already closed");
        }
        this.status = PeriodStatus.CLOSED;
        this.closedAt = Instant.now();
        this.closedBy = actorId;
    }

    public void reopen(UUID actorId) {
        if (this.status != PeriodStatus.CLOSED) {
            throw new IllegalStateException("Only closed accounting periods can be reopened");
        }
        this.status = PeriodStatus.REOPENED;
        this.closedAt = null;
        this.closedBy = null;
    }
}

package com.fashion.platform.finance.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private AccountingPeriod period;

    @Column(name = "entry_no", nullable = false, length = 60)
    private String entryNo;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_id", length = 150)
    private String sourceId;

    @Column(name = "posting_date", nullable = false)
    private LocalDate postingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_status", nullable = false, length = 20)
    @Builder.Default
    private JournalEntryStatus entryStatus = JournalEntryStatus.DRAFT;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversed_by_id")
    private JournalEntry reversedBy;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reverses_id")
    private JournalEntry reverses;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "posted_by")
    private UUID postedBy;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JournalEntryLine> lines = new ArrayList<>();

    public void addLine(JournalEntryLine line) {
        lines.add(line);
        line.setJournalEntry(this);
    }

    public BigDecimal getTotalDebit() {
        return lines.stream()
                .map(JournalEntryLine::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalCredit() {
        return lines.stream()
                .map(JournalEntryLine::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void validateBalanced() {
        BigDecimal totalDebit = getTotalDebit();
        BigDecimal totalCredit = getTotalCredit();

        if (totalDebit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Journal entry must have at least one debit line greater than zero");
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new UnbalancedJournalEntryException(totalDebit, totalCredit);
        }
    }

    public void post(UUID actorId) {
        if (!period.isOpen()) {
            throw new IllegalStateException(String.format(
                    "Cannot post journal entry into %s accounting period: %s", period.getStatus(), period.getPeriodCode()));
        }
        if (entryStatus == JournalEntryStatus.POSTED) {
            throw new IllegalStateException("Journal entry is already posted");
        }
        if (entryStatus == JournalEntryStatus.REVERSED) {
            throw new IllegalStateException("Cannot post a reversed journal entry");
        }

        validateBalanced();

        this.entryStatus = JournalEntryStatus.POSTED;
        this.postedAt = Instant.now();
        this.postedBy = actorId;
    }

    public JournalEntry createReversal(String reversalEntryNo, AccountingPeriod currentPeriod, UUID actorId) {
        if (this.reversedBy != null || this.entryStatus == JournalEntryStatus.REVERSED) {
            throw new IllegalStateException("Journal entry has already been reversed");
        }
        if (this.entryStatus != JournalEntryStatus.POSTED) {
            throw new IllegalStateException("Only posted journal entries can be reversed");
        }

        JournalEntry reversal = JournalEntry.builder()
                .legalEntity(this.legalEntity)
                .period(currentPeriod)
                .entryNo(reversalEntryNo)
                .sourceType("REVERSAL")
                .sourceId(this.entryNo)
                .postingDate(LocalDate.now())
                .entryStatus(JournalEntryStatus.POSTED)
                .reverses(this)
                .description("Reversal of entry " + this.entryNo + (this.description != null ? ": " + this.description : ""))
                .postedAt(Instant.now())
                .postedBy(actorId)
                .build();

        // Swap Debits and Credits
        for (JournalEntryLine line : this.lines) {
            JournalEntryLine reversedLine = JournalEntryLine.builder()
                    .account(line.getAccount())
                    .debitAmount(line.getCreditAmount())   // Credit becomes Debit
                    .creditAmount(line.getDebitAmount())   // Debit becomes Credit
                    .currencyCode(line.getCurrencyCode())
                    .description("Reversal: " + (line.getDescription() != null ? line.getDescription() : ""))
                    .build();
            reversal.addLine(reversedLine);
        }

        this.entryStatus = JournalEntryStatus.REVERSED;
        this.reversedBy = reversal;

        return reversal;
    }
}

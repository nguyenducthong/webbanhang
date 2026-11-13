package com.fashion.platform.finance.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cash_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @Column(name = "account_name", nullable = false, length = 150)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private CashAccountType accountType;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "account_number_masked", length = 100)
    private String accountNumberMasked;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "VND";

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "cashAccount", cascade = CascadeType.ALL)
    @Builder.Default
    private List<CashTransaction> transactions = new ArrayList<>();

    public BigDecimal calculateCurrentBalance() {
        BigDecimal balance = BigDecimal.ZERO;
        for (CashTransaction tx : transactions) {
            if ("CONFIRMED".equals(tx.getStatus())) {
                if (tx.getTransactionType() == CashTransactionType.INFLOW) {
                    balance = balance.add(tx.getAmount());
                } else if (tx.getTransactionType() == CashTransactionType.OUTFLOW) {
                    balance = balance.subtract(tx.getAmount());
                }
            }
        }
        return balance;
    }
}

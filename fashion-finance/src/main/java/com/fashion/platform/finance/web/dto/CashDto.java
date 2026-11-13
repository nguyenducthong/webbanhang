package com.fashion.platform.finance.web.dto;

import com.fashion.platform.finance.domain.entity.CashAccount;
import com.fashion.platform.finance.domain.entity.CashAccountType;
import com.fashion.platform.finance.domain.entity.CashTransaction;
import com.fashion.platform.finance.domain.entity.CashTransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CashDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountCreateRequest {
        @NotBlank(message = "Account name is required")
        private String accountName;

        @NotNull(message = "Account type is required")
        private CashAccountType accountType;

        private String bankName;
        private String accountNumberMasked;
        private String currencyCode;
    }

    @Value
    @Builder
    public static class AccountResponse {
        UUID id;
        UUID legalEntityId;
        String accountName;
        CashAccountType accountType;
        String bankName;
        String accountNumberMasked;
        String currencyCode;
        String status;
        BigDecimal currentBalance;
        Instant createdAt;

        public static AccountResponse from(CashAccount acc) {
            return AccountResponse.builder()
                    .id(acc.getId())
                    .legalEntityId(acc.getLegalEntity().getId())
                    .accountName(acc.getAccountName())
                    .accountType(acc.getAccountType())
                    .bankName(acc.getBankName())
                    .accountNumberMasked(acc.getAccountNumberMasked())
                    .currencyCode(acc.getCurrencyCode())
                    .status(acc.getStatus())
                    .currentBalance(acc.calculateCurrentBalance())
                    .createdAt(acc.getCreatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionCreateRequest {
        @NotNull(message = "Transaction type is required (INFLOW, OUTFLOW, TRANSFER)")
        private CashTransactionType transactionType;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private BigDecimal amount;

        private String sourceType;
        private String sourceId;
        private Instant occurredAt;
        private String description;
        private String metadata;
    }

    @Value
    @Builder
    public static class TransactionResponse {
        UUID id;
        UUID legalEntityId;
        UUID cashAccountId;
        CashTransactionType transactionType;
        String sourceType;
        String sourceId;
        BigDecimal amount;
        Instant occurredAt;
        String status;
        String description;
        Instant createdAt;

        public static TransactionResponse from(CashTransaction tx) {
            return TransactionResponse.builder()
                    .id(tx.getId())
                    .legalEntityId(tx.getLegalEntity().getId())
                    .cashAccountId(tx.getCashAccount().getId())
                    .transactionType(tx.getTransactionType())
                    .sourceType(tx.getSourceType())
                    .sourceId(tx.getSourceId())
                    .amount(tx.getAmount())
                    .occurredAt(tx.getOccurredAt())
                    .status(tx.getStatus())
                    .description(tx.getDescription())
                    .createdAt(tx.getCreatedAt())
                    .build();
        }
    }
}

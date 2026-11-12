package com.fashion.platform.finance.web.dto;

import com.fashion.platform.finance.domain.entity.FinanceTransactionType;
import com.fashion.platform.finance.domain.entity.TransactionChannel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionIngestRequest {

    @NotBlank(message = "Transaction code is required")
    private String transactionCode;

    @NotNull(message = "Transaction type is required")
    private FinanceTransactionType transactionType;

    @NotBlank(message = "Source type is required (e.g. ORDER, PAYMENT_INTENT)")
    private String sourceType;

    @NotBlank(message = "Source ID is required")
    private String sourceId;

    @NotNull(message = "Channel is required")
    private TransactionChannel channel;

    @NotNull(message = "Amount is required")
    @PositiveOrZero(message = "Amount must not be negative")
    private BigDecimal amount;

    private String currencyCode;
    private Instant occurredAt;
    private String description;
    private String metadata;

    @Valid
    @Builder.Default
    private List<TransactionLineDto> lines = new ArrayList<>();
}


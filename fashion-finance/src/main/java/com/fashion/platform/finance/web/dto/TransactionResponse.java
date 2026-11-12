package com.fashion.platform.finance.web.dto;

import com.fashion.platform.finance.domain.entity.FinanceTransaction;
import com.fashion.platform.finance.domain.entity.FinanceTransactionStatus;
import com.fashion.platform.finance.domain.entity.FinanceTransactionType;
import com.fashion.platform.finance.domain.entity.TransactionChannel;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class TransactionResponse {
    UUID id;
    UUID legalEntityId;
    String transactionCode;
    FinanceTransactionType transactionType;
    String sourceType;
    String sourceId;
    TransactionChannel channel;
    BigDecimal amount;
    String currencyCode;
    Instant occurredAt;
    FinanceTransactionStatus status;
    String description;
    String metadata;
    List<LineResponse> lines;
    Instant createdAt;

    @Value
    @Builder
    public static class LineResponse {
        UUID id;
        String lineType;
        String referenceType;
        String referenceId;
        BigDecimal amount;
        String description;
    }

    public static TransactionResponse from(FinanceTransaction tx) {
        List<LineResponse> lineResponses = tx.getLines().stream()
                .map(l -> LineResponse.builder()
                        .id(l.getId())
                        .lineType(l.getLineType())
                        .referenceType(l.getReferenceType())
                        .referenceId(l.getReferenceId())
                        .amount(l.getAmount())
                        .description(l.getDescription())
                        .build())
                .toList();

        return TransactionResponse.builder()
                .id(tx.getId())
                .legalEntityId(tx.getLegalEntity().getId())
                .transactionCode(tx.getTransactionCode())
                .transactionType(tx.getTransactionType())
                .sourceType(tx.getSourceType())
                .sourceId(tx.getSourceId())
                .channel(tx.getChannel())
                .amount(tx.getAmount())
                .currencyCode(tx.getCurrencyCode())
                .occurredAt(tx.getOccurredAt())
                .status(tx.getStatus())
                .description(tx.getDescription())
                .metadata(tx.getMetadata())
                .lines(lineResponses)
                .createdAt(tx.getCreatedAt())
                .build();
    }
}


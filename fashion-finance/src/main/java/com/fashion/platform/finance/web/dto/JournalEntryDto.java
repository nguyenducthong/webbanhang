package com.fashion.platform.finance.web.dto;

import com.fashion.platform.finance.domain.entity.JournalEntry;
import com.fashion.platform.finance.domain.entity.JournalEntryStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class JournalEntryDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineRequest {
        @NotNull(message = "Account ID is required")
        private UUID accountId;

        @PositiveOrZero(message = "Debit amount must be >= 0")
        @Builder.Default
        private BigDecimal debitAmount = BigDecimal.ZERO;

        @PositiveOrZero(message = "Credit amount must be >= 0")
        @Builder.Default
        private BigDecimal creditAmount = BigDecimal.ZERO;

        private String currencyCode;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotNull(message = "Period ID is required")
        private UUID periodId;

        @NotBlank(message = "Entry No is required")
        private String entryNo;

        private String sourceType;
        private String sourceId;

        @NotNull(message = "Posting date is required")
        private LocalDate postingDate;

        private String description;

        @Valid
        @NotEmpty(message = "Journal entry must have at least one line")
        private List<LineRequest> lines;
    }

    @Value
    @Builder
    public static class Response {
        UUID id;
        UUID legalEntityId;
        UUID periodId;
        String entryNo;
        String sourceType;
        String sourceId;
        LocalDate postingDate;
        JournalEntryStatus status;
        BigDecimal totalDebit;
        BigDecimal totalCredit;
        UUID reversedById;
        UUID reversesId;
        String description;
        List<LineResponse> lines;
        Instant postedAt;
        UUID postedBy;
        Instant createdAt;

        @Value
        @Builder
        public static class LineResponse {
            UUID id;
            UUID accountId;
            String accountCode;
            String accountName;
            BigDecimal debitAmount;
            BigDecimal creditAmount;
            String currencyCode;
            String description;
        }

        public static Response from(JournalEntry entry) {
            List<LineResponse> lineResponses = entry.getLines().stream()
                    .map(l -> LineResponse.builder()
                            .id(l.getId())
                            .accountId(l.getAccount().getId())
                            .accountCode(l.getAccount().getAccountCode())
                            .accountName(l.getAccount().getAccountName())
                            .debitAmount(l.getDebitAmount())
                            .creditAmount(l.getCreditAmount())
                            .currencyCode(l.getCurrencyCode())
                            .description(l.getDescription())
                            .build())
                    .toList();

            return Response.builder()
                    .id(entry.getId())
                    .legalEntityId(entry.getLegalEntity().getId())
                    .periodId(entry.getPeriod().getId())
                    .entryNo(entry.getEntryNo())
                    .sourceType(entry.getSourceType())
                    .sourceId(entry.getSourceId())
                    .postingDate(entry.getPostingDate())
                    .status(entry.getEntryStatus())
                    .totalDebit(entry.getTotalDebit())
                    .totalCredit(entry.getTotalCredit())
                    .reversedById(entry.getReversedBy() != null ? entry.getReversedBy().getId() : null)
                    .reversesId(entry.getReverses() != null ? entry.getReverses().getId() : null)
                    .description(entry.getDescription())
                    .lines(lineResponses)
                    .postedAt(entry.getPostedAt())
                    .postedBy(entry.getPostedBy())
                    .createdAt(entry.getCreatedAt())
                    .build();
        }
    }
}

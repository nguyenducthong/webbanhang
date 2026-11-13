package com.fashion.platform.finance.web.dto;

import com.fashion.platform.finance.domain.entity.AccountingPeriod;
import com.fashion.platform.finance.domain.entity.PeriodStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class AccountingPeriodDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Period code is required (e.g. 2026-M11)")
        private String periodCode;

        @NotNull(message = "Start date is required")
        private LocalDate startDate;

        @NotNull(message = "End date is required")
        private LocalDate endDate;
    }

    @Value
    @Builder
    public static class Response {
        UUID id;
        UUID legalEntityId;
        String periodCode;
        LocalDate startDate;
        LocalDate endDate;
        PeriodStatus status;
        Instant closedAt;
        UUID closedBy;
        Instant createdAt;

        public static Response from(AccountingPeriod p) {
            return Response.builder()
                    .id(p.getId())
                    .legalEntityId(p.getLegalEntity().getId())
                    .periodCode(p.getPeriodCode())
                    .startDate(p.getStartDate())
                    .endDate(p.getEndDate())
                    .status(p.getStatus())
                    .closedAt(p.getClosedAt())
                    .closedBy(p.getClosedBy())
                    .createdAt(p.getCreatedAt())
                    .build();
        }
    }
}

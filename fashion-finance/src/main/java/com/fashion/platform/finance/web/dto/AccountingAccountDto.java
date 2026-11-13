package com.fashion.platform.finance.web.dto;

import com.fashion.platform.finance.domain.entity.AccountType;
import com.fashion.platform.finance.domain.entity.AccountingAccount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

public class AccountingAccountDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Account code is required (e.g. 111, 112)")
        private String accountCode;

        @NotBlank(message = "Account name is required")
        private String accountName;

        @NotNull(message = "Account type is required")
        private AccountType accountType;

        private UUID parentId;
    }

    @Value
    @Builder
    public static class Response {
        UUID id;
        UUID legalEntityId;
        String accountCode;
        String accountName;
        AccountType accountType;
        UUID parentId;
        Boolean isActive;
        Instant createdAt;

        public static Response from(AccountingAccount a) {
            return Response.builder()
                    .id(a.getId())
                    .legalEntityId(a.getLegalEntity().getId())
                    .accountCode(a.getAccountCode())
                    .accountName(a.getAccountName())
                    .accountType(a.getAccountType())
                    .parentId(a.getParent() != null ? a.getParent().getId() : null)
                    .isActive(a.getIsActive())
                    .createdAt(a.getCreatedAt())
                    .build();
        }
    }
}

package com.fashion.platform.finance.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionLineDto {

    @NotBlank(message = "Line type is required")
    private String lineType;

    private String referenceType;
    private String referenceId;

    @NotNull(message = "Line amount is required")
    @PositiveOrZero(message = "Line amount must not be negative")
    private BigDecimal amount;

    private String description;
    private String metadata;
}


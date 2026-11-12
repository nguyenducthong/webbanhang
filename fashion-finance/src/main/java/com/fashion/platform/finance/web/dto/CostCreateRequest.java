package com.fashion.platform.finance.web.dto;

import com.fashion.platform.finance.domain.entity.CostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCreateRequest {

    @NotBlank(message = "Cost code is required")
    private String costCode;

    @NotNull(message = "Cost category is required")
    private CostCategory costCategory;

    @NotBlank(message = "Cost type is required")
    private String costType;

    @NotNull(message = "Amount is required")
    @PositiveOrZero(message = "Amount must not be negative")
    private BigDecimal amount;

    private String currencyCode;
    private Instant incurredAt;
    private UUID vendorId;
    private String referenceNo;
    private String metadata;
}


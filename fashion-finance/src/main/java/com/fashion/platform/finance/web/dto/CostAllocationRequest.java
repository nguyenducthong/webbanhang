package com.fashion.platform.finance.web.dto;

import com.fashion.platform.finance.domain.entity.AllocationTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostAllocationRequest {

    @NotNull(message = "Allocation target type is required")
    private AllocationTargetType allocationTargetType;

    @NotBlank(message = "Target ID is required")
    private String targetId;

    @NotNull(message = "Allocated amount is required")
    @Positive(message = "Allocated amount must be positive")
    private BigDecimal allocatedAmount;
}


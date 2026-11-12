package com.fashion.platform.finance.web.dto;

import com.fashion.platform.finance.domain.entity.AllocationTargetType;
import com.fashion.platform.finance.domain.entity.Cost;
import com.fashion.platform.finance.domain.entity.CostCategory;
import com.fashion.platform.finance.domain.entity.CostStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class CostResponse {
    UUID id;
    UUID legalEntityId;
    String costCode;
    CostCategory costCategory;
    String costType;
    BigDecimal amount;
    BigDecimal totalAllocated;
    String currencyCode;
    Instant incurredAt;
    UUID vendorId;
    String referenceNo;
    CostStatus status;
    List<AllocationResponse> allocations;
    Instant createdAt;

    @Value
    @Builder
    public static class AllocationResponse {
        UUID id;
        AllocationTargetType targetType;
        String targetId;
        BigDecimal allocatedAmount;
        BigDecimal allocationRatio;
    }

    public static CostResponse from(Cost cost) {
        List<AllocationResponse> allocs = cost.getAllocations().stream()
                .map(a -> AllocationResponse.builder()
                        .id(a.getId())
                        .targetType(a.getAllocationTargetType())
                        .targetId(a.getTargetId())
                        .allocatedAmount(a.getAllocatedAmount())
                        .allocationRatio(a.getAllocationRatio())
                        .build())
                .toList();

        return CostResponse.builder()
                .id(cost.getId())
                .legalEntityId(cost.getLegalEntity().getId())
                .costCode(cost.getCostCode())
                .costCategory(cost.getCostCategory())
                .costType(cost.getCostType())
                .amount(cost.getAmount())
                .totalAllocated(cost.getTotalAllocatedAmount())
                .currencyCode(cost.getCurrencyCode())
                .incurredAt(cost.getIncurredAt())
                .vendorId(cost.getVendorId())
                .referenceNo(cost.getReferenceNo())
                .status(cost.getStatus())
                .allocations(allocs)
                .createdAt(cost.getCreatedAt())
                .build();
    }
}


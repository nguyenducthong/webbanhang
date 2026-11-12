package com.fashion.platform.finance.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CostAllocationTest {

    @Test
    @DisplayName("Should successfully allocate cost within total amount and update status to ALLOCATED")
    void testSuccessfulAllocation() {
        Cost cost = Cost.builder()
                .amount(new BigDecimal("1000000.0000"))
                .currencyCode("VND")
                .status(CostStatus.RECORDED)
                .build();

        CostAllocation a1 = CostAllocation.builder()
                .allocationTargetType(AllocationTargetType.CAMPAIGN)
                .targetId("SUMMER_SALE_2026")
                .allocatedAmount(new BigDecimal("400000.0000"))
                .build();

        cost.addAllocation(a1);
        assertThat(cost.getTotalAllocatedAmount()).isEqualByComparingTo(new BigDecimal("400000.0000"));
        assertThat(cost.getStatus()).isEqualTo(CostStatus.RECORDED);

        CostAllocation a2 = CostAllocation.builder()
                .allocationTargetType(AllocationTargetType.STORE)
                .targetId("STORE_HN_01")
                .allocatedAmount(new BigDecimal("600000.0000"))
                .build();

        cost.addAllocation(a2);
        assertThat(cost.getTotalAllocatedAmount()).isEqualByComparingTo(new BigDecimal("1000000.0000"));
        assertThat(cost.getStatus()).isEqualTo(CostStatus.ALLOCATED);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when allocation exceeds original cost amount")
    void testExceedingAllocation() {
        Cost cost = Cost.builder()
                .amount(new BigDecimal("500000.0000"))
                .status(CostStatus.RECORDED)
                .build();

        CostAllocation a1 = CostAllocation.builder()
                .allocationTargetType(AllocationTargetType.ORDER)
                .targetId("ORD-999")
                .allocatedAmount(new BigDecimal("550000.0000"))
                .build();

        assertThatThrownBy(() -> cost.addAllocation(a1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds original cost amount");
    }
}


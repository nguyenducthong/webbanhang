package com.fashion.platform.reconciliation.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeVarianceReportResponse {
    private String channel;
    private long totalOrders;
    private BigDecimal totalGrossSales;
    private BigDecimal totalActualFee;
    private BigDecimal totalContractFee;
    private BigDecimal feeVarianceAmount;
    private double effectiveFeeRate;
    private double contractFeeRate;
    private String status; // NORMAL, HIGH_VARIANCE
    private String notes;
}

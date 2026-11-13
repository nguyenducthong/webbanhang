package com.fashion.platform.reconciliation.application.service;

import com.fashion.platform.reconciliation.api.dto.FeeVarianceReportResponse;
import com.fashion.platform.reconciliation.domain.model.ReconciliationBatch;
import com.fashion.platform.reconciliation.domain.model.ReconciliationItem;
import com.fashion.platform.reconciliation.domain.model.ReconciliationSourceType;
import com.fashion.platform.reconciliation.domain.repository.ReconciliationBatchRepository;
import com.fashion.platform.reconciliation.domain.repository.ReconciliationItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationReportService {

    private final ReconciliationBatchRepository batchRepository;
    private final ReconciliationItemRepository itemRepository;

    private static final Map<ReconciliationSourceType, Double> CONTRACT_RATES = Map.of(
            ReconciliationSourceType.SHOPEE, 7.5,
            ReconciliationSourceType.TIKTOK, 8.5,
            ReconciliationSourceType.COD, 3.0,
            ReconciliationSourceType.BANK, 0.0
    );

    @Transactional(readOnly = true)
    public List<FeeVarianceReportResponse> generateFeeVarianceReport(UUID legalEntityId) {
        List<ReconciliationBatch> batches = batchRepository.findByLegalEntityIdOrderByCreatedAtDesc(legalEntityId);
        if (batches.isEmpty()) {
            return Collections.emptyList();
        }

        Map<ReconciliationSourceType, List<ReconciliationItem>> itemsBySource = new HashMap<>();
        for (ReconciliationBatch batch : batches) {
            List<ReconciliationItem> items = itemRepository.findByBatchId(batch.getId());
            itemsBySource.computeIfAbsent(batch.getSourceType(), k -> new ArrayList<>()).addAll(items);
        }

        List<FeeVarianceReportResponse> report = new ArrayList<>();

        for (Map.Entry<ReconciliationSourceType, List<ReconciliationItem>> entry : itemsBySource.entrySet()) {
            ReconciliationSourceType source = entry.getKey();
            List<ReconciliationItem> items = entry.getValue();

            long totalOrders = items.size();
            BigDecimal totalNetPayout = BigDecimal.ZERO;
            BigDecimal totalActualFee = BigDecimal.ZERO;

            for (ReconciliationItem item : items) {
                totalNetPayout = totalNetPayout.add(item.getExternalAmount());
                totalActualFee = totalActualFee.add(item.getFeeExternal());
            }

            BigDecimal totalGrossSales = totalNetPayout.add(totalActualFee);
            double contractRate = CONTRACT_RATES.getOrDefault(source, 5.0);

            BigDecimal totalContractFee = totalGrossSales.multiply(BigDecimal.valueOf(contractRate))
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            BigDecimal feeVarianceAmount = totalActualFee.subtract(totalContractFee);

            double effectiveFeeRate = totalGrossSales.compareTo(BigDecimal.ZERO) > 0
                    ? totalActualFee.divide(totalGrossSales, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;

            double variancePercent = Math.abs(effectiveFeeRate - contractRate);
            String status = variancePercent > 1.0 ? "HIGH_VARIANCE" : "NORMAL";

            String notes;
            if ("HIGH_VARIANCE".equals(status)) {
                notes = String.format("Cảnh báo: Tỷ lệ phí thực tế (%.2f%%) lệch hơn %.2f%% so với hợp đồng (%.2f%%). Cần kiểm tra chi phí voucher sàn hoặc cước vượt cân.",
                        effectiveFeeRate, variancePercent, contractRate);
            } else {
                notes = String.format("Phí sàn nằm trong ngưỡng kiểm soát chuẩn (%.2f%% so với chuẩn %.2f%%).",
                        effectiveFeeRate, contractRate);
            }

            report.add(FeeVarianceReportResponse.builder()
                    .channel(source.name())
                    .totalOrders(totalOrders)
                    .totalGrossSales(totalGrossSales)
                    .totalActualFee(totalActualFee)
                    .totalContractFee(totalContractFee)
                    .feeVarianceAmount(feeVarianceAmount)
                    .effectiveFeeRate(effectiveFeeRate)
                    .contractFeeRate(contractRate)
                    .status(status)
                    .notes(notes)
                    .build());
        }

        return report;
    }
}

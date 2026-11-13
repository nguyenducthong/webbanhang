package com.fashion.platform.reconciliation;

import com.fashion.platform.reconciliation.api.dto.FeeVarianceReportResponse;
import com.fashion.platform.reconciliation.application.service.ReconciliationReportService;
import com.fashion.platform.reconciliation.domain.model.*;
import com.fashion.platform.reconciliation.domain.repository.ReconciliationBatchRepository;
import com.fashion.platform.reconciliation.domain.repository.ReconciliationItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeVarianceReportTest {

    @Mock
    private ReconciliationBatchRepository batchRepository;

    @Mock
    private ReconciliationItemRepository itemRepository;

    @InjectMocks
    private ReconciliationReportService reportService;

    @Test
    @DisplayName("Should generate fee variance report and detect high variance when rate exceeds contract threshold")
    void shouldGenerateFeeVarianceReport() {
        UUID legalEntityId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();

        ReconciliationBatch batch = ReconciliationBatch.builder()
                .id(batchId)
                .batchCode("REC-TIKTOK-01")
                .sourceType(ReconciliationSourceType.TIKTOK)
                .build();

        // 1 đơn TikTok: Doanh thu net 900k, Phí sàn 150k -> Gross = 1,050,000 VND
        // Effective fee rate = 150k / 1050k ~ 14.28% (so với hợp đồng chuẩn 8.5% -> lệch 5.78% > 1.0% -> HIGH_VARIANCE)
        ReconciliationItem item = ReconciliationItem.builder()
                .batch(batch)
                .externalOrderId("TT-999")
                .externalAmount(new BigDecimal("900000.0000"))
                .feeExternal(new BigDecimal("150000.0000"))
                .build();

        when(batchRepository.findByLegalEntityIdOrderByCreatedAtDesc(legalEntityId)).thenReturn(List.of(batch));
        when(itemRepository.findByBatchId(batchId)).thenReturn(List.of(item));

        List<FeeVarianceReportResponse> reports = reportService.generateFeeVarianceReport(legalEntityId);

        assertNotNull(reports);
        assertEquals(1, reports.size());

        FeeVarianceReportResponse tiktokReport = reports.get(0);
        assertEquals("TIKTOK", tiktokReport.getChannel());
        assertEquals(1, tiktokReport.getTotalOrders());
        assertEquals(new BigDecimal("1050000.0000"), tiktokReport.getTotalGrossSales());
        assertEquals(new BigDecimal("150000.0000"), tiktokReport.getTotalActualFee());
        assertEquals(8.5, tiktokReport.getContractFeeRate());
        assertEquals("HIGH_VARIANCE", tiktokReport.getStatus());
        assertTrue(tiktokReport.getEffectiveFeeRate() > 14.0);
    }
}

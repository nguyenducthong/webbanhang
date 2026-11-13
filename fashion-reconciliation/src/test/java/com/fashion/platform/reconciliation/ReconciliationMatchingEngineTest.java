package com.fashion.platform.reconciliation;

import com.fashion.platform.finance.domain.entity.FinanceTransaction;
import com.fashion.platform.finance.domain.entity.LegalEntity;
import com.fashion.platform.finance.domain.entity.LegalEntityStatus;
import com.fashion.platform.finance.repository.FinanceTransactionRepository;
import com.fashion.platform.finance.repository.LegalEntityRepository;
import com.fashion.platform.reconciliation.application.service.ReconciliationMatchingService;
import com.fashion.platform.reconciliation.domain.model.BatchStatus;
import com.fashion.platform.reconciliation.domain.model.MatchStatus;
import com.fashion.platform.reconciliation.domain.model.ReconciliationBatch;
import com.fashion.platform.reconciliation.domain.model.ReconciliationSourceType;
import com.fashion.platform.reconciliation.infrastructure.parser.ShopeeSettlementCsvParser;
import com.fashion.platform.reconciliation.domain.repository.ReconciliationBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ReconciliationMatchingEngineTest {

    private ReconciliationBatchRepository batchRepository;
    private LegalEntityRepository legalEntityRepository;
    private FinanceTransactionRepository financeTransactionRepository;
    private ReconciliationMatchingService matchingService;

    @BeforeEach
    void setUp() {
        batchRepository = Mockito.mock(ReconciliationBatchRepository.class);
        legalEntityRepository = Mockito.mock(LegalEntityRepository.class);
        financeTransactionRepository = Mockito.mock(FinanceTransactionRepository.class);

        matchingService = new ReconciliationMatchingService(
                batchRepository,
                legalEntityRepository,
                financeTransactionRepository,
                List.of(new ShopeeSettlementCsvParser())
        );
    }

    @Test
    @DisplayName("Should correctly match exact and fee mismatch items")
    void shouldMatchExactAndMismatchTransactions() {
        UUID legalEntityId = UUID.randomUUID();
        LegalEntity entity = LegalEntity.builder()
                .id(legalEntityId)
                .entityName("Fashion Store")
                .status(LegalEntityStatus.ACTIVE)
                .build();

        when(legalEntityRepository.findById(legalEntityId)).thenReturn(Optional.of(entity));
        when(batchRepository.save(any(ReconciliationBatch.class))).thenAnswer(i -> i.getArgument(0));

        // Nội bộ có đơn SHOPEE-ORD-101 đúng số tiền net 585,000 VND
        FinanceTransaction tx1 = FinanceTransaction.builder()
                .id(UUID.randomUUID())
                .transactionCode("FT-001")
                .sourceId("SHOPEE-ORD-101")
                .amount(new BigDecimal("585000.0000"))
                .build();
        when(financeTransactionRepository.findFirstByLegalEntityIdAndSourceId(legalEntityId, "SHOPEE-ORD-101"))
                .thenReturn(Optional.of(tx1));

        // Nội bộ có đơn SHOPEE-ORD-102 nhưng số tiền nội bộ là 390,000 VND (lệch so với sàn báo 378,000 VND)
        FinanceTransaction tx2 = FinanceTransaction.builder()
                .id(UUID.randomUUID())
                .transactionCode("FT-002")
                .sourceId("SHOPEE-ORD-102")
                .amount(new BigDecimal("390000.0000"))
                .build();
        when(financeTransactionRepository.findFirstByLegalEntityIdAndSourceId(legalEntityId, "SHOPEE-ORD-102"))
                .thenReturn(Optional.of(tx2));

        // Đơn SHOPEE-ORD-999 không có trong nội bộ
        when(financeTransactionRepository.findFirstByLegalEntityIdAndSourceId(legalEntityId, "SHOPEE-ORD-999"))
                .thenReturn(Optional.empty());

        String csvContent = "Order ID,Gross,Fee,Net\n" +
                "SHOPEE-ORD-101,650000,65000,585000\n" +
                "SHOPEE-ORD-102,420000,42000,378000\n" +
                "SHOPEE-ORD-999,100000,10000,90000\n";

        ReconciliationBatch batch = matchingService.processSettlementFile(
                legalEntityId,
                ReconciliationSourceType.SHOPEE,
                "shopee_settlement_test.csv",
                csvContent.getBytes(StandardCharsets.UTF_8),
                "accountant_tester"
        );

        assertEquals(3, batch.getTotalRecords());
        assertEquals(1, batch.getMatchedRecords()); // Chỉ 101 khớp EXACT_MATCH
        assertEquals(2, batch.getDiscrepancyRecords()); // 102 (FEE_MISMATCH) và 999 (UNMATCHED_EXTERNAL)
        assertEquals(BatchStatus.DISCREPANCY_FOUND, batch.getStatus());

        assertEquals(MatchStatus.EXACT_MATCH, batch.getItems().get(0).getMatchStatus());
        assertEquals(MatchStatus.FEE_MISMATCH, batch.getItems().get(1).getMatchStatus());
        assertEquals(MatchStatus.UNMATCHED_EXTERNAL, batch.getItems().get(2).getMatchStatus());
    }
}

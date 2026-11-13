package com.fashion.platform.reconciliation;

import com.fashion.platform.reconciliation.application.service.ReconciliationResolutionService;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManualResolutionTest {

    @Mock
    private ReconciliationItemRepository itemRepository;

    @Mock
    private ReconciliationBatchRepository batchRepository;

    @InjectMocks
    private ReconciliationResolutionService resolutionService;

    @Test
    @DisplayName("Should successfully resolve discrepancy and transition batch to COMPLETED when all items are resolved")
    void shouldResolveDiscrepancyAndCompleteBatch() {
        UUID batchId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        ReconciliationBatch batch = ReconciliationBatch.builder()
                .id(batchId)
                .batchCode("REC-SHOPEE-TEST-01")
                .status(BatchStatus.DISCREPANCY_FOUND)
                .build();

        ReconciliationItem item = ReconciliationItem.builder()
                .id(itemId)
                .batch(batch)
                .externalOrderId("SP-ORDER-999")
                .matchStatus(MatchStatus.FEE_MISMATCH)
                .resolutionStatus(ResolutionStatus.PENDING)
                .discrepancyAmount(new BigDecimal("15000.0000"))
                .build();

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(ReconciliationItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.findByBatchId(batchId)).thenReturn(List.of(item));

        ReconciliationItem resolved = resolutionService.resolveDiscrepancy(itemId, "Chấp thuận bù trừ phí voucher sàn tháng 11", "ke_toan_truong");

        assertNotNull(resolved);
        assertEquals(ResolutionStatus.RESOLVED, resolved.getResolutionStatus());
        assertEquals("ke_toan_truong", resolved.getResolvedBy());
        assertEquals("Chấp thuận bù trừ phí voucher sàn tháng 11", resolved.getResolutionNotes());
        assertNotNull(resolved.getResolvedAt());

        // Batch should transition to COMPLETED
        assertEquals(BatchStatus.COMPLETED, batch.getStatus());
        verify(batchRepository).save(batch);
    }
}

package com.fashion.platform.reconciliation.application.service;

import com.fashion.platform.reconciliation.domain.model.BatchStatus;
import com.fashion.platform.reconciliation.domain.model.ReconciliationBatch;
import com.fashion.platform.reconciliation.domain.model.ReconciliationItem;
import com.fashion.platform.reconciliation.domain.model.ResolutionStatus;
import com.fashion.platform.reconciliation.domain.repository.ReconciliationBatchRepository;
import com.fashion.platform.reconciliation.domain.repository.ReconciliationItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationResolutionService {

    private final ReconciliationItemRepository itemRepository;
    private final ReconciliationBatchRepository batchRepository;

    @Transactional
    public ReconciliationItem resolveDiscrepancy(UUID itemId, String notes, String performedBy) {
        ReconciliationItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Reconciliation item not found: " + itemId));

        item.resolve(performedBy, notes);
        item = itemRepository.save(item);

        // Kiểm tra xem tất cả items trong batch đã được xử lý chưa
        ReconciliationBatch batch = item.getBatch();
        List<ReconciliationItem> allItems = itemRepository.findByBatchId(batch.getId());

        boolean allResolved = allItems.stream()
                .allMatch(i -> i.getResolutionStatus() == ResolutionStatus.RESOLVED || i.getResolutionStatus() == ResolutionStatus.IGNORED);

        if (allResolved && batch.getStatus() != BatchStatus.COMPLETED) {
            batch.setStatus(BatchStatus.COMPLETED);
            batch.setNotes((batch.getNotes() != null ? batch.getNotes() + "\n" : "") + "Tất cả các chênh lệch đã được Kế toán trưởng phê duyệt xử lý.");
            batchRepository.save(batch);
            log.info("Batch {} automatically transitioned to COMPLETED after all items resolved.", batch.getBatchCode());
        }

        return item;
    }
}

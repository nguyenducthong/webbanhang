package com.fashion.platform.reconciliation.domain.repository;

import com.fashion.platform.reconciliation.domain.model.MatchStatus;
import com.fashion.platform.reconciliation.domain.model.ReconciliationItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReconciliationItemRepository extends JpaRepository<ReconciliationItem, UUID> {
    List<ReconciliationItem> findByBatchId(UUID batchId);
    Page<ReconciliationItem> findByBatchId(UUID batchId, Pageable pageable);
    Page<ReconciliationItem> findByBatchIdAndMatchStatus(UUID batchId, MatchStatus matchStatus, Pageable pageable);
}

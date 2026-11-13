package com.fashion.platform.reconciliation.domain.repository;

import com.fashion.platform.reconciliation.domain.model.BatchStatus;
import com.fashion.platform.reconciliation.domain.model.ReconciliationBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReconciliationBatchRepository extends JpaRepository<ReconciliationBatch, UUID> {
    Optional<ReconciliationBatch> findByBatchCode(String batchCode);
    List<ReconciliationBatch> findByLegalEntityIdOrderByCreatedAtDesc(UUID legalEntityId);
    Page<ReconciliationBatch> findByLegalEntityIdOrderByCreatedAtDesc(UUID legalEntityId, Pageable pageable);
    long countByStatus(BatchStatus status);
}

package com.fashion.platform.finance.repository;

import com.fashion.platform.finance.domain.entity.FinanceTransaction;
import com.fashion.platform.finance.domain.entity.FinanceTransactionType;
import com.fashion.platform.finance.domain.entity.TransactionChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, UUID> {

    Optional<FinanceTransaction> findByLegalEntityIdAndSourceTypeAndSourceIdAndTransactionType(
            UUID legalEntityId, String sourceType, String sourceId, FinanceTransactionType transactionType);

    boolean existsByLegalEntityIdAndSourceTypeAndSourceIdAndTransactionType(
            UUID legalEntityId, String sourceType, String sourceId, FinanceTransactionType transactionType);

    Page<FinanceTransaction> findByLegalEntityIdAndChannel(UUID legalEntityId, TransactionChannel channel, Pageable pageable);

    Page<FinanceTransaction> findByLegalEntityId(UUID legalEntityId, Pageable pageable);
}


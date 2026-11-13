package com.fashion.platform.finance.repository;

import com.fashion.platform.finance.domain.entity.CashTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CashTransactionRepository extends JpaRepository<CashTransaction, UUID> {
    Page<CashTransaction> findByCashAccountId(UUID cashAccountId, Pageable pageable);
    Page<CashTransaction> findByLegalEntityId(UUID legalEntityId, Pageable pageable);
}

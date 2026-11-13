package com.fashion.platform.finance.repository;

import com.fashion.platform.finance.domain.entity.CashAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CashAccountRepository extends JpaRepository<CashAccount, UUID> {
    List<CashAccount> findByLegalEntityIdAndStatus(UUID legalEntityId, String status);
    Page<CashAccount> findByLegalEntityId(UUID legalEntityId, Pageable pageable);
}

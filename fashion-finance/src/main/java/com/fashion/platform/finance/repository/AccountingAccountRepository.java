package com.fashion.platform.finance.repository;

import com.fashion.platform.finance.domain.entity.AccountingAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountingAccountRepository extends JpaRepository<AccountingAccount, UUID> {

    Optional<AccountingAccount> findByLegalEntityIdAndAccountCode(UUID legalEntityId, String accountCode);

    boolean existsByLegalEntityIdAndAccountCode(UUID legalEntityId, String accountCode);

    List<AccountingAccount> findByLegalEntityIdAndIsActiveTrue(UUID legalEntityId);

    Page<AccountingAccount> findByLegalEntityId(UUID legalEntityId, Pageable pageable);
}

package com.fashion.platform.finance.repository;

import com.fashion.platform.finance.domain.entity.AccountingPeriod;
import com.fashion.platform.finance.domain.entity.PeriodStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, UUID> {

    Optional<AccountingPeriod> findByLegalEntityIdAndPeriodCode(UUID legalEntityId, String periodCode);

    boolean existsByLegalEntityIdAndPeriodCode(UUID legalEntityId, String periodCode);

    Page<AccountingPeriod> findByLegalEntityId(UUID legalEntityId, Pageable pageable);

    Optional<AccountingPeriod> findFirstByLegalEntityIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatus(
            UUID legalEntityId, LocalDate date1, LocalDate date2, PeriodStatus status);
}

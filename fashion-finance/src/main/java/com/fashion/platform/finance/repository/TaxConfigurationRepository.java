package com.fashion.platform.finance.repository;

import com.fashion.platform.finance.domain.entity.TaxConfiguration;
import com.fashion.platform.finance.domain.entity.TaxType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxConfigurationRepository extends JpaRepository<TaxConfiguration, UUID> {

    List<TaxConfiguration> findByLegalEntityId(UUID legalEntityId);

    @Query("""
        SELECT tc FROM TaxConfiguration tc
        WHERE tc.legalEntity.id = :legalEntityId
          AND tc.taxType = :taxType
          AND tc.approvedAt IS NOT NULL
          AND tc.effectiveFrom <= :date
          AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :date)
        ORDER BY tc.effectiveFrom DESC
    """)
    List<TaxConfiguration> findEffectiveTaxConfigurations(
            @Param("legalEntityId") UUID legalEntityId,
            @Param("taxType") TaxType taxType,
            @Param("date") LocalDate date);
}


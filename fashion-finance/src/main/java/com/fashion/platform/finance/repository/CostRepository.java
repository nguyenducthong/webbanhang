package com.fashion.platform.finance.repository;

import com.fashion.platform.finance.domain.entity.Cost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CostRepository extends JpaRepository<Cost, UUID> {
    Optional<Cost> findByLegalEntityIdAndCostCode(UUID legalEntityId, String costCode);
    Page<Cost> findByLegalEntityId(UUID legalEntityId, Pageable pageable);
}


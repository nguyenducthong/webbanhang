package com.fashion.platform.finance.repository;

import com.fashion.platform.finance.domain.entity.LegalEntity;
import com.fashion.platform.finance.domain.entity.LegalEntityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalEntityRepository extends JpaRepository<LegalEntity, UUID> {
    Optional<LegalEntity> findByEntityCode(String entityCode);
    boolean existsByEntityCode(String entityCode);
    Page<LegalEntity> findByStatus(LegalEntityStatus status, Pageable pageable);
}

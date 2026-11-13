package com.fashion.platform.reconciliation.domain.repository;

import com.fashion.platform.reconciliation.domain.model.PosShift;
import com.fashion.platform.reconciliation.domain.model.PosShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PosShiftRepository extends JpaRepository<PosShift, UUID> {
    Optional<PosShift> findByShiftCode(String shiftCode);
    List<PosShift> findByLegalEntityIdOrderByOpenedAtDesc(UUID legalEntityId);
    Optional<PosShift> findFirstByLegalEntityIdAndStoreCodeAndStatus(UUID legalEntityId, String storeCode, PosShiftStatus status);
}

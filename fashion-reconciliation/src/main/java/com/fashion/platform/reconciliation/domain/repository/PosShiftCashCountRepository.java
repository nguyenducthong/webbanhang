package com.fashion.platform.reconciliation.domain.repository;

import com.fashion.platform.reconciliation.domain.model.PosShiftCashCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PosShiftCashCountRepository extends JpaRepository<PosShiftCashCount, UUID> {
    List<PosShiftCashCount> findByShiftIdOrderByDenominationDesc(UUID shiftId);
}

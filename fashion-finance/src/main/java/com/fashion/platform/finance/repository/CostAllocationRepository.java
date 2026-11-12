package com.fashion.platform.finance.repository;

import com.fashion.platform.finance.domain.entity.CostAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CostAllocationRepository extends JpaRepository<CostAllocation, UUID> {
    List<CostAllocation> findByCostId(UUID costId);
}


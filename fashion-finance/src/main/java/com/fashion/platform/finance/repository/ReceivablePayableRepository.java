package com.fashion.platform.finance.repository;

import com.fashion.platform.finance.domain.entity.PartyType;
import com.fashion.platform.finance.domain.entity.ReceivablePayable;
import com.fashion.platform.finance.domain.entity.ReceivableStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceivablePayableRepository extends JpaRepository<ReceivablePayable, UUID> {

    Optional<ReceivablePayable> findByLegalEntityIdAndSourceTypeAndSourceId(
            UUID legalEntityId, String sourceType, String sourceId);

    Page<ReceivablePayable> findByLegalEntityIdAndStatus(
            UUID legalEntityId, ReceivableStatus status, Pageable pageable);

    Page<ReceivablePayable> findByLegalEntityIdAndPartyType(
            UUID legalEntityId, PartyType partyType, Pageable pageable);
}


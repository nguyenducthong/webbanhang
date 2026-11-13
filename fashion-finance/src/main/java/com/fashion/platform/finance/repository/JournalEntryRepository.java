package com.fashion.platform.finance.repository;

import com.fashion.platform.finance.domain.entity.JournalEntry;
import com.fashion.platform.finance.domain.entity.JournalEntryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    Optional<JournalEntry> findByLegalEntityIdAndEntryNo(UUID legalEntityId, String entryNo);

    boolean existsByLegalEntityIdAndEntryNo(UUID legalEntityId, String entryNo);

    Page<JournalEntry> findByLegalEntityIdAndEntryStatus(UUID legalEntityId, JournalEntryStatus status, Pageable pageable);

    Page<JournalEntry> findByLegalEntityIdAndPeriodId(UUID legalEntityId, UUID periodId, Pageable pageable);

    Page<JournalEntry> findByLegalEntityId(UUID legalEntityId, Pageable pageable);
}

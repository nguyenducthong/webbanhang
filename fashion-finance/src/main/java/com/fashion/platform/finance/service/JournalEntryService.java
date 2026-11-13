package com.fashion.platform.finance.service;

import com.fashion.platform.common.security.SecurityContextHelper;
import com.fashion.platform.common.security.UserContext;
import com.fashion.platform.finance.domain.entity.*;
import com.fashion.platform.finance.repository.AccountingAccountRepository;
import com.fashion.platform.finance.repository.AccountingPeriodRepository;
import com.fashion.platform.finance.repository.JournalEntryRepository;
import com.fashion.platform.finance.repository.LegalEntityRepository;
import com.fashion.platform.finance.web.dto.JournalEntryDto;
import com.fashion.platform.infra.audit.AuditLogService;
import com.fashion.platform.infra.outbox.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JournalEntryService {

    private final LegalEntityRepository legalEntityRepository;
    private final AccountingPeriodRepository periodRepository;
    private final AccountingAccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final AuditLogService auditLogService;

    @Transactional
    public JournalEntryDto.Response createDraft(UUID legalEntityId, JournalEntryDto.CreateRequest request) {
        LegalEntity legalEntity = legalEntityRepository.findById(legalEntityId)
                .orElseThrow(() -> new NoSuchElementException("Legal entity not found: " + legalEntityId));

        AccountingPeriod period = periodRepository.findById(request.getPeriodId())
                .orElseThrow(() -> new NoSuchElementException("Accounting period not found: " + request.getPeriodId()));

        if (!period.getLegalEntity().getId().equals(legalEntityId)) {
            throw new IllegalArgumentException("Accounting period does not belong to legal entity");
        }

        if (journalEntryRepository.existsByLegalEntityIdAndEntryNo(legalEntityId, request.getEntryNo())) {
            throw new IllegalArgumentException("Journal entry number already exists: " + request.getEntryNo());
        }

        JournalEntry entry = JournalEntry.builder()
                .legalEntity(legalEntity)
                .period(period)
                .entryNo(request.getEntryNo().trim())
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .postingDate(request.getPostingDate())
                .entryStatus(JournalEntryStatus.DRAFT)
                .description(request.getDescription())
                .build();

        for (JournalEntryDto.LineRequest lineDto : request.getLines()) {
            AccountingAccount account = accountRepository.findById(lineDto.getAccountId())
                    .orElseThrow(() -> new NoSuchElementException("Account not found: " + lineDto.getAccountId()));

            JournalEntryLine line = JournalEntryLine.builder()
                    .account(account)
                    .debitAmount(lineDto.getDebitAmount() != null ? lineDto.getDebitAmount() : java.math.BigDecimal.ZERO)
                    .creditAmount(lineDto.getCreditAmount() != null ? lineDto.getCreditAmount() : java.math.BigDecimal.ZERO)
                    .currencyCode(lineDto.getCurrencyCode() != null ? lineDto.getCurrencyCode() : "VND")
                    .description(lineDto.getDescription())
                    .build();
            entry.addLine(line);
        }

        JournalEntry saved = journalEntryRepository.save(entry);
        log.info("Created draft journal entry: id={}, entryNo={}", saved.getId(), saved.getEntryNo());
        return JournalEntryDto.Response.from(saved);
    }

    @Transactional
    public JournalEntryDto.Response post(UUID legalEntityId, UUID entryId) {
        JournalEntry entry = findEntry(legalEntityId, entryId);
        UUID currentUserId = SecurityContextHelper.getContext().map(UserContext::getUserId).orElse(null);

        // Enforce balanced entry & period open rule
        entry.post(currentUserId);
        JournalEntry saved = journalEntryRepository.save(entry);

        outboxEventPublisher.publish("JOURNAL_ENTRY", saved.getId().toString(), "JOURNAL_ENTRY_POSTED",
                Map.of("id", saved.getId(), "entryNo", saved.getEntryNo(), "totalDebit", saved.getTotalDebit()));

        auditLogService.record(currentUserId, legalEntityId, null, "POST_JOURNAL_ENTRY",
                "JOURNAL_ENTRY", saved.getId().toString(), Map.of("entryNo", saved.getEntryNo()), null);

        log.info("Posted journal entry: id={}, entryNo={}, totalDebit={}", saved.getId(), saved.getEntryNo(), saved.getTotalDebit());
        return JournalEntryDto.Response.from(saved);
    }

    @Transactional
    public JournalEntryDto.Response reverse(UUID legalEntityId, UUID entryId, String reversalEntryNo, UUID currentPeriodId) {
        JournalEntry original = findEntry(legalEntityId, entryId);

        AccountingPeriod currentPeriod = periodRepository.findById(currentPeriodId)
                .orElseThrow(() -> new NoSuchElementException("Current accounting period not found: " + currentPeriodId));

        if (!currentPeriod.isOpen()) {
            throw new IllegalStateException("Target accounting period for reversal must be OPEN");
        }

        UUID currentUserId = SecurityContextHelper.getContext().map(UserContext::getUserId).orElse(null);

        JournalEntry reversal = original.createReversal(reversalEntryNo, currentPeriod, currentUserId);
        journalEntryRepository.save(original); // Updates original status to REVERSED
        JournalEntry savedReversal = journalEntryRepository.save(reversal);

        outboxEventPublisher.publish("JOURNAL_ENTRY", savedReversal.getId().toString(), "JOURNAL_ENTRY_REVERSED",
                Map.of("originalId", original.getId(), "reversalId", savedReversal.getId(), "entryNo", savedReversal.getEntryNo()));

        auditLogService.record(currentUserId, legalEntityId, null, "REVERSE_JOURNAL_ENTRY",
                "JOURNAL_ENTRY", original.getId().toString(), Map.of("reversalEntryNo", reversalEntryNo), null);

        log.info("Reversed journal entry id={} with reversal entry id={}", original.getId(), savedReversal.getId());
        return JournalEntryDto.Response.from(savedReversal);
    }

    @Transactional(readOnly = true)
    public Page<JournalEntryDto.Response> list(UUID legalEntityId, JournalEntryStatus status, Pageable pageable) {
        if (status != null) {
            return journalEntryRepository.findByLegalEntityIdAndEntryStatus(legalEntityId, status, pageable)
                    .map(JournalEntryDto.Response::from);
        }
        return journalEntryRepository.findByLegalEntityId(legalEntityId, pageable)
                .map(JournalEntryDto.Response::from);
    }

    private JournalEntry findEntry(UUID legalEntityId, UUID entryId) {
        JournalEntry entry = journalEntryRepository.findById(entryId)
                .orElseThrow(() -> new NoSuchElementException("Journal entry not found: " + entryId));
        if (!entry.getLegalEntity().getId().equals(legalEntityId)) {
            throw new IllegalArgumentException("Journal entry does not belong to legal entity: " + legalEntityId);
        }
        return entry;
    }
}

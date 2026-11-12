package com.fashion.platform.finance.service;

import com.fashion.platform.common.security.SecurityContextHelper;
import com.fashion.platform.common.security.UserContext;
import com.fashion.platform.finance.domain.entity.*;
import com.fashion.platform.finance.repository.FinanceTransactionRepository;
import com.fashion.platform.finance.repository.LegalEntityRepository;
import com.fashion.platform.finance.repository.ReceivablePayableRepository;
import com.fashion.platform.finance.web.dto.TransactionIngestRequest;
import com.fashion.platform.finance.web.dto.TransactionLineDto;
import com.fashion.platform.finance.web.dto.TransactionResponse;
import com.fashion.platform.infra.audit.AuditLogService;
import com.fashion.platform.infra.outbox.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceTransactionService {

    private final LegalEntityRepository legalEntityRepository;
    private final FinanceTransactionRepository transactionRepository;
    private final ReceivablePayableRepository receivablePayableRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final AuditLogService auditLogService;

    @Transactional
    public TransactionResponse ingest(UUID legalEntityId, TransactionIngestRequest request) {
        LegalEntity legalEntity = legalEntityRepository.findById(legalEntityId)
                .orElseThrow(() -> new NoSuchElementException("Legal entity not found: " + legalEntityId));

        // Idempotency check: if transaction already ingested, return existing record
        Optional<FinanceTransaction> existing = transactionRepository
                .findByLegalEntityIdAndSourceTypeAndSourceIdAndTransactionType(
                        legalEntityId, request.getSourceType(), request.getSourceId(), request.getTransactionType());

        if (existing.isPresent()) {
            log.info("Idempotent hit: Transaction already exists for source={}/{} type={}",
                    request.getSourceType(), request.getSourceId(), request.getTransactionType());
            return TransactionResponse.from(existing.get());
        }

        UUID currentUserId = SecurityContextHelper.getContext().map(UserContext::getUserId).orElse(null);

        FinanceTransaction tx = FinanceTransaction.builder()
                .legalEntity(legalEntity)
                .transactionCode(request.getTransactionCode())
                .transactionType(request.getTransactionType())
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .channel(request.getChannel())
                .amount(request.getAmount())
                .currencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "VND")
                .occurredAt(request.getOccurredAt() != null ? request.getOccurredAt() : Instant.now())
                .status(FinanceTransactionStatus.CONFIRMED)
                .description(request.getDescription())
                .metadata(request.getMetadata() != null ? request.getMetadata() : "{}")
                .createdBy(currentUserId)
                .build();

        if (request.getLines() != null) {
            for (TransactionLineDto lineDto : request.getLines()) {
                FinanceTransactionLine line = FinanceTransactionLine.builder()
                        .lineType(lineDto.getLineType())
                        .referenceType(lineDto.getReferenceType())
                        .referenceId(lineDto.getReferenceId())
                        .amount(lineDto.getAmount())
                        .description(lineDto.getDescription())
                        .metadata(lineDto.getMetadata() != null ? lineDto.getMetadata() : "{}")
                        .build();
                tx.addLine(line);
            }
        }

        FinanceTransaction saved = transactionRepository.save(tx);

        // Auto-create Receivable/Payable on specific transaction types
        handleReceivablePayableCreation(legalEntity, saved);

        // Outbox event
        outboxEventPublisher.publish("FINANCE_TRANSACTION", saved.getId().toString(), "FINANCE_TRANSACTION_INGESTED",
                Map.of("id", saved.getId(), "code", saved.getTransactionCode(), "amount", saved.getAmount(), "channel", saved.getChannel()));

        // Audit log
        auditLogService.record(currentUserId, legalEntityId, null, "INGEST_TRANSACTION",
                "FINANCE_TRANSACTION", saved.getId().toString(), Map.of("code", saved.getTransactionCode()), null);

        log.info("Ingested finance transaction: id={}, code={}, type={}", saved.getId(), saved.getTransactionCode(), saved.getTransactionType());
        return TransactionResponse.from(saved);
    }

    private void handleReceivablePayableCreation(LegalEntity legalEntity, FinanceTransaction tx) {
        if (tx.getTransactionType() == FinanceTransactionType.SALE_ORDER) {
            PartyType partyType;
            String partyId;

            switch (tx.getChannel()) {
                case SHOPEE -> {
                    partyType = PartyType.PLATFORM_SHOPEE;
                    partyId = "SHOPEE_VN";
                }
                case TIKTOK -> {
                    partyType = PartyType.PLATFORM_TIKTOK;
                    partyId = "TIKTOK_SHOP_VN";
                }
                case WEBSITE -> {
                    partyType = PartyType.CARRIER;
                    partyId = "DEFAULT_CARRIER";
                }
                default -> {
                    partyType = PartyType.CUSTOMER;
                    partyId = "OFFLINE_CUSTOMER";
                }
            }

            ReceivablePayable rp = ReceivablePayable.builder()
                    .legalEntity(legalEntity)
                    .entryType(EntryType.RECEIVABLE)
                    .partyType(partyType)
                    .partyId(partyId)
                    .sourceType(tx.getSourceType())
                    .sourceId(tx.getSourceId())
                    .originalAmount(tx.getAmount())
                    .settledAmount(java.math.BigDecimal.ZERO)
                    .currencyCode(tx.getCurrencyCode())
                    .status(ReceivableStatus.OPEN)
                    .build();

            receivablePayableRepository.save(rp);
            log.info("Auto-created receivable for order {}: partyType={}, amount={}",
                    tx.getSourceId(), partyType, tx.getAmount());
        }
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(UUID legalEntityId, TransactionChannel channel, Pageable pageable) {
        if (channel != null) {
            return transactionRepository.findByLegalEntityIdAndChannel(legalEntityId, channel, pageable)
                    .map(TransactionResponse::from);
        }
        return transactionRepository.findByLegalEntityId(legalEntityId, pageable)
                .map(TransactionResponse::from);
    }
}


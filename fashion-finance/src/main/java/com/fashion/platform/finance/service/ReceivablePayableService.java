package com.fashion.platform.finance.service;

import com.fashion.platform.finance.domain.entity.PartyType;
import com.fashion.platform.finance.domain.entity.ReceivablePayable;
import com.fashion.platform.finance.domain.entity.ReceivableStatus;
import com.fashion.platform.finance.repository.ReceivablePayableRepository;
import com.fashion.platform.finance.web.dto.ReceivablePayableResponse;
import com.fashion.platform.infra.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivablePayableService {

    private final ReceivablePayableRepository repository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<ReceivablePayableResponse> listByStatus(UUID legalEntityId, ReceivableStatus status, Pageable pageable) {
        if (status != null) {
            return repository.findByLegalEntityIdAndStatus(legalEntityId, status, pageable)
                    .map(ReceivablePayableResponse::from);
        }
        return repository.findAll(pageable).map(ReceivablePayableResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ReceivablePayableResponse> listByPartyType(UUID legalEntityId, PartyType partyType, Pageable pageable) {
        return repository.findByLegalEntityIdAndPartyType(legalEntityId, partyType, pageable)
                .map(ReceivablePayableResponse::from);
    }

    @Transactional
    public ReceivablePayableResponse settle(UUID legalEntityId, UUID id, BigDecimal amountToSettle) {
        ReceivablePayable rp = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Receivable/Payable not found: " + id));

        if (!rp.getLegalEntity().getId().equals(legalEntityId)) {
            throw new IllegalArgumentException("Record does not belong to legal entity: " + legalEntityId);
        }

        rp.settle(amountToSettle);
        ReceivablePayable saved = repository.save(rp);

        auditLogService.record(null, legalEntityId, null, "SETTLE_RECEIVABLE",
                "RECEIVABLE_PAYABLE", saved.getId().toString(),
                Map.of("settledAmount", amountToSettle, "newStatus", saved.getStatus()), null);

        log.info("Settled {} for RP id={}, newStatus={}", amountToSettle, saved.getId(), saved.getStatus());
        return ReceivablePayableResponse.from(saved);
    }
}


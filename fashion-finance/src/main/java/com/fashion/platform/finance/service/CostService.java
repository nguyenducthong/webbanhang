package com.fashion.platform.finance.service;

import com.fashion.platform.common.security.SecurityContextHelper;
import com.fashion.platform.common.security.UserContext;
import com.fashion.platform.finance.domain.entity.*;
import com.fashion.platform.finance.repository.CostAllocationRepository;
import com.fashion.platform.finance.repository.CostRepository;
import com.fashion.platform.finance.repository.LegalEntityRepository;
import com.fashion.platform.finance.web.dto.CostAllocationRequest;
import com.fashion.platform.finance.web.dto.CostCreateRequest;
import com.fashion.platform.finance.web.dto.CostResponse;
import com.fashion.platform.infra.audit.AuditLogService;
import com.fashion.platform.infra.outbox.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostService {

    private final LegalEntityRepository legalEntityRepository;
    private final CostRepository costRepository;
    private final CostAllocationRepository costAllocationRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final AuditLogService auditLogService;

    @Transactional
    public CostResponse create(UUID legalEntityId, CostCreateRequest request) {
        LegalEntity legalEntity = legalEntityRepository.findById(legalEntityId)
                .orElseThrow(() -> new NoSuchElementException("Legal entity not found: " + legalEntityId));

        if (costRepository.findByLegalEntityIdAndCostCode(legalEntityId, request.getCostCode()).isPresent()) {
            throw new IllegalArgumentException("Cost code already exists: " + request.getCostCode());
        }

        UUID currentUserId = SecurityContextHelper.getContext().map(UserContext::getUserId).orElse(null);

        Cost cost = Cost.builder()
                .legalEntity(legalEntity)
                .costCode(request.getCostCode())
                .costCategory(request.getCostCategory())
                .costType(request.getCostType())
                .amount(request.getAmount())
                .currencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "VND")
                .incurredAt(request.getIncurredAt() != null ? request.getIncurredAt() : Instant.now())
                .vendorId(request.getVendorId())
                .referenceNo(request.getReferenceNo())
                .metadata(request.getMetadata() != null ? request.getMetadata() : "{}")
                .status(CostStatus.RECORDED)
                .createdBy(currentUserId)
                .build();

        Cost saved = costRepository.save(cost);

        outboxEventPublisher.publish("COST", saved.getId().toString(), "COST_RECORDED",
                Map.of("id", saved.getId(), "code", saved.getCostCode(), "amount", saved.getAmount()));

        auditLogService.record(currentUserId, legalEntityId, null, "CREATE_COST",
                "COST", saved.getId().toString(), Map.of("code", saved.getCostCode()), null);

        log.info("Recorded cost: id={}, code={}, amount={}", saved.getId(), saved.getCostCode(), saved.getAmount());
        return CostResponse.from(saved);
    }

    @Transactional
    public CostResponse allocate(UUID legalEntityId, UUID costId, CostAllocationRequest request) {
        Cost cost = costRepository.findById(costId)
                .orElseThrow(() -> new NoSuchElementException("Cost record not found: " + costId));

        if (!cost.getLegalEntity().getId().equals(legalEntityId)) {
            throw new IllegalArgumentException("Cost does not belong to legal entity: " + legalEntityId);
        }

        BigDecimal ratio = request.getAllocatedAmount().divide(cost.getAmount(), 6, RoundingMode.HALF_UP);

        CostAllocation allocation = CostAllocation.builder()
                .allocationTargetType(request.getAllocationTargetType())
                .targetId(request.getTargetId())
                .allocatedAmount(request.getAllocatedAmount())
                .allocationRatio(ratio)
                .build();

        cost.addAllocation(allocation);
        Cost saved = costRepository.save(cost);

        outboxEventPublisher.publish("COST", saved.getId().toString(), "COST_ALLOCATED",
                Map.of("costId", saved.getId(), "targetType", request.getAllocationTargetType(), "targetId", request.getTargetId(), "amount", request.getAllocatedAmount()));

        UUID currentUserId = SecurityContextHelper.getContext().map(UserContext::getUserId).orElse(null);
        auditLogService.record(currentUserId, legalEntityId, null, "ALLOCATE_COST",
                "COST", saved.getId().toString(), Map.of("allocatedAmount", request.getAllocatedAmount()), null);

        log.info("Allocated cost id={}, amount={}, target={}/{}", saved.getId(), request.getAllocatedAmount(), request.getAllocationTargetType(), request.getTargetId());
        return CostResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<CostResponse> list(UUID legalEntityId, Pageable pageable) {
        return costRepository.findByLegalEntityId(legalEntityId, pageable).map(CostResponse::from);
    }
}


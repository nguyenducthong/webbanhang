package com.fashion.platform.finance.service;

import com.fashion.platform.common.security.SecurityContextHelper;
import com.fashion.platform.common.security.UserContext;
import com.fashion.platform.finance.domain.entity.LegalEntity;
import com.fashion.platform.finance.domain.entity.LegalEntityStatus;
import com.fashion.platform.finance.domain.entity.LegalEntityType;
import com.fashion.platform.finance.repository.LegalEntityRepository;
import com.fashion.platform.finance.web.dto.LegalEntityCreateRequest;
import com.fashion.platform.finance.web.dto.LegalEntityResponse;
import com.fashion.platform.finance.web.dto.LegalEntityUpdateRequest;
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
public class LegalEntityService {

    private final LegalEntityRepository legalEntityRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final AuditLogService auditLogService;

    @Transactional
    public LegalEntityResponse create(LegalEntityCreateRequest request) {
        if (legalEntityRepository.existsByEntityCode(request.getEntityCode())) {
            throw new IllegalArgumentException("Legal entity code already exists: " + request.getEntityCode());
        }

        UUID currentUserId = SecurityContextHelper.getContext().map(UserContext::getUserId).orElse(null);

        LegalEntity entity = LegalEntity.builder()
                .entityCode(request.getEntityCode().trim().toUpperCase())
                .entityName(request.getEntityName().trim())
                .entityType(request.getEntityType() != null ? request.getEntityType() : LegalEntityType.UNDECIDED)
                .taxCode(request.getTaxCode())
                .businessRegistrationNo(request.getBusinessRegistrationNo())
                .address(request.getAddress())
                .countryCode(request.getCountryCode() != null ? request.getCountryCode() : "VN")
                .status(LegalEntityStatus.DRAFT)
                .createdBy(currentUserId)
                .build();

        LegalEntity saved = legalEntityRepository.save(entity);

        // Outbox event
        outboxEventPublisher.publish("LEGAL_ENTITY", saved.getId().toString(), "LEGAL_ENTITY_CREATED",
                Map.of("id", saved.getId(), "code", saved.getEntityCode(), "name", saved.getEntityName()));

        // Audit log
        auditLogService.record(currentUserId, saved.getId(), null, "CREATE",
                "LEGAL_ENTITY", saved.getId().toString(), saved, null);

        log.info("Created legal entity: id={}, code={}", saved.getId(), saved.getEntityCode());
        return LegalEntityResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public LegalEntityResponse getById(UUID id) {
        return LegalEntityResponse.from(findEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<LegalEntityResponse> listAll(LegalEntityStatus status, Pageable pageable) {
        if (status != null) {
            return legalEntityRepository.findByStatus(status, pageable).map(LegalEntityResponse::from);
        }
        return legalEntityRepository.findAll(pageable).map(LegalEntityResponse::from);
    }

    @Transactional
    public LegalEntityResponse update(UUID id, LegalEntityUpdateRequest request) {
        LegalEntity entity = findEntity(id);
        UUID currentUserId = SecurityContextHelper.getContext().map(UserContext::getUserId).orElse(null);

        entity.updateDetails(
                request.getEntityName(),
                request.getEntityType(),
                request.getTaxCode(),
                request.getBusinessRegistrationNo(),
                request.getAddress(),
                currentUserId
        );

        LegalEntity updated = legalEntityRepository.save(entity);

        outboxEventPublisher.publish("LEGAL_ENTITY", updated.getId().toString(), "LEGAL_ENTITY_UPDATED",
                Map.of("id", updated.getId(), "name", updated.getEntityName()));

        auditLogService.record(currentUserId, updated.getId(), null, "UPDATE",
                "LEGAL_ENTITY", updated.getId().toString(), updated, null);

        return LegalEntityResponse.from(updated);
    }

    @Transactional
    public LegalEntityResponse changeStatus(UUID id, LegalEntityStatus newStatus) {
        LegalEntity entity = findEntity(id);
        UUID currentUserId = SecurityContextHelper.getContext().map(UserContext::getUserId).orElse(null);

        if (newStatus == LegalEntityStatus.ACTIVE) {
            entity.activate(currentUserId);
        } else if (newStatus == LegalEntityStatus.INACTIVE) {
            entity.deactivate(currentUserId);
        }

        LegalEntity updated = legalEntityRepository.save(entity);

        auditLogService.record(currentUserId, updated.getId(), null, "STATUS_CHANGE",
                "LEGAL_ENTITY", updated.getId().toString(), Map.of("newStatus", newStatus), null);

        return LegalEntityResponse.from(updated);
    }

    private LegalEntity findEntity(UUID id) {
        return legalEntityRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Legal entity not found with id: " + id));
    }
}

package com.fashion.platform.finance.service;

import com.fashion.platform.common.security.SecurityContextHelper;
import com.fashion.platform.common.security.UserContext;
import com.fashion.platform.finance.domain.entity.LegalEntity;
import com.fashion.platform.finance.domain.entity.TaxConfiguration;
import com.fashion.platform.finance.domain.entity.TaxType;
import com.fashion.platform.finance.repository.LegalEntityRepository;
import com.fashion.platform.finance.repository.TaxConfigurationRepository;
import com.fashion.platform.finance.web.dto.TaxConfigCreateRequest;
import com.fashion.platform.finance.web.dto.TaxConfigResponse;
import com.fashion.platform.infra.audit.AuditLogService;
import com.fashion.platform.infra.outbox.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxConfigurationService {

    private final LegalEntityRepository legalEntityRepository;
    private final TaxConfigurationRepository taxConfigurationRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final AuditLogService auditLogService;

    @Transactional
    public TaxConfigResponse create(UUID legalEntityId, TaxConfigCreateRequest request) {
        LegalEntity legalEntity = legalEntityRepository.findById(legalEntityId)
                .orElseThrow(() -> new NoSuchElementException("Legal entity not found: " + legalEntityId));

        if (request.getEffectiveTo() != null && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
            throw new IllegalArgumentException("effectiveTo must not be earlier than effectiveFrom");
        }

        UUID currentUserId = SecurityContextHelper.getContext().map(UserContext::getUserId).orElse(null);

        TaxConfiguration tc = TaxConfiguration.builder()
                .legalEntity(legalEntity)
                .taxType(request.getTaxType())
                .taxMethod(request.getTaxMethod())
                .configJson(request.getConfigJson() != null ? request.getConfigJson() : "{}")
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .createdBy(currentUserId)
                .build();

        TaxConfiguration saved = taxConfigurationRepository.save(tc);

        auditLogService.record(currentUserId, legalEntityId, null, "CREATE",
                "TAX_CONFIGURATION", saved.getId().toString(), saved, null);

        log.info("Created tax config: id={}, entityId={}, type={}", saved.getId(), legalEntityId, saved.getTaxType());
        return TaxConfigResponse.from(saved);
    }

    @Transactional
    public TaxConfigResponse approve(UUID legalEntityId, UUID configId) {
        TaxConfiguration tc = taxConfigurationRepository.findById(configId)
                .orElseThrow(() -> new NoSuchElementException("Tax configuration not found: " + configId));

        if (!tc.getLegalEntity().getId().equals(legalEntityId)) {
            throw new IllegalArgumentException("Tax configuration does not belong to legal entity: " + legalEntityId);
        }

        UserContext user = SecurityContextHelper.getContext()
                .orElseThrow(() -> new SecurityException("Authentication context required for approval"));

        if (!user.hasRole("ROLE_FINANCE_MANAGER") && !user.hasRole("ROLE_SUPER_ADMIN")) {
            throw new SecurityException("Only ROLE_FINANCE_MANAGER or ROLE_SUPER_ADMIN can approve tax configurations");
        }

        // Maker-Checker enforcement
        tc.approve(user.getUserId());
        TaxConfiguration saved = taxConfigurationRepository.save(tc);

        outboxEventPublisher.publish("TAX_CONFIGURATION", saved.getId().toString(), "TAX_CONFIG_APPROVED",
                Map.of("id", saved.getId(), "entityId", legalEntityId, "taxType", saved.getTaxType()));

        auditLogService.record(user.getUserId(), legalEntityId, null, "APPROVE",
                "TAX_CONFIGURATION", saved.getId().toString(), Map.of("approvedBy", user.getUserId()), null);

        log.info("Approved tax config: id={}, approver={}", saved.getId(), user.getUserId());
        return TaxConfigResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TaxConfigResponse> listByLegalEntity(UUID legalEntityId) {
        return taxConfigurationRepository.findByLegalEntityId(legalEntityId)
                .stream()
                .map(TaxConfigResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaxConfigResponse findEffectiveTaxConfig(UUID legalEntityId, TaxType taxType, LocalDate date) {
        List<TaxConfiguration> configs = taxConfigurationRepository.findEffectiveTaxConfigurations(
                legalEntityId, taxType, date != null ? date : LocalDate.now());
        if (configs.isEmpty()) {
            return null;
        }
        return TaxConfigResponse.from(configs.get(0));
    }
}


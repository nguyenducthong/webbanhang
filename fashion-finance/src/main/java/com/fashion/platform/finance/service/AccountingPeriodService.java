package com.fashion.platform.finance.service;

import com.fashion.platform.common.security.SecurityContextHelper;
import com.fashion.platform.common.security.UserContext;
import com.fashion.platform.finance.domain.entity.AccountingPeriod;
import com.fashion.platform.finance.domain.entity.LegalEntity;
import com.fashion.platform.finance.repository.AccountingPeriodRepository;
import com.fashion.platform.finance.repository.LegalEntityRepository;
import com.fashion.platform.finance.web.dto.AccountingPeriodDto;
import com.fashion.platform.infra.audit.AuditLogService;
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
public class AccountingPeriodService {

    private final LegalEntityRepository legalEntityRepository;
    private final AccountingPeriodRepository periodRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public AccountingPeriodDto.Response create(UUID legalEntityId, AccountingPeriodDto.CreateRequest request) {
        LegalEntity legalEntity = legalEntityRepository.findById(legalEntityId)
                .orElseThrow(() -> new NoSuchElementException("Legal entity not found: " + legalEntityId));

        if (periodRepository.existsByLegalEntityIdAndPeriodCode(legalEntityId, request.getPeriodCode())) {
            throw new IllegalArgumentException("Period code already exists: " + request.getPeriodCode());
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be earlier than start date");
        }

        AccountingPeriod period = AccountingPeriod.builder()
                .legalEntity(legalEntity)
                .periodCode(request.getPeriodCode().trim().toUpperCase())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        AccountingPeriod saved = periodRepository.save(period);
        log.info("Created accounting period: id={}, code={}", saved.getId(), saved.getPeriodCode());
        return AccountingPeriodDto.Response.from(saved);
    }

    @Transactional
    public AccountingPeriodDto.Response closePeriod(UUID legalEntityId, UUID periodId) {
        AccountingPeriod period = findPeriod(legalEntityId, periodId);
        UUID currentUserId = SecurityContextHelper.getContext().map(UserContext::getUserId).orElse(null);

        period.close(currentUserId);
        AccountingPeriod saved = periodRepository.save(period);

        auditLogService.record(currentUserId, legalEntityId, null, "CLOSE_PERIOD",
                "ACCOUNTING_PERIOD", saved.getId().toString(), Map.of("periodCode", saved.getPeriodCode()), null);

        log.info("Closed accounting period: id={}, code={}", saved.getId(), saved.getPeriodCode());
        return AccountingPeriodDto.Response.from(saved);
    }

    @Transactional
    public AccountingPeriodDto.Response reopenPeriod(UUID legalEntityId, UUID periodId) {
        AccountingPeriod period = findPeriod(legalEntityId, periodId);

        UserContext user = SecurityContextHelper.getContext()
                .orElseThrow(() -> new SecurityException("Authentication context required to reopen period"));

        if (!user.hasRole("ROLE_FINANCE_MANAGER") && !user.hasRole("ROLE_SUPER_ADMIN")) {
            throw new SecurityException("Only ROLE_FINANCE_MANAGER or ROLE_SUPER_ADMIN can reopen a closed period");
        }

        period.reopen(user.getUserId());
        AccountingPeriod saved = periodRepository.save(period);

        auditLogService.record(user.getUserId(), legalEntityId, null, "REOPEN_PERIOD",
                "ACCOUNTING_PERIOD", saved.getId().toString(), Map.of("periodCode", saved.getPeriodCode()), null);

        log.info("Reopened accounting period: id={}, code={}, by user={}", saved.getId(), saved.getPeriodCode(), user.getUserId());
        return AccountingPeriodDto.Response.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<AccountingPeriodDto.Response> list(UUID legalEntityId, Pageable pageable) {
        return periodRepository.findByLegalEntityId(legalEntityId, pageable)
                .map(AccountingPeriodDto.Response::from);
    }

    private AccountingPeriod findPeriod(UUID legalEntityId, UUID periodId) {
        AccountingPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new NoSuchElementException("Accounting period not found: " + periodId));
        if (!period.getLegalEntity().getId().equals(legalEntityId)) {
            throw new IllegalArgumentException("Period does not belong to legal entity: " + legalEntityId);
        }
        return period;
    }
}

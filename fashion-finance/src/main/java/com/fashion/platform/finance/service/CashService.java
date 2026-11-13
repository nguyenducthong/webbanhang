package com.fashion.platform.finance.service;

import com.fashion.platform.common.security.SecurityContextHelper;
import com.fashion.platform.common.security.UserContext;
import com.fashion.platform.finance.domain.entity.CashAccount;
import com.fashion.platform.finance.domain.entity.CashTransaction;
import com.fashion.platform.finance.domain.entity.LegalEntity;
import com.fashion.platform.finance.repository.CashAccountRepository;
import com.fashion.platform.finance.repository.CashTransactionRepository;
import com.fashion.platform.finance.repository.LegalEntityRepository;
import com.fashion.platform.finance.web.dto.CashDto;
import com.fashion.platform.infra.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashService {

    private final LegalEntityRepository legalEntityRepository;
    private final CashAccountRepository cashAccountRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public CashDto.AccountResponse createAccount(UUID legalEntityId, CashDto.AccountCreateRequest request) {
        LegalEntity legalEntity = legalEntityRepository.findById(legalEntityId)
                .orElseThrow(() -> new NoSuchElementException("Legal entity not found: " + legalEntityId));

        CashAccount account = CashAccount.builder()
                .legalEntity(legalEntity)
                .accountName(request.getAccountName())
                .accountType(request.getAccountType())
                .bankName(request.getBankName())
                .accountNumberMasked(request.getAccountNumberMasked())
                .currencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "VND")
                .status("ACTIVE")
                .build();

        CashAccount saved = cashAccountRepository.save(account);
        log.info("Created cash account: id={}, name={}", saved.getId(), saved.getAccountName());
        return CashDto.AccountResponse.from(saved);
    }

    @Transactional
    public CashDto.TransactionResponse recordTransaction(UUID legalEntityId, UUID accountId, CashDto.TransactionCreateRequest request) {
        CashAccount account = cashAccountRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Cash account not found: " + accountId));

        if (!account.getLegalEntity().getId().equals(legalEntityId)) {
            throw new IllegalArgumentException("Cash account does not belong to legal entity");
        }

        UUID currentUserId = SecurityContextHelper.getContext().map(UserContext::getUserId).orElse(null);

        CashTransaction tx = CashTransaction.builder()
                .legalEntity(account.getLegalEntity())
                .cashAccount(account)
                .transactionType(request.getTransactionType())
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .amount(request.getAmount())
                .occurredAt(request.getOccurredAt() != null ? request.getOccurredAt() : Instant.now())
                .description(request.getDescription())
                .metadata(request.getMetadata() != null ? request.getMetadata() : "{}")
                .status("CONFIRMED")
                .createdBy(currentUserId)
                .build();

        CashTransaction saved = cashTransactionRepository.save(tx);

        auditLogService.record(currentUserId, legalEntityId, null, "CASH_TRANSACTION",
                "CASH_ACCOUNT", accountId.toString(), Map.of("type", tx.getTransactionType(), "amount", tx.getAmount()), null);

        log.info("Recorded cash transaction: id={}, type={}, amount={}", saved.getId(), saved.getTransactionType(), saved.getAmount());
        return CashDto.TransactionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CashDto.AccountResponse> listActiveAccounts(UUID legalEntityId) {
        return cashAccountRepository.findByLegalEntityIdAndStatus(legalEntityId, "ACTIVE").stream()
                .map(CashDto.AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CashDto.TransactionResponse> listTransactions(UUID cashAccountId, Pageable pageable) {
        return cashTransactionRepository.findByCashAccountId(cashAccountId, pageable)
                .map(CashDto.TransactionResponse::from);
    }
}

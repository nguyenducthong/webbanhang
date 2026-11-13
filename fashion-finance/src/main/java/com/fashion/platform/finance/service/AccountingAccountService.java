package com.fashion.platform.finance.service;

import com.fashion.platform.finance.domain.entity.AccountType;
import com.fashion.platform.finance.domain.entity.AccountingAccount;
import com.fashion.platform.finance.domain.entity.LegalEntity;
import com.fashion.platform.finance.repository.AccountingAccountRepository;
import com.fashion.platform.finance.repository.LegalEntityRepository;
import com.fashion.platform.finance.web.dto.AccountingAccountDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountingAccountService {

    private final LegalEntityRepository legalEntityRepository;
    private final AccountingAccountRepository accountRepository;

    @Transactional
    public AccountingAccountDto.Response create(UUID legalEntityId, AccountingAccountDto.CreateRequest request) {
        LegalEntity legalEntity = legalEntityRepository.findById(legalEntityId)
                .orElseThrow(() -> new NoSuchElementException("Legal entity not found: " + legalEntityId));

        if (accountRepository.existsByLegalEntityIdAndAccountCode(legalEntityId, request.getAccountCode())) {
            throw new IllegalArgumentException("Account code already exists: " + request.getAccountCode());
        }

        AccountingAccount parent = null;
        if (request.getParentId() != null) {
            parent = accountRepository.findById(request.getParentId())
                    .orElseThrow(() -> new NoSuchElementException("Parent account not found: " + request.getParentId()));
        }

        AccountingAccount account = AccountingAccount.builder()
                .legalEntity(legalEntity)
                .accountCode(request.getAccountCode().trim())
                .accountName(request.getAccountName().trim())
                .accountType(request.getAccountType())
                .parent(parent)
                .isActive(true)
                .build();

        AccountingAccount saved = accountRepository.save(account);
        log.info("Created accounting account: id={}, code={}, name={}", saved.getId(), saved.getAccountCode(), saved.getAccountName());
        return AccountingAccountDto.Response.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountingAccountDto.Response> listActiveAccounts(UUID legalEntityId) {
        return accountRepository.findByLegalEntityIdAndIsActiveTrue(legalEntityId).stream()
                .map(AccountingAccountDto.Response::from)
                .toList();
    }

    @Transactional
    public void seedStandardChartOfAccounts(UUID legalEntityId) {
        LegalEntity legalEntity = legalEntityRepository.findById(legalEntityId)
                .orElseThrow(() -> new NoSuchElementException("Legal entity not found: " + legalEntityId));

        createIfNotExists(legalEntity, "111", "Tiền mặt", AccountType.ASSET);
        createIfNotExists(legalEntity, "112", "Tiền gửi ngân hàng", AccountType.ASSET);
        createIfNotExists(legalEntity, "131", "Phải thu của khách hàng", AccountType.ASSET);
        createIfNotExists(legalEntity, "156", "Hàng hóa tồn kho", AccountType.ASSET);
        createIfNotExists(legalEntity, "331", "Phải trả cho người bán", AccountType.LIABILITY);
        createIfNotExists(legalEntity, "3331", "Thuế GTGT phải nộp", AccountType.LIABILITY);
        createIfNotExists(legalEntity, "411", "Vốn đầu tư của chủ sở hữu", AccountType.EQUITY);
        createIfNotExists(legalEntity, "511", "Doanh thu bán hàng và cung cấp dịch vụ", AccountType.REVENUE);
        createIfNotExists(legalEntity, "632", "Giá vốn hàng bán", AccountType.EXPENSE);
        createIfNotExists(legalEntity, "641", "Chi phí bán hàng", AccountType.EXPENSE);
        createIfNotExists(legalEntity, "642", "Chi phí quản lý doanh nghiệp", AccountType.EXPENSE);
    }

    private void createIfNotExists(LegalEntity entity, String code, String name, AccountType type) {
        if (!accountRepository.existsByLegalEntityIdAndAccountCode(entity.getId(), code)) {
            AccountingAccount account = AccountingAccount.builder()
                    .legalEntity(entity)
                    .accountCode(code)
                    .accountName(name)
                    .accountType(type)
                    .isActive(true)
                    .build();
            accountRepository.save(account);
        }
    }
}

package com.fashion.platform.loader;

import com.fashion.platform.einvoice.domain.model.InvoiceProvider;
import com.fashion.platform.einvoice.domain.model.InvoiceProviderAccount;
import com.fashion.platform.einvoice.domain.model.InvoiceTemplate;
import com.fashion.platform.einvoice.domain.model.InvoiceType;
import com.fashion.platform.einvoice.domain.repository.InvoiceProviderAccountRepository;
import com.fashion.platform.einvoice.domain.repository.InvoiceProviderRepository;
import com.fashion.platform.einvoice.domain.repository.InvoiceTemplateRepository;
import com.fashion.platform.finance.domain.entity.*;
import com.fashion.platform.finance.repository.AccountingPeriodRepository;
import com.fashion.platform.finance.repository.CashAccountRepository;
import com.fashion.platform.finance.repository.LegalEntityRepository;
import com.fashion.platform.finance.service.AccountingAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataLoader implements ApplicationRunner {

    private final LegalEntityRepository legalEntityRepository;
    private final AccountingPeriodRepository periodRepository;
    private final AccountingAccountService accountService;
    private final CashAccountRepository cashAccountRepository;
    private final InvoiceProviderRepository invoiceProviderRepository;
    private final InvoiceProviderAccountRepository invoiceProviderAccountRepository;
    private final InvoiceTemplateRepository invoiceTemplateRepository;

    @Override
    public void run(ApplicationArguments args) {
        UUID demoEntityId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        LegalEntity demo = legalEntityRepository.findByEntityCode("LE-DEMO-FASHION")
                .orElseGet(() -> {
                    LegalEntity newEntity = LegalEntity.builder()
                            .id(demoEntityId)
                            .entityCode("LE-DEMO-FASHION")
                            .entityName("Fashion Demo Store VN")
                            .entityType(LegalEntityType.COMPANY)
                            .taxCode("0101234567")
                            .address("123 Le Loi, District 1, Ho Chi Minh City")
                            .countryCode("VN")
                            .status(LegalEntityStatus.ACTIVE)
                            .build();
                    return legalEntityRepository.save(newEntity);
                });

        log.info("Verified DEMO Legal Entity: LE-DEMO-FASHION");

        // Seed Accounting Period 2026-M11
        if (!periodRepository.existsByLegalEntityIdAndPeriodCode(demo.getId(), "2026-M11")) {
            AccountingPeriod period = AccountingPeriod.builder()
                    .legalEntity(demo)
                    .periodCode("2026-M11")
                    .startDate(LocalDate.of(2026, 11, 1))
                    .endDate(LocalDate.of(2026, 11, 30))
                    .status(PeriodStatus.OPEN)
                    .build();
            periodRepository.save(period);
            log.info("Initialized DEMO Accounting Period: 2026-M11");
        }

        // Seed Chart of Accounts (COA)
        accountService.seedStandardChartOfAccounts(demo.getId());
        log.info("Initialized DEMO Chart of Accounts");

        // Seed Cash Accounts
        if (cashAccountRepository.findByLegalEntityIdAndStatus(demo.getId(), "ACTIVE").isEmpty()) {
            CashAccount cashDrawer = CashAccount.builder()
                    .legalEntity(demo)
                    .accountName("Quỹ Tiền mặt Cửa hàng chính")
                    .accountType(CashAccountType.CASH_DRAWER)
                    .currencyCode("VND")
                    .status("ACTIVE")
                    .build();
            cashAccountRepository.save(cashDrawer);

            CashAccount bankAcc = CashAccount.builder()
                    .legalEntity(demo)
                    .accountName("Tài khoản Ngân hàng VCB")
                    .accountType(CashAccountType.BANK_ACCOUNT)
                    .bankName("Vietcombank")
                    .accountNumberMasked("******8888")
                    .currencyCode("VND")
                    .status("ACTIVE")
                    .build();
            cashAccountRepository.save(bankAcc);
            log.info("Initialized DEMO Cash and Bank Accounts");
        }

        // Seed E-Invoice Provider & Account & Template
        InvoiceProvider fakeProvider = invoiceProviderRepository.findByProviderCode("FAKE")
                .orElseGet(() -> {
                    InvoiceProvider p = InvoiceProvider.builder()
                            .providerCode("FAKE")
                            .name("Mock Electronic Invoice Provider")
                            .active(true)
                            .build();
                    return invoiceProviderRepository.save(p);
                });

        if (invoiceProviderAccountRepository.findByLegalEntityIdAndProviderId(demo.getId(), fakeProvider.getId()).isEmpty()) {
            InvoiceProviderAccount account = InvoiceProviderAccount.builder()
                    .legalEntity(demo)
                    .provider(fakeProvider)
                    .accountUsername("demo_einvoice_user")
                    .apiEndpoint("https://einvoice-api.fake-provider.vn/v1")
                    .active(true)
                    .build();
            invoiceProviderAccountRepository.save(account);
            log.info("Initialized DEMO E-Invoice Provider Account");
        }

        // Seed E-Invoice Provider VNPT
        InvoiceProvider vnptProvider = invoiceProviderRepository.findByProviderCode("VNPT")
                .orElseGet(() -> {
                    InvoiceProvider p = InvoiceProvider.builder()
                            .providerCode("VNPT")
                            .name("VNPT Electronic Invoice Gateway (Nghị định 123)")
                            .active(true)
                            .build();
                    return invoiceProviderRepository.save(p);
                });

        if (invoiceProviderAccountRepository.findByLegalEntityIdAndProviderId(demo.getId(), vnptProvider.getId()).isEmpty()) {
            InvoiceProviderAccount vnptAccount = InvoiceProviderAccount.builder()
                    .legalEntity(demo)
                    .provider(vnptProvider)
                    .accountUsername("fashion_vnpt_merchant")
                    .apiEndpoint("https://api-einvoice.vnpt.vn/v2")
                    .webhookSecret("sec_webhook_default_key_2026")
                    .active(true)
                    .build();
            invoiceProviderAccountRepository.save(vnptAccount);
            log.info("Initialized DEMO VNPT E-Invoice Provider Account");
        }

        if (invoiceTemplateRepository.findFirstByLegalEntityIdAndInvoiceTypeAndActiveTrue(demo.getId(), InvoiceType.VAT_INVOICE).isEmpty()) {
            InvoiceTemplate template = InvoiceTemplate.builder()
                    .legalEntity(demo)
                    .templateCode("1/001")
                    .invoiceSeries("C26TDD")
                    .invoiceType(InvoiceType.VAT_INVOICE)
                    .active(true)
                    .build();
            invoiceTemplateRepository.save(template);
            log.info("Initialized DEMO E-Invoice Template: 1/001 - C26TDD");
        }
    }
}


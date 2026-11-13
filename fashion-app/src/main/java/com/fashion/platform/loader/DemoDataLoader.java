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
import com.fashion.platform.finance.repository.FinanceTransactionRepository;
import com.fashion.platform.finance.repository.LegalEntityRepository;
import com.fashion.platform.finance.service.AccountingAccountService;
import com.fashion.platform.reconciliation.domain.model.*;
import com.fashion.platform.reconciliation.domain.repository.ReconciliationBatchRepository;
import com.fashion.platform.reconciliation.domain.repository.ReconciliationItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
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
    private final FinanceTransactionRepository financeTransactionRepository;
    private final ReconciliationBatchRepository reconciliationBatchRepository;
    private final ReconciliationItemRepository reconciliationItemRepository;

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

        // Seed Sample Finance Transactions for Shopee Reconciliation
        FinanceTransaction tx1 = financeTransactionRepository.findFirstByLegalEntityIdAndSourceId(demo.getId(), "SP-20261113-001")
                .orElseGet(() -> {
                    FinanceTransaction tx = FinanceTransaction.builder()
                            .legalEntity(demo)
                            .transactionCode("TX-SHOPEE-001")
                            .transactionType(FinanceTransactionType.SALE_ORDER)
                            .sourceType("SHOPEE_ORDER")
                            .sourceId("SP-20261113-001")
                            .channel(TransactionChannel.SHOPEE)
                            .amount(new BigDecimal("450000.0000"))
                            .currencyCode("VND")
                            .occurredAt(Instant.now().minusSeconds(86400))
                            .status(FinanceTransactionStatus.CONFIRMED)
                            .description("Đơn hàng Shopee Áo Thun Basic Cotton SP-20261113-001")
                            .metadata("{\"buyer\":\"nguyen_van_a\"}")
                            .build();
                    return financeTransactionRepository.save(tx);
                });

        FinanceTransaction tx2 = financeTransactionRepository.findFirstByLegalEntityIdAndSourceId(demo.getId(), "SP-20261113-002")
                .orElseGet(() -> {
                    FinanceTransaction tx = FinanceTransaction.builder()
                            .legalEntity(demo)
                            .transactionCode("TX-SHOPEE-002")
                            .transactionType(FinanceTransactionType.SALE_ORDER)
                            .sourceType("SHOPEE_ORDER")
                            .sourceId("SP-20261113-002")
                            .channel(TransactionChannel.SHOPEE)
                            .amount(new BigDecimal("620000.0000"))
                            .currencyCode("VND")
                            .occurredAt(Instant.now().minusSeconds(43200))
                            .status(FinanceTransactionStatus.CONFIRMED)
                            .description("Đơn hàng Shopee Quần Jean Regular Fit SP-20261113-002")
                            .metadata("{\"buyer\":\"tran_thi_b\"}")
                            .build();
                    return financeTransactionRepository.save(tx);
                });

        // Seed Sample Reconciliation Batch (Shopee 13/11/2026)
        if (reconciliationBatchRepository.findByLegalEntityIdOrderByCreatedAtDesc(demo.getId()).isEmpty()) {
            ReconciliationBatch batch = ReconciliationBatch.builder()
                    .legalEntity(demo)
                    .batchCode("REC-SP-20261113-01")
                    .sourceType(ReconciliationSourceType.SHOPEE)
                    .status(BatchStatus.DISCREPANCY_FOUND)
                    .fileName("Shopee_Settlement_20261113.csv")
                    .totalRecords(3)
                    .matchedRecords(1)
                    .discrepancyRecords(2)
                    .totalAmountExternal(new BigDecimal("1335000.0000"))
                    .totalAmountInternal(new BigDecimal("1070000.0000"))
                    .notes("Đợt đối soát Shopee tự động hàng ngày. Phát hiện 1 đơn lệch phí sàn và 1 đơn chưa ghi nhận nội bộ.")
                    .build();
            reconciliationBatchRepository.save(batch);

            // Item 1: Khớp hoàn toàn
            ReconciliationItem item1 = ReconciliationItem.builder()
                    .batch(batch)
                    .externalOrderId("SP-20261113-001")
                    .internalOrderId(tx1.getSourceId())
                    .externalAmount(new BigDecimal("450000.0000"))
                    .internalAmount(new BigDecimal("450000.0000"))
                    .discrepancyAmount(BigDecimal.ZERO)
                    .feeExternal(new BigDecimal("25000.0000"))
                    .feeInternal(new BigDecimal("25000.0000"))
                    .matchStatus(MatchStatus.EXACT_MATCH)
                    .resolutionStatus(ResolutionStatus.RESOLVED)
                    .resolvedBy("SYSTEM")
                    .resolutionNotes("Tự động khớp chính xác 100%")
                    .build();

            // Item 2: Lệch phí sàn
            ReconciliationItem item2 = ReconciliationItem.builder()
                    .batch(batch)
                    .externalOrderId("SP-20261113-002")
                    .internalOrderId(tx2.getSourceId())
                    .externalAmount(new BigDecimal("605000.0000"))
                    .internalAmount(new BigDecimal("620000.0000"))
                    .discrepancyAmount(new BigDecimal("-15000.0000"))
                    .feeExternal(new BigDecimal("45000.0000"))
                    .feeInternal(new BigDecimal("30000.0000"))
                    .matchStatus(MatchStatus.FEE_MISMATCH)
                    .resolutionStatus(ResolutionStatus.PENDING)
                    .build();

            // Item 3: Đơn hàng thiếu phía nội bộ
            ReconciliationItem item3 = ReconciliationItem.builder()
                    .batch(batch)
                    .externalOrderId("SP-20261113-999")
                    .externalAmount(new BigDecimal("280000.0000"))
                    .internalAmount(BigDecimal.ZERO)
                    .discrepancyAmount(new BigDecimal("280000.0000"))
                    .feeExternal(new BigDecimal("12000.0000"))
                    .feeInternal(BigDecimal.ZERO)
                    .matchStatus(MatchStatus.UNMATCHED_EXTERNAL)
                    .resolutionStatus(ResolutionStatus.PENDING)
                    .build();

            reconciliationItemRepository.save(item1);
            reconciliationItemRepository.save(item2);
            reconciliationItemRepository.save(item3);
            log.info("Initialized DEMO Reconciliation Batch: REC-SP-20261113-01 with 3 items");

            // Seed Sample Finance Transaction for TikTok Shop
            FinanceTransaction txTikTok = FinanceTransaction.builder()
                    .legalEntity(demo)
                    .transactionCode("TX-TIKTOK-001")
                    .transactionType(FinanceTransactionType.SALE_ORDER)
                    .sourceType("TIKTOK_ORDER")
                    .sourceId("TT-20261113-001")
                    .channel(TransactionChannel.TIKTOK)
                    .amount(new BigDecimal("647500.0000"))
                    .currencyCode("VND")
                    .occurredAt(Instant.now().minusSeconds(21600))
                    .status(FinanceTransactionStatus.CONFIRMED)
                    .description("Đơn hàng TikTok Shop Đầm Dạ Hội Lụa TT-20261113-001")
                    .metadata("{\"buyer\":\"le_thi_c\"}")
                    .build();
            financeTransactionRepository.save(txTikTok);

            // Seed TikTok Reconciliation Batch (Sprint 8)
            ReconciliationBatch tiktokBatch = ReconciliationBatch.builder()
                    .legalEntity(demo)
                    .batchCode("REC-TT-20261113-01")
                    .sourceType(ReconciliationSourceType.TIKTOK)
                    .status(BatchStatus.COMPLETED)
                    .fileName("TikTok_Settlement_20261113.csv")
                    .totalRecords(1)
                    .matchedRecords(1)
                    .discrepancyRecords(0)
                    .totalAmountExternal(new BigDecimal("647500.0000"))
                    .totalAmountInternal(new BigDecimal("647500.0000"))
                    .notes("Đợt đối soát TikTok Shop tự động: Khớp 100% doanh thu và phí hoa hồng sàn.")
                    .build();
            reconciliationBatchRepository.save(tiktokBatch);

            ReconciliationItem ttItem = ReconciliationItem.builder()
                    .batch(tiktokBatch)
                    .externalOrderId("TT-20261113-001")
                    .internalOrderId(txTikTok.getSourceId())
                    .externalAmount(new BigDecimal("647500.0000"))
                    .internalAmount(new BigDecimal("647500.0000"))
                    .discrepancyAmount(BigDecimal.ZERO)
                    .feeExternal(new BigDecimal("72500.0000"))
                    .feeInternal(new BigDecimal("72500.0000"))
                    .matchStatus(MatchStatus.EXACT_MATCH)
                    .resolutionStatus(ResolutionStatus.RESOLVED)
                    .resolvedBy("SYSTEM")
                    .resolutionNotes("Tự động khớp chính xác với ví TikTok Shop")
                    .build();
            reconciliationItemRepository.save(ttItem);

            // Seed Sample Finance Transaction for COD GHN
            FinanceTransaction txCod = FinanceTransaction.builder()
                    .legalEntity(demo)
                    .transactionCode("TX-COD-001")
                    .transactionType(FinanceTransactionType.SALE_ORDER)
                    .sourceType("COD_SHIPMENT")
                    .sourceId("GHN-VN-889922")
                    .channel(TransactionChannel.OFFLINE)
                    .amount(new BigDecimal("810000.0000"))
                    .currencyCode("VND")
                    .occurredAt(Instant.now().minusSeconds(10800))
                    .status(FinanceTransactionStatus.CONFIRMED)
                    .description("Đơn COD Giao Hàng Nhanh - Thu hộ tiền mặt GHN-VN-889922")
                    .metadata("{\"carrier\":\"GHN\"}")
                    .build();
            financeTransactionRepository.save(txCod);

            // Seed COD Carrier Reconciliation Batch (Sprint 8)
            ReconciliationBatch codBatch = ReconciliationBatch.builder()
                    .legalEntity(demo)
                    .batchCode("REC-COD-20261113-01")
                    .sourceType(ReconciliationSourceType.COD)
                    .status(BatchStatus.DISCREPANCY_FOUND)
                    .fileName("GHN_COD_Settlement_20261113.csv")
                    .totalRecords(2)
                    .matchedRecords(1)
                    .discrepancyRecords(1)
                    .totalAmountExternal(new BigDecimal("1085000.0000"))
                    .totalAmountInternal(new BigDecimal("810000.0000"))
                    .notes("Đợt đối soát tiền thu hộ COD từ Giao Hàng Nhanh (GHN).")
                    .build();
            reconciliationBatchRepository.save(codBatch);

            ReconciliationItem codItem1 = ReconciliationItem.builder()
                    .batch(codBatch)
                    .externalOrderId("GHN-VN-889922")
                    .internalOrderId(txCod.getSourceId())
                    .externalAmount(new BigDecimal("810000.0000"))
                    .internalAmount(new BigDecimal("810000.0000"))
                    .discrepancyAmount(BigDecimal.ZERO)
                    .feeExternal(new BigDecimal("40000.0000"))
                    .feeInternal(new BigDecimal("40000.0000"))
                    .matchStatus(MatchStatus.EXACT_MATCH)
                    .resolutionStatus(ResolutionStatus.RESOLVED)
                    .resolvedBy("SYSTEM")
                    .resolutionNotes("ĐVVC đã chuyển khoản đúng số tiền thu hộ trừ cước vận chuyển")
                    .build();

            ReconciliationItem codItem2 = ReconciliationItem.builder()
                    .batch(codBatch)
                    .externalOrderId("GHN-VN-889923")
                    .externalAmount(new BigDecimal("275000.0000"))
                    .internalAmount(BigDecimal.ZERO)
                    .discrepancyAmount(new BigDecimal("275000.0000"))
                    .feeExternal(new BigDecimal("25000.0000"))
                    .feeInternal(BigDecimal.ZERO)
                    .matchStatus(MatchStatus.UNMATCHED_EXTERNAL)
                    .resolutionStatus(ResolutionStatus.PENDING)
                    .resolutionNotes("Đơn COD chuyển khoản ngoài danh sách vận đơn nội bộ")
                    .build();

            reconciliationItemRepository.save(codItem1);
            reconciliationItemRepository.save(codItem2);
            log.info("Initialized DEMO TikTok & COD Carrier Reconciliation Batches for Sprint 8");
        }
    }
}


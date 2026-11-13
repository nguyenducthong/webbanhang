package com.fashion.platform.einvoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.platform.einvoice.application.service.InvoiceIssuanceService;
import com.fashion.platform.einvoice.domain.model.*;
import com.fashion.platform.einvoice.domain.repository.InvoiceDocumentRepository;
import com.fashion.platform.einvoice.domain.repository.InvoiceRepository;
import com.fashion.platform.einvoice.infrastructure.adapter.fake.FakeEInvoiceProviderAdapter;
import com.fashion.platform.finance.domain.entity.LegalEntity;
import com.fashion.platform.finance.domain.entity.LegalEntityStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class UnknownResultRecoveryTest {

    private InvoiceRepository invoiceRepository;
    private InvoiceDocumentRepository documentRepository;
    private FakeEInvoiceProviderAdapter fakeAdapter;
    private InvoiceIssuanceService issuanceService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        invoiceRepository = Mockito.mock(InvoiceRepository.class);
        documentRepository = Mockito.mock(InvoiceDocumentRepository.class);
        fakeAdapter = new FakeEInvoiceProviderAdapter();
        objectMapper = new ObjectMapper();

        issuanceService = new InvoiceIssuanceService(
                invoiceRepository,
                documentRepository,
                List.of(fakeAdapter),
                objectMapper
        );
    }

    @Test
    @DisplayName("Should recover seamlessly from UNKNOWN_RESULT via queryInvoiceStatus without duplicating")
    void shouldRecoverFromUnknownResult() {
        UUID invoiceId = UUID.randomUUID();
        LegalEntity entity = LegalEntity.builder()
                .id(UUID.randomUUID())
                .entityCode("LE-01")
                .entityName("Fashion Store")
                .status(LegalEntityStatus.ACTIVE)
                .build();

        InvoiceTemplate template = InvoiceTemplate.builder()
                .templateCode("1/001")
                .invoiceSeries("C26TDD")
                .invoiceType(InvoiceType.VAT_INVOICE)
                .build();

        InvoiceProvider provider = InvoiceProvider.builder()
                .providerCode("FAKE")
                .name("Fake Provider")
                .build();

        InvoiceProviderAccount account = InvoiceProviderAccount.builder()
                .provider(provider)
                .legalEntity(entity)
                .accountUsername("demo")
                .apiEndpoint("http://fake.api")
                .build();

        Invoice invoice = Invoice.builder()
                .id(invoiceId)
                .legalEntity(entity)
                .template(template)
                .providerAccount(account)
                .invoiceType(InvoiceType.VAT_INVOICE)
                .invoiceStatus(InvoiceStatus.PENDING_ISSUANCE)
                .idempotencyKey("KEY-TIMEOUT-RECOVER-001")
                .sellerSnapshot("{\"legalName\":\"Fashion Store\"}")
                .buyerSnapshot("{\"buyerName\":\"Nguyen Van A\",\"enterprise\":false}")
                .build();

        InvoiceItem item = InvoiceItem.builder()
                .lineNumber(1)
                .itemName("Vay Da Hoi")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("1000000.0000"))
                .taxRate(new BigDecimal("0.1000"))
                .build();
        invoice.addItem(item);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.saveAndFlush(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 1. Kích hoạt timeout mô phỏng
        fakeAdapter.setSimulateTimeout(true);

        Invoice resultUnknown = issuanceService.issueInvoice(invoiceId, "tester");
        assertEquals(InvoiceStatus.UNKNOWN_RESULT, resultUnknown.getInvoiceStatus());
        assertNotNull(resultUnknown.getErrorMessage());
        assertTrue(resultUnknown.getErrorMessage().contains("timed out"));

        // 2. Tắt chế độ timeout để mô phỏng mạng hoạt động trở lại
        fakeAdapter.setSimulateTimeout(false);

        // 3. Thử phát hành lại - Service phải tự động query provider và chuyển sang ISSUED
        Invoice resultRecovered = issuanceService.issueInvoice(invoiceId, "tester");
        assertEquals(InvoiceStatus.ISSUED, resultRecovered.getInvoiceStatus());
        assertNotNull(resultRecovered.getInvoiceNumber());
        assertNotNull(resultRecovered.getTaxAuthorityCode());
        assertNull(resultRecovered.getErrorMessage());

        // Đảm bảo có audit log RECOVERED_ISSUED
        boolean hasRecoverAudit = resultRecovered.getAuditLogs().stream()
                .anyMatch(log -> "RECOVERED_ISSUED".equals(log.getAction()));
        assertTrue(hasRecoverAudit, "Expected audit log with action RECOVERED_ISSUED");
    }
}

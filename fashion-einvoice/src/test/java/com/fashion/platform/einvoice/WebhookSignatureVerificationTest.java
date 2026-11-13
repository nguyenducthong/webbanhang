package com.fashion.platform.einvoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.platform.einvoice.application.service.InvoiceWebhookService;
import com.fashion.platform.einvoice.domain.model.*;
import com.fashion.platform.einvoice.domain.repository.InvoiceProviderAccountRepository;
import com.fashion.platform.einvoice.domain.repository.InvoiceRepository;
import com.fashion.platform.einvoice.domain.repository.InvoiceWebhookLogRepository;
import com.fashion.platform.finance.domain.entity.LegalEntity;
import com.fashion.platform.finance.domain.entity.LegalEntityStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class WebhookSignatureVerificationTest {

    private InvoiceRepository invoiceRepository;
    private InvoiceProviderAccountRepository accountRepository;
    private InvoiceWebhookLogRepository webhookLogRepository;
    private ObjectMapper objectMapper;
    private InvoiceWebhookService webhookService;

    @BeforeEach
    void setUp() {
        invoiceRepository = Mockito.mock(InvoiceRepository.class);
        accountRepository = Mockito.mock(InvoiceProviderAccountRepository.class);
        webhookLogRepository = Mockito.mock(InvoiceWebhookLogRepository.class);
        objectMapper = new ObjectMapper();

        webhookService = new InvoiceWebhookService(
                invoiceRepository,
                accountRepository,
                webhookLogRepository,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should successfully process webhook with valid HMAC-SHA256 signature")
    void shouldProcessWebhookWithValidSignature() {
        UUID invoiceId = UUID.randomUUID();
        String secret = "secret_test_key_123";

        InvoiceProviderAccount account = InvoiceProviderAccount.builder()
                .webhookSecret(secret)
                .build();

        Invoice invoice = Invoice.builder()
                .id(invoiceId)
                .providerAccount(account)
                .invoiceStatus(InvoiceStatus.SUBMITTED)
                .idempotencyKey("KEY-WH-001")
                .build();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        String payload = String.format("{\"eventType\":\"TAX_AUTHORITY_ACCEPTED\",\"invoiceId\":\"%s\",\"taxAuthorityCode\":\"TCT-SUCCESS-999\"}", invoiceId);
        String validSignature = InvoiceWebhookService.computeHmacSha256(payload, secret);

        assertDoesNotThrow(() -> webhookService.processWebhook("VNPT", payload, validSignature));
        assertEquals(InvoiceStatus.ISSUED, invoice.getInvoiceStatus());
        assertEquals("TCT-SUCCESS-999", invoice.getTaxAuthorityCode());
    }

    @Test
    @DisplayName("Should reject webhook with invalid signature")
    void shouldRejectWebhookWithInvalidSignature() {
        UUID invoiceId = UUID.randomUUID();
        String secret = "secret_test_key_123";

        InvoiceProviderAccount account = InvoiceProviderAccount.builder()
                .webhookSecret(secret)
                .build();

        Invoice invoice = Invoice.builder()
                .id(invoiceId)
                .providerAccount(account)
                .invoiceStatus(InvoiceStatus.SUBMITTED)
                .build();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        String payload = String.format("{\"eventType\":\"TAX_AUTHORITY_ACCEPTED\",\"invoiceId\":\"%s\"}", invoiceId);
        String invalidSignature = "tampered_fake_signature";

        assertThrows(RuntimeException.class, () -> webhookService.processWebhook("VNPT", payload, invalidSignature));
    }
}

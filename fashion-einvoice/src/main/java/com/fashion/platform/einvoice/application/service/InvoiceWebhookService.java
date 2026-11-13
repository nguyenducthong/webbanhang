package com.fashion.platform.einvoice.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.platform.einvoice.domain.model.Invoice;
import com.fashion.platform.einvoice.domain.model.InvoiceProviderAccount;
import com.fashion.platform.einvoice.domain.model.InvoiceStatus;
import com.fashion.platform.einvoice.domain.model.InvoiceWebhookLog;
import com.fashion.platform.einvoice.domain.repository.InvoiceProviderAccountRepository;
import com.fashion.platform.einvoice.domain.repository.InvoiceRepository;
import com.fashion.platform.einvoice.domain.repository.InvoiceWebhookLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceWebhookService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceProviderAccountRepository accountRepository;
    private final InvoiceWebhookLogRepository webhookLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processWebhook(String providerCode, String payload, String signature) {
        log.info("Received E-Invoice webhook from provider {}. Signature: {}", providerCode, signature);

        Invoice invoice = null;
        String eventType = "UNKNOWN";

        try {
            JsonNode root = objectMapper.readTree(payload);
            eventType = root.has("eventType") ? root.get("eventType").asText() : "TAX_AUTHORITY_RESULT";
            String invoiceIdStr = root.has("invoiceId") ? root.get("invoiceId").asText() : null;
            String idempotencyKey = root.has("idempotencyKey") ? root.get("idempotencyKey").asText() : null;

            if (invoiceIdStr != null) {
                invoice = invoiceRepository.findById(UUID.fromString(invoiceIdStr)).orElse(null);
            }
            if (invoice == null && idempotencyKey != null) {
                invoice = invoiceRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            }

            if (invoice == null) {
                throw new IllegalArgumentException("Target invoice not found for webhook: " + payload);
            }

            // 1. Thẩm định chữ ký số Webhook HMAC-SHA256
            String secret = "sec_webhook_default_key_2026";
            if (invoice.getProviderAccount() != null && invoice.getProviderAccount().getWebhookSecret() != null) {
                secret = invoice.getProviderAccount().getWebhookSecret();
            }

            if (signature != null && !signature.isBlank()) {
                String expectedSig = computeHmacSha256(payload, secret);
                if (!MessageDigest.isEqual(expectedSig.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
                    throw new SecurityException("Webhook signature verification failed!");
                }
            }

            // 2. Xử lý các sự kiện
            if ("TAX_AUTHORITY_ACCEPTED".equalsIgnoreCase(eventType)) {
                String taxAuthorityCode = root.has("taxAuthorityCode") ? root.get("taxAuthorityCode").asText() : "TCT-WH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                invoice.setTaxAuthorityCode(taxAuthorityCode);
                invoice.setErrorMessage(null);
                if (invoice.getInvoiceStatus() != InvoiceStatus.ISSUED) {
                    invoice.transitionTo(InvoiceStatus.ISSUED, "WEBHOOK_TAX_ACCEPTED", "Tax authority code assigned via Webhook: " + taxAuthorityCode, "PROVIDER_WEBHOOK");
                }
                log.info("Invoice {} confirmed ISSUED via Tax Authority webhook.", invoice.getId());
            } else if ("TAX_AUTHORITY_REJECTED".equalsIgnoreCase(eventType)) {
                String reason = root.has("reason") ? root.get("reason").asText() : "Rejected by Tax Authority";
                invoice.setErrorMessage(reason);
                invoice.transitionTo(InvoiceStatus.REJECTED, "WEBHOOK_TAX_REJECTED", reason, "PROVIDER_WEBHOOK");
                log.warn("Invoice {} REJECTED by Tax Authority webhook: {}", invoice.getId(), reason);
            }

            invoiceRepository.save(invoice);

            // 3. Ghi log webhook thành công
            InvoiceWebhookLog logEntry = InvoiceWebhookLog.builder()
                    .providerCode(providerCode)
                    .invoice(invoice)
                    .eventType(eventType)
                    .payload(payload)
                    .signature(signature)
                    .processedStatus("SUCCESS")
                    .createdAt(Instant.now())
                    .build();
            webhookLogRepository.save(logEntry);

        } catch (Exception e) {
            log.error("Error processing webhook from {}: {}", providerCode, e.getMessage());
            InvoiceWebhookLog errorLog = InvoiceWebhookLog.builder()
                    .providerCode(providerCode)
                    .invoice(invoice)
                    .eventType(eventType)
                    .payload(payload)
                    .signature(signature)
                    .processedStatus("FAILED")
                    .errorMessage(e.getMessage())
                    .createdAt(Instant.now())
                    .build();
            webhookLogRepository.save(errorLog);
            throw new RuntimeException("Webhook processing error: " + e.getMessage(), e);
        }
    }

    public static String computeHmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC-SHA256", e);
        }
    }
}

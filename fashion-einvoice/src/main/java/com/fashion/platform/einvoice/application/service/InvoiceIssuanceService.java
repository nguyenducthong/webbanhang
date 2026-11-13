package com.fashion.platform.einvoice.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.platform.einvoice.domain.adapter.EInvoiceProviderAdapter;
import com.fashion.platform.einvoice.domain.adapter.ProviderIssuanceResult;
import com.fashion.platform.einvoice.domain.adapter.ProviderStatusResult;
import com.fashion.platform.einvoice.domain.model.*;
import com.fashion.platform.einvoice.domain.repository.InvoiceDocumentRepository;
import com.fashion.platform.einvoice.domain.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceIssuanceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceDocumentRepository documentRepository;
    private final List<EInvoiceProviderAdapter> providerAdapters;
    private final ObjectMapper objectMapper;

    private Map<String, EInvoiceProviderAdapter> getAdapterMap() {
        return providerAdapters.stream()
                .collect(Collectors.toMap(EInvoiceProviderAdapter::getProviderCode, a -> a));
    }

    private EInvoiceProviderAdapter getAdapter(String providerCode) {
        EInvoiceProviderAdapter adapter = getAdapterMap().get(providerCode != null ? providerCode : "FAKE");
        if (adapter == null) {
            // Default to FAKE if not matched
            return getAdapterMap().get("FAKE");
        }
        return adapter;
    }

    @Transactional
    public Invoice validateInvoice(UUID invoiceId, String performedBy) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        if (invoice.getInvoiceStatus() == InvoiceStatus.ISSUED) {
            throw new IllegalStateException("Invoice is already ISSUED");
        }

        invoice.transitionTo(InvoiceStatus.VALIDATING, "VALIDATE", "Starting invoice validation", performedBy);

        try {
            // 1. Kiểm tra danh sách mặt hàng
            if (invoice.getItems().isEmpty()) {
                throw new IllegalArgumentException("Invoice has no items");
            }

            for (InvoiceItem item : invoice.getItems()) {
                if (item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Item " + item.getItemName() + " has quantity <= 0");
                }
                if (item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Item " + item.getItemName() + " has unitPrice < 0");
                }
            }

            // 2. Kiểm tra thông tin người mua
            BuyerSnapshot buyer = objectMapper.readValue(invoice.getBuyerSnapshot(), BuyerSnapshot.class);
            if (buyer.isEnterprise()) {
                String taxCode = buyer.getTaxCode();
                if (taxCode == null || !taxCode.matches("^[0-9]{10}(-[0-9]{3})?$")) {
                    throw new IllegalArgumentException("Enterprise taxCode is invalid: " + taxCode);
                }
            }

            // 3. Kiểm tra tính cân đối tiền tệ
            BigDecimal calculatedTotal = invoice.getTotalPretaxAmount().add(invoice.getTotalTaxAmount());
            BigDecimal diff = calculatedTotal.subtract(invoice.getTotalAmount()).abs();
            if (diff.compareTo(new BigDecimal("1.0000")) > 0) {
                throw new IllegalArgumentException("Discrepancy between pretax + tax vs total: " + diff);
            }

            invoice.transitionTo(InvoiceStatus.PENDING_ISSUANCE, "VALIDATION_PASSED", "Validation successful", performedBy);
            invoice.setErrorMessage(null);
        } catch (Exception e) {
            log.error("Validation failed for invoice {}: {}", invoiceId, e.getMessage());
            invoice.setErrorMessage(e.getMessage());
            invoice.transitionTo(InvoiceStatus.REJECTED, "VALIDATION_FAILED", e.getMessage(), performedBy);
        }

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice issueInvoice(UUID invoiceId, String performedBy) {
        // Khóa bi quan (Pessimistic Lock) chặn race condition giữa các luồng phát hành đồng thời
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        // 1. Nếu đã ISSUED hoặc đang SUBMITTED thì trả về ngay để tránh Duplicate Issuance
        if (invoice.getInvoiceStatus() == InvoiceStatus.ISSUED) {
            log.warn("Invoice {} is already ISSUED. Returning current state.", invoiceId);
            return invoice;
        }

        // 2. Xác định Adapter
        String providerCode = (invoice.getProviderAccount() != null && invoice.getProviderAccount().getProvider() != null)
                ? invoice.getProviderAccount().getProvider().getProviderCode()
                : "FAKE";
        EInvoiceProviderAdapter adapter = getAdapter(providerCode);

        // 3. Xử lý UNKNOWN_RESULT Recovery
        if (invoice.getInvoiceStatus() == InvoiceStatus.UNKNOWN_RESULT) {
            log.warn("Invoice {} is in UNKNOWN_RESULT status. Querying provider before taking action...", invoiceId);
            ProviderStatusResult statusResult = adapter.queryInvoiceStatus(invoice, invoice.getProviderAccount());
            if (statusResult.status() == InvoiceStatus.ISSUED) {
                log.info("Recovery success: Provider confirmed invoice {} was already ISSUED (number: {})",
                        invoiceId, statusResult.invoiceNumber());
                invoice.setInvoiceNumber(statusResult.invoiceNumber());
                invoice.setTaxAuthorityCode(statusResult.taxAuthorityCode());
                invoice.setIssueDate(statusResult.issueDate() != null ? statusResult.issueDate() : Instant.now());
                invoice.setErrorMessage(null);
                invoice.transitionTo(InvoiceStatus.ISSUED, "RECOVERED_ISSUED", "Confirmed ISSUED via status query", performedBy);
                return invoiceRepository.save(invoice);
            } else if (statusResult.status() == InvoiceStatus.PENDING_ISSUANCE) {
                log.info("Invoice {} is still pending at provider. Waiting...", invoiceId);
                return invoice;
            } else {
                log.info("Provider does not have invoice {}. Safe to re-issue.", invoiceId);
                // Đưa về PENDING_ISSUANCE để phát hành lại
                invoice.setInvoiceStatus(InvoiceStatus.PENDING_ISSUANCE);
            }
        }

        // 4. Nếu chưa Validate, tiến hành validate trước
        if (invoice.getInvoiceStatus() == InvoiceStatus.DRAFT) {
            invoice = validateInvoice(invoiceId, performedBy);
            if (invoice.getInvoiceStatus() != InvoiceStatus.PENDING_ISSUANCE) {
                throw new IllegalStateException("Invoice validation failed: " + invoice.getErrorMessage());
            }
        }

        // 5. Gửi lệnh phát hành sang Provider
        invoice.transitionTo(InvoiceStatus.SUBMITTED, "SUBMIT_PROVIDER", "Sending to provider " + providerCode, performedBy);
        invoice = invoiceRepository.saveAndFlush(invoice);

        ProviderIssuanceResult result = adapter.issueInvoice(invoice, invoice.getProviderAccount());

        // 6. Xử lý kết quả trả về
        if (result.isUnknownResult()) {
            invoice.setErrorMessage(result.errorMessage());
            invoice.transitionTo(InvoiceStatus.UNKNOWN_RESULT, "PROVIDER_TIMEOUT", result.errorMessage(), performedBy);
            log.warn("Invoice {} transitioned to UNKNOWN_RESULT: {}", invoiceId, result.errorMessage());
        } else if (result.success()) {
            invoice.setInvoiceNumber(result.invoiceNumber());
            invoice.setInvoiceSeries(result.invoiceSeries());
            invoice.setTemplateCode(result.templateCode());
            invoice.setTaxAuthorityCode(result.taxAuthorityCode());
            invoice.setIssueDate(result.issueDate() != null ? result.issueDate() : Instant.now());
            invoice.setErrorMessage(null);
            invoice.transitionTo(InvoiceStatus.ISSUED, "ISSUED_SUCCESS", "Successfully issued by " + providerCode, performedBy);

            // Lưu tài liệu XML và PDF
            saveDocument(invoice, InvoiceDocumentType.XML_ORIGINAL, result.xmlContent().getBytes());
            if (result.pdfContent() != null && result.pdfContent().length > 0) {
                saveDocument(invoice, InvoiceDocumentType.PDF_REPRESENTATION, result.pdfContent());
            }
        } else {
            invoice.setErrorMessage(result.errorMessage());
            invoice.transitionTo(InvoiceStatus.REJECTED, "PROVIDER_REJECTED", result.errorMessage(), performedBy);
        }

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice queryAndSyncStatus(UUID invoiceId, String performedBy) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        String providerCode = (invoice.getProviderAccount() != null && invoice.getProviderAccount().getProvider() != null)
                ? invoice.getProviderAccount().getProvider().getProviderCode()
                : "FAKE";
        EInvoiceProviderAdapter adapter = getAdapter(providerCode);

        ProviderStatusResult statusResult = adapter.queryInvoiceStatus(invoice, invoice.getProviderAccount());
        if (statusResult.status() == InvoiceStatus.ISSUED && invoice.getInvoiceStatus() != InvoiceStatus.ISSUED) {
            invoice.setInvoiceNumber(statusResult.invoiceNumber());
            invoice.setTaxAuthorityCode(statusResult.taxAuthorityCode());
            invoice.setIssueDate(statusResult.issueDate() != null ? statusResult.issueDate() : Instant.now());
            invoice.transitionTo(InvoiceStatus.ISSUED, "SYNC_ISSUED", "Synced with provider", performedBy);
            return invoiceRepository.save(invoice);
        }

        return invoice;
    }

    private void saveDocument(Invoice invoice, InvoiceDocumentType docType, byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content);
            String hash = HexFormat.of().formatHex(hashBytes);

            String key = "invoices/" + invoice.getLegalEntity().getId() + "/" + invoice.getId() + "/" + docType.name().toLowerCase() + (docType == InvoiceDocumentType.XML_ORIGINAL ? ".xml" : ".pdf");

            InvoiceDocument document = InvoiceDocument.builder()
                    .invoice(invoice)
                    .documentType(docType)
                    .s3Bucket("fashion-invoices")
                    .s3Key(key)
                    .fileHash(hash)
                    .fileSize((long) content.length)
                    .createdAt(Instant.now())
                    .build();

            documentRepository.save(document);
        } catch (Exception e) {
            log.error("Failed to hash/save invoice document: {}", e.getMessage());
        }
    }
}

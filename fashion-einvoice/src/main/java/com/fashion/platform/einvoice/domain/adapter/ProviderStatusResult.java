package com.fashion.platform.einvoice.domain.adapter;

import com.fashion.platform.einvoice.domain.model.InvoiceStatus;

import java.time.Instant;

public record ProviderStatusResult(
        InvoiceStatus status,
        String invoiceNumber,
        String taxAuthorityCode,
        Instant issueDate,
        String rawMessage
) {
    public static ProviderStatusResult issued(String invoiceNumber, String taxAuthorityCode, Instant issueDate) {
        return new ProviderStatusResult(InvoiceStatus.ISSUED, invoiceNumber, taxAuthorityCode, issueDate, "Invoice confirmed ISSUED by provider");
    }

    public static ProviderStatusResult pending() {
        return new ProviderStatusResult(InvoiceStatus.PENDING_ISSUANCE, null, null, null, "Invoice is still processing at provider");
    }

    public static ProviderStatusResult notFound() {
        return new ProviderStatusResult(InvoiceStatus.DRAFT, null, null, null, "Invoice not found at provider");
    }

    public static ProviderStatusResult rejected(String reason) {
        return new ProviderStatusResult(InvoiceStatus.REJECTED, null, null, null, reason);
    }
}

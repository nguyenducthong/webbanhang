package com.fashion.platform.einvoice.domain.adapter;

import java.time.Instant;

public record ProviderIssuanceResult(
        boolean success,
        boolean isUnknownResult,
        String invoiceNumber,
        String invoiceSeries,
        String templateCode,
        Instant issueDate,
        String taxAuthorityCode,
        String xmlContent,
        byte[] pdfContent,
        String errorMessage
) {
    public static ProviderIssuanceResult success(
            String invoiceNumber,
            String invoiceSeries,
            String templateCode,
            Instant issueDate,
            String taxAuthorityCode,
            String xmlContent,
            byte[] pdfContent
    ) {
        return new ProviderIssuanceResult(
                true,
                false,
                invoiceNumber,
                invoiceSeries,
                templateCode,
                issueDate,
                taxAuthorityCode,
                xmlContent,
                pdfContent,
                null
        );
    }

    public static ProviderIssuanceResult unknown(String message) {
        return new ProviderIssuanceResult(
                false,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                message
        );
    }

    public static ProviderIssuanceResult failure(String errorMessage) {
        return new ProviderIssuanceResult(
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                errorMessage
        );
    }
}

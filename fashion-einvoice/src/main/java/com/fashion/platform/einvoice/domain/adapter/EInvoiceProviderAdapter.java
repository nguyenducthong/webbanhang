package com.fashion.platform.einvoice.domain.adapter;

import com.fashion.platform.einvoice.domain.model.Invoice;
import com.fashion.platform.einvoice.domain.model.InvoiceDocumentType;
import com.fashion.platform.einvoice.domain.model.InvoiceProviderAccount;

public interface EInvoiceProviderAdapter {

    String getProviderCode();

    ProviderIssuanceResult issueInvoice(Invoice invoice, InvoiceProviderAccount account);

    ProviderStatusResult queryInvoiceStatus(Invoice invoice, InvoiceProviderAccount account);

    byte[] downloadDocument(Invoice invoice, InvoiceProviderAccount account, InvoiceDocumentType documentType);
}

package com.fashion.platform.einvoice.domain.repository;

import com.fashion.platform.einvoice.domain.model.InvoiceTemplate;
import com.fashion.platform.einvoice.domain.model.InvoiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceTemplateRepository extends JpaRepository<InvoiceTemplate, UUID> {
    Optional<InvoiceTemplate> findFirstByLegalEntityIdAndInvoiceTypeAndActiveTrue(UUID legalEntityId, InvoiceType invoiceType);
    Optional<InvoiceTemplate> findByLegalEntityIdAndTemplateCodeAndInvoiceSeries(UUID legalEntityId, String templateCode, String invoiceSeries);
    List<InvoiceTemplate> findByLegalEntityId(UUID legalEntityId);
}

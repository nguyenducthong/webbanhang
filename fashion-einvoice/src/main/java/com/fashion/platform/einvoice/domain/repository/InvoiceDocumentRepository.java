package com.fashion.platform.einvoice.domain.repository;

import com.fashion.platform.einvoice.domain.model.InvoiceDocument;
import com.fashion.platform.einvoice.domain.model.InvoiceDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceDocumentRepository extends JpaRepository<InvoiceDocument, UUID> {
    List<InvoiceDocument> findByInvoiceId(UUID invoiceId);
    Optional<InvoiceDocument> findByInvoiceIdAndDocumentType(UUID invoiceId, InvoiceDocumentType documentType);
}

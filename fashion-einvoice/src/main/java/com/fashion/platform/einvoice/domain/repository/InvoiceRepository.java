package com.fashion.platform.einvoice.domain.repository;

import com.fashion.platform.einvoice.domain.model.Invoice;
import com.fashion.platform.einvoice.domain.model.InvoiceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByIdempotencyKey(String idempotencyKey);
    Optional<Invoice> findByFinanceTransactionId(UUID financeTransactionId);
    Page<Invoice> findByLegalEntityId(UUID legalEntityId, Pageable pageable);
    List<Invoice> findByInvoiceStatus(InvoiceStatus status);
    List<Invoice> findByInvoiceStatusIn(Collection<InvoiceStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") UUID id);
}

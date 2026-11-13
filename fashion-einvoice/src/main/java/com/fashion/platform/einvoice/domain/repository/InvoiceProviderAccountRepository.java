package com.fashion.platform.einvoice.domain.repository;

import com.fashion.platform.einvoice.domain.model.InvoiceProviderAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceProviderAccountRepository extends JpaRepository<InvoiceProviderAccount, UUID> {
    Optional<InvoiceProviderAccount> findFirstByLegalEntityIdAndActiveTrue(UUID legalEntityId);
    Optional<InvoiceProviderAccount> findByLegalEntityIdAndProviderId(UUID legalEntityId, UUID providerId);
}

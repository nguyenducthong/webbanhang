package com.fashion.platform.einvoice.domain.repository;

import com.fashion.platform.einvoice.domain.model.InvoiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceProviderRepository extends JpaRepository<InvoiceProvider, UUID> {
    Optional<InvoiceProvider> findByProviderCode(String providerCode);
}

package com.fashion.platform.einvoice.domain.model;

import com.fashion.platform.finance.domain.entity.LegalEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invoice_provider_accounts",
       uniqueConstraints = @UniqueConstraint(name = "uq_provider_account_entity", columnNames = {"legal_entity_id", "provider_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceProviderAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private InvoiceProvider provider;

    @Column(name = "account_username", nullable = false, length = 100)
    private String accountUsername;

    @Column(name = "account_password_encrypted", length = 255)
    private String accountPasswordEncrypted;

    @Column(name = "api_endpoint", nullable = false)
    private String apiEndpoint;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}

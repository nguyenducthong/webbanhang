package com.fashion.platform.einvoice.domain.model;

import com.fashion.platform.finance.domain.entity.LegalEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invoice_templates",
       uniqueConstraints = @UniqueConstraint(name = "uq_template_series_entity", columnNames = {"legal_entity_id", "template_code", "invoice_series"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @Column(name = "template_code", nullable = false, length = 50)
    private String templateCode; // e.g. "1/001"

    @Column(name = "invoice_series", nullable = false, length = 20)
    private String invoiceSeries; // e.g. "C26TDD"

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 50)
    @Builder.Default
    private InvoiceType invoiceType = InvoiceType.VAT_INVOICE;

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

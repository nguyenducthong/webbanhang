package com.fashion.platform.einvoice.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invoice_documents",
       uniqueConstraints = @UniqueConstraint(name = "uq_invoice_document_type", columnNames = {"invoice_id", "document_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private InvoiceDocumentType documentType;

    @Column(name = "s3_bucket", nullable = false, length = 100)
    private String s3Bucket;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Column(name = "file_hash", length = 100)
    private String fileHash;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

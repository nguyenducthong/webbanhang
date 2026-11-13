package com.fashion.platform.einvoice.domain.model;

import com.fashion.platform.finance.domain.entity.FinanceTransaction;
import com.fashion.platform.finance.domain.entity.LegalEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices",
       indexes = {
           @Index(name = "idx_invoices_legal_entity", columnList = "legal_entity_id"),
           @Index(name = "idx_invoices_status", columnList = "invoice_status"),
           @Index(name = "idx_invoices_issue_date", columnList = "issue_date"),
           @Index(name = "idx_invoices_tx_id", columnList = "finance_transaction_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_account_id")
    private InvoiceProviderAccount providerAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private InvoiceTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finance_transaction_id")
    private FinanceTransaction financeTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 50)
    @Builder.Default
    private InvoiceType invoiceType = InvoiceType.VAT_INVOICE;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_status", nullable = false, length = 50)
    @Builder.Default
    private InvoiceStatus invoiceStatus = InvoiceStatus.DRAFT;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber; // Số hóa đơn, ví dụ: "00000001" (được cấp bởi NCC hóa đơn)

    @Column(name = "invoice_series", length = 20)
    private String invoiceSeries; // Ký hiệu, ví dụ: "C26TDD"

    @Column(name = "template_code", length = 50)
    private String templateCode; // Mẫu số, ví dụ: "1/001"

    @Column(name = "issue_date")
    private Instant issueDate;

    @Column(name = "tax_authority_code", length = 100)
    private String taxAuthorityCode; // Mã CQT cấp (theo Nghị định 123)

    @Column(name = "seller_snapshot", nullable = false, columnDefinition = "TEXT")
    private String sellerSnapshot;

    @Column(name = "buyer_snapshot", nullable = false, columnDefinition = "TEXT")
    private String buyerSnapshot;

    @Column(name = "total_pretax_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalPretaxAmount = BigDecimal.ZERO;

    @Column(name = "total_discount_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalDiscountAmount = BigDecimal.ZERO;

    @Column(name = "total_tax_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalTaxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceAuditLog> auditLogs = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addItem(InvoiceItem item) {
        if (this.invoiceStatus == InvoiceStatus.ISSUED) {
            throw new IllegalStateException("Cannot add items to an already ISSUED invoice");
        }
        item.setInvoice(this);
        item.calculateAmounts();
        this.items.add(item);
        recalculateTotals();
    }

    public void recalculateTotals() {
        if (this.invoiceStatus == InvoiceStatus.ISSUED) {
            throw new IllegalStateException("Cannot modify totals of an already ISSUED invoice");
        }
        BigDecimal sumPretax = BigDecimal.ZERO;
        BigDecimal sumDiscount = BigDecimal.ZERO;
        BigDecimal sumTax = BigDecimal.ZERO;
        BigDecimal sumTotal = BigDecimal.ZERO;

        for (InvoiceItem item : this.items) {
            item.calculateAmounts();
            sumPretax = sumPretax.add(item.getPretaxAmount());
            sumDiscount = sumDiscount.add(item.getDiscountAmount());
            sumTax = sumTax.add(item.getTaxAmount());
            sumTotal = sumTotal.add(item.getTotalAmount());
        }

        this.totalPretaxAmount = sumPretax.setScale(4, RoundingMode.HALF_UP);
        this.totalDiscountAmount = sumDiscount.setScale(4, RoundingMode.HALF_UP);
        this.totalTaxAmount = sumTax.setScale(4, RoundingMode.HALF_UP);
        this.totalAmount = sumTotal.setScale(4, RoundingMode.HALF_UP);
    }

    public void transitionTo(InvoiceStatus targetStatus, String action, String reason, String performedBy) {
        // Kiểm tra tính hợp lệ của chuyển trạng thái
        validateStateTransition(this.invoiceStatus, targetStatus);

        InvoiceAuditLog log = InvoiceAuditLog.builder()
                .invoice(this)
                .fromStatus(this.invoiceStatus)
                .toStatus(targetStatus)
                .action(action)
                .reason(reason)
                .performedBy(performedBy)
                .createdAt(Instant.now())
                .build();
        this.auditLogs.add(log);
        this.invoiceStatus = targetStatus;
    }

    private void validateStateTransition(InvoiceStatus current, InvoiceStatus target) {
        if (current == target) {
            return;
        }
        if (current == InvoiceStatus.ISSUED && target != InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("An ISSUED invoice can only transition to CANCELLED, got: " + target);
        }
        if (current == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Cannot transition from CANCELLED status");
        }
        if (current == InvoiceStatus.REJECTED && target != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("A REJECTED invoice can only be revised back to DRAFT");
        }
    }
}

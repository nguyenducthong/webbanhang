package com.fashion.platform.einvoice.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invoice_items",
       uniqueConstraints = @UniqueConstraint(name = "uq_invoice_item_line", columnNames = {"invoice_id", "line_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "quantity", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_rate", precision = 7, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO; // ví dụ: 0.08 hoặc 0.10

    @Column(name = "pretax_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal pretaxAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Tự động tính toán các trường tiền theo công thức chuẩn:
     * pretaxAmount = (quantity * unitPrice) - discountAmount
     * taxAmount = pretaxAmount * taxRate
     * totalAmount = pretaxAmount + taxAmount
     */
    public void calculateAmounts() {
        if (this.quantity == null) this.quantity = BigDecimal.ZERO;
        if (this.unitPrice == null) this.unitPrice = BigDecimal.ZERO;
        if (this.discountAmount == null) this.discountAmount = BigDecimal.ZERO;
        if (this.taxRate == null) this.taxRate = BigDecimal.ZERO;

        BigDecimal gross = this.quantity.multiply(this.unitPrice).setScale(4, RoundingMode.HALF_UP);
        this.pretaxAmount = gross.subtract(this.discountAmount).setScale(4, RoundingMode.HALF_UP);
        this.taxAmount = this.pretaxAmount.multiply(this.taxRate).setScale(4, RoundingMode.HALF_UP);
        this.totalAmount = this.pretaxAmount.add(this.taxAmount).setScale(4, RoundingMode.HALF_UP);
    }
}

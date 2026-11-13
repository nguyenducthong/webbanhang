package com.fashion.platform.einvoice.api.dto;

import com.fashion.platform.einvoice.domain.model.Invoice;
import com.fashion.platform.einvoice.domain.model.InvoiceStatus;
import com.fashion.platform.einvoice.domain.model.InvoiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private UUID id;
    private UUID legalEntityId;
    private UUID financeTransactionId;
    private InvoiceType invoiceType;
    private InvoiceStatus invoiceStatus;
    private String idempotencyKey;
    private String invoiceNumber;
    private String invoiceSeries;
    private String templateCode;
    private Instant issueDate;
    private String taxAuthorityCode;
    private String sellerSnapshot;
    private String buyerSnapshot;
    private BigDecimal totalPretaxAmount;
    private BigDecimal totalDiscountAmount;
    private BigDecimal totalTaxAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String notes;
    private String errorMessage;
    private List<ItemDto> items;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDto {
        private UUID id;
        private int lineNumber;
        private String itemName;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal discountAmount;
        private BigDecimal taxRate;
        private BigDecimal pretaxAmount;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
    }

    public static InvoiceResponse fromEntity(Invoice invoice) {
        List<ItemDto> itemDtos = invoice.getItems().stream()
                .map(i -> ItemDto.builder()
                        .id(i.getId())
                        .lineNumber(i.getLineNumber())
                        .itemName(i.getItemName())
                        .unit(i.getUnit())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .discountAmount(i.getDiscountAmount())
                        .taxRate(i.getTaxRate())
                        .pretaxAmount(i.getPretaxAmount())
                        .taxAmount(i.getTaxAmount())
                        .totalAmount(i.getTotalAmount())
                        .build())
                .toList();

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .legalEntityId(invoice.getLegalEntity().getId())
                .financeTransactionId(invoice.getFinanceTransaction() != null ? invoice.getFinanceTransaction().getId() : null)
                .invoiceType(invoice.getInvoiceType())
                .invoiceStatus(invoice.getInvoiceStatus())
                .idempotencyKey(invoice.getIdempotencyKey())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceSeries(invoice.getInvoiceSeries())
                .templateCode(invoice.getTemplateCode())
                .issueDate(invoice.getIssueDate())
                .taxAuthorityCode(invoice.getTaxAuthorityCode())
                .sellerSnapshot(invoice.getSellerSnapshot())
                .buyerSnapshot(invoice.getBuyerSnapshot())
                .totalPretaxAmount(invoice.getTotalPretaxAmount())
                .totalDiscountAmount(invoice.getTotalDiscountAmount())
                .totalTaxAmount(invoice.getTotalTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .notes(invoice.getNotes())
                .errorMessage(invoice.getErrorMessage())
                .items(itemDtos)
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }
}

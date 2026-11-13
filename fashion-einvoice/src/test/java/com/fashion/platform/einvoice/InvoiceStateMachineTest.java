package com.fashion.platform.einvoice;

import com.fashion.platform.einvoice.domain.model.*;
import com.fashion.platform.finance.domain.entity.LegalEntity;
import com.fashion.platform.finance.domain.entity.LegalEntityStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceStateMachineTest {

    @Test
    @DisplayName("Should successfully calculate amounts when items are added")
    void shouldCalculateAmountsCorrectly() {
        LegalEntity entity = LegalEntity.builder()
                .id(UUID.randomUUID())
                .entityCode("LE-01")
                .entityName("Fashion Corp")
                .status(LegalEntityStatus.ACTIVE)
                .build();

        Invoice invoice = Invoice.builder()
                .legalEntity(entity)
                .invoiceStatus(InvoiceStatus.DRAFT)
                .idempotencyKey("INV-TEST-001")
                .sellerSnapshot("{}")
                .buyerSnapshot("{}")
                .build();

        InvoiceItem item1 = InvoiceItem.builder()
                .lineNumber(1)
                .itemName("Ao Thun Cotton Trắng")
                .quantity(new BigDecimal("2.0000"))
                .unitPrice(new BigDecimal("150000.0000"))
                .discountAmount(new BigDecimal("20000.0000"))
                .taxRate(new BigDecimal("0.0800"))
                .build();

        invoice.addItem(item1);

        // gross = 300,000, discount = 20,000 -> pretax = 280,000
        // tax = 280,000 * 0.08 = 22,400 -> total = 302,400
        assertEquals(0, new BigDecimal("280000.0000").compareTo(invoice.getTotalPretaxAmount()));
        assertEquals(0, new BigDecimal("22400.0000").compareTo(invoice.getTotalTaxAmount()));
        assertEquals(0, new BigDecimal("302400.0000").compareTo(invoice.getTotalAmount()));
    }

    @Test
    @DisplayName("Should throw exception when modifying an ISSUED invoice")
    void shouldBlockModificationOnIssuedInvoice() {
        Invoice invoice = Invoice.builder()
                .invoiceStatus(InvoiceStatus.ISSUED)
                .idempotencyKey("INV-TEST-002")
                .sellerSnapshot("{}")
                .buyerSnapshot("{}")
                .build();

        InvoiceItem item = InvoiceItem.builder()
                .lineNumber(1)
                .itemName("Quan Jean Slim")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("500000.0000"))
                .taxRate(new BigDecimal("0.1000"))
                .build();

        assertThrows(IllegalStateException.class, () -> invoice.addItem(item));
        assertThrows(IllegalStateException.class, invoice::recalculateTotals);
    }

    @Test
    @DisplayName("Should validate state transitions correctly")
    void shouldValidateStateTransitions() {
        Invoice invoice = Invoice.builder()
                .invoiceStatus(InvoiceStatus.DRAFT)
                .idempotencyKey("INV-TEST-003")
                .sellerSnapshot("{}")
                .buyerSnapshot("{}")
                .build();

        // DRAFT -> VALIDATING -> PENDING_ISSUANCE -> SUBMITTED -> ISSUED
        invoice.transitionTo(InvoiceStatus.VALIDATING, "ACTION_VALIDATE", null, "admin");
        assertEquals(InvoiceStatus.VALIDATING, invoice.getInvoiceStatus());

        invoice.transitionTo(InvoiceStatus.PENDING_ISSUANCE, "ACTION_PASSED", null, "admin");
        assertEquals(InvoiceStatus.PENDING_ISSUANCE, invoice.getInvoiceStatus());

        invoice.transitionTo(InvoiceStatus.SUBMITTED, "ACTION_SUBMIT", null, "admin");
        assertEquals(InvoiceStatus.SUBMITTED, invoice.getInvoiceStatus());

        invoice.transitionTo(InvoiceStatus.ISSUED, "ACTION_ISSUED", null, "admin");
        assertEquals(InvoiceStatus.ISSUED, invoice.getInvoiceStatus());

        // ISSUED cannot transition back to DRAFT
        assertThrows(IllegalStateException.class, () ->
                invoice.transitionTo(InvoiceStatus.DRAFT, "INVALID_REVERT", null, "admin"));

        // ISSUED can only transition to CANCELLED
        assertDoesNotThrow(() ->
                invoice.transitionTo(InvoiceStatus.CANCELLED, "ACTION_CANCEL", "Customer cancelled", "admin"));
        assertEquals(InvoiceStatus.CANCELLED, invoice.getInvoiceStatus());

        // CANCELLED cannot transition anywhere
        assertThrows(IllegalStateException.class, () ->
                invoice.transitionTo(InvoiceStatus.DRAFT, "REVERT_CANCEL", null, "admin"));
    }
}

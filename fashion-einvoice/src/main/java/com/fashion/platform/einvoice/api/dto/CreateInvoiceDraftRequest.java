package com.fashion.platform.einvoice.api.dto;

import com.fashion.platform.einvoice.domain.model.BuyerSnapshot;
import com.fashion.platform.einvoice.domain.model.InvoiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceDraftRequest {

    @NotNull(message = "Legal entity ID is required")
    private UUID legalEntityId;

    private UUID financeTransactionId;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @Builder.Default
    private InvoiceType invoiceType = InvoiceType.VAT_INVOICE;

    @NotNull(message = "Buyer snapshot is required")
    private BuyerSnapshot buyerSnapshot;

    @NotEmpty(message = "Invoice must contain at least one item")
    @Valid
    private List<InvoiceItemDto> items;

    private String notes;
}

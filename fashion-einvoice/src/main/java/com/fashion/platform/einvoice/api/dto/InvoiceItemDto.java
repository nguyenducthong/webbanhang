package com.fashion.platform.einvoice.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemDto {
    private int lineNumber;

    @NotBlank(message = "Item name is required")
    private String itemName;

    private String unit;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0000", message = "Unit price must be non-negative")
    private BigDecimal unitPrice;

    private BigDecimal discountAmount;

    @NotNull(message = "Tax rate is required")
    private BigDecimal taxRate; // e.g. 0.08, 0.10
}

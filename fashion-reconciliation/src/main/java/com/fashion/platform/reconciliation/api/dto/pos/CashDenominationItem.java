package com.fashion.platform.reconciliation.api.dto.pos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashDenominationItem {
    private int denomination;
    private int quantity;
}

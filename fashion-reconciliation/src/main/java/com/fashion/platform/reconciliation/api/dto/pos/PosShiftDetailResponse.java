package com.fashion.platform.reconciliation.api.dto.pos;

import com.fashion.platform.reconciliation.domain.model.PosShift;
import com.fashion.platform.reconciliation.domain.model.PosShiftCashCount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosShiftDetailResponse {
    private PosShiftResponse shift;
    private List<CashCountItemResponse> cashCounts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashCountItemResponse {
        private int denomination;
        private int quantity;
        private BigDecimal totalAmount;

        public static CashCountItemResponse fromEntity(PosShiftCashCount count) {
            return CashCountItemResponse.builder()
                    .denomination(count.getDenomination())
                    .quantity(count.getQuantity())
                    .totalAmount(count.getTotalAmount())
                    .build();
        }
    }

    public static PosShiftDetailResponse of(PosShift shift, List<PosShiftCashCount> counts) {
        return PosShiftDetailResponse.builder()
                .shift(PosShiftResponse.fromEntity(shift))
                .cashCounts(counts.stream().map(CashCountItemResponse::fromEntity).toList())
                .build();
    }
}

package com.fashion.platform.finance.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReceivablePayableTest {

    @Test
    @DisplayName("Should update status to PARTIALLY_SETTLED and SETTLED accurately")
    void testSettlementFlow() {
        ReceivablePayable rp = ReceivablePayable.builder()
                .entryType(EntryType.RECEIVABLE)
                .partyType(PartyType.PLATFORM_SHOPEE)
                .partyId("SHOPEE_VN")
                .sourceType("ORDER")
                .sourceId("SHOPEE-ORD-01")
                .originalAmount(new BigDecimal("500000.0000"))
                .settledAmount(BigDecimal.ZERO)
                .status(ReceivableStatus.OPEN)
                .build();

        // Settle partially: 200,000 VND
        rp.settle(new BigDecimal("200000.0000"));
        assertThat(rp.getSettledAmount()).isEqualByComparingTo(new BigDecimal("200000.0000"));
        assertThat(rp.getRemainingAmount()).isEqualByComparingTo(new BigDecimal("300000.0000"));
        assertThat(rp.getStatus()).isEqualTo(ReceivableStatus.PARTIALLY_SETTLED);

        // Settle remaining: 300,000 VND
        rp.settle(new BigDecimal("300000.0000"));
        assertThat(rp.getSettledAmount()).isEqualByComparingTo(new BigDecimal("500000.0000"));
        assertThat(rp.getRemainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(rp.getStatus()).isEqualTo(ReceivableStatus.SETTLED);
    }

    @Test
    @DisplayName("Should throw exception when settlement exceeds original amount")
    void testOverSettlement() {
        ReceivablePayable rp = ReceivablePayable.builder()
                .originalAmount(new BigDecimal("100000.0000"))
                .settledAmount(BigDecimal.ZERO)
                .status(ReceivableStatus.OPEN)
                .build();

        assertThatThrownBy(() -> rp.settle(new BigDecimal("150000.0000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds original amount");
    }
}


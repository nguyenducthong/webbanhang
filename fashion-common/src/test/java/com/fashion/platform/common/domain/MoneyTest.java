package com.fashion.platform.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    @DisplayName("Should create Money with default scale of 4")
    void testMoneyCreationScale() {
        Money m = Money.vnd(50000);
        assertThat(m.getAmount()).isEqualByComparingTo(new BigDecimal("50000.0000"));
        assertThat(m.getCurrency()).isEqualTo(CurrencyCode.VND);
    }

    @Test
    @DisplayName("Should add money with same currency correctly")
    void testMoneyAdd() {
        Money m1 = Money.vnd(100000);
        Money m2 = Money.vnd(25000);
        Money sum = m1.add(m2);

        assertThat(sum.getAmount()).isEqualByComparingTo(new BigDecimal("125000.0000"));
        assertThat(sum.getCurrency()).isEqualTo(CurrencyCode.VND);
    }

    @Test
    @DisplayName("Should throw CurrencyMismatchException when adding different currencies")
    void testCurrencyMismatchOnAdd() {
        Money vnd = Money.vnd(100000);
        Money usd = Money.of(BigDecimal.valueOf(10), CurrencyCode.USD);

        assertThatThrownBy(() -> vnd.add(usd))
                .isInstanceOf(CurrencyMismatchException.class)
                .hasMessageContaining("Currency mismatch: expected VND but was USD");
    }

    @Test
    @DisplayName("Should multiply and divide with high precision without floating-point artifacts")
    void testMultiplyAndDivide() {
        Money price = Money.vnd(100000);
        Money tax = price.multiply(new BigDecimal("0.08")); // 8% VAT
        assertThat(tax.getAmount()).isEqualByComparingTo(new BigDecimal("8000.0000"));

        Money share = price.divide(new BigDecimal("3"));
        assertThat(share.getAmount()).isEqualByComparingTo(new BigDecimal("33333.3333"));
    }

    @Test
    @DisplayName("Should compare money values properly")
    void testComparison() {
        Money m1 = Money.vnd(100);
        Money m2 = Money.vnd(200);

        assertThat(m1.isLessThan(m2)).isTrue();
        assertThat(m2.isGreaterThan(m1)).isTrue();
        assertThat(Money.zero(CurrencyCode.VND).isZero()).isTrue();
    }
}


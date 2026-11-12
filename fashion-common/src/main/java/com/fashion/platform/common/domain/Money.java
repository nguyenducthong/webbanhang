package com.fashion.platform.common.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable Money Value Object.
 * Preserves high precision internally (scale = 4) to eliminate floating-point rounding errors.
 */
@Getter
@EqualsAndHashCode
public final class Money implements Serializable, Comparable<Money> {

    public static final int INTERNAL_SCALE = 4;
    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;

    private final BigDecimal amount;
    private final CurrencyCode currency;

    @JsonCreator
    public Money(@JsonProperty("amount") BigDecimal amount,
                 @JsonProperty("currency") CurrencyCode currency) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        this.amount = amount.setScale(INTERNAL_SCALE, DEFAULT_ROUNDING);
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, CurrencyCode currency) {
        return new Money(amount, currency);
    }

    public static Money of(long amount, CurrencyCode currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public static Money of(double amount, CurrencyCode currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public static Money zero(CurrencyCode currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money vnd(long amount) {
        return of(amount, CurrencyCode.VND);
    }

    public static Money vnd(BigDecimal amount) {
        return of(amount, CurrencyCode.VND);
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(BigDecimal multiplicand) {
        Objects.requireNonNull(multiplicand, "Multiplicand cannot be null");
        return new Money(this.amount.multiply(multiplicand), this.currency);
    }

    public Money multiply(long multiplier) {
        return multiply(BigDecimal.valueOf(multiplier));
    }

    public Money divide(BigDecimal divisor) {
        Objects.requireNonNull(divisor, "Divisor cannot be null");
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Cannot divide money by zero");
        }
        return new Money(this.amount.divide(divisor, INTERNAL_SCALE, DEFAULT_ROUNDING), this.currency);
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return this.compareTo(other) > 0;
    }

    public boolean isLessThan(Money other) {
        assertSameCurrency(other);
        return this.compareTo(other) < 0;
    }

    private void assertSameCurrency(Money other) {
        Objects.requireNonNull(other, "Money operand cannot be null");
        if (this.currency != other.currency) {
            throw new CurrencyMismatchException(this.currency, other.currency);
        }
    }

    @Override
    public int compareTo(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.name();
    }
}


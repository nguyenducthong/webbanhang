package com.fashion.platform.common.domain;

public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException(CurrencyCode expected, CurrencyCode actual) {
        super(String.format("Currency mismatch: expected %s but was %s", expected, actual));
    }

    public CurrencyMismatchException(String message) {
        super(message);
    }
}


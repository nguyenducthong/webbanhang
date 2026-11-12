package com.fashion.platform.common.domain;

public enum CurrencyCode {
    VND(0),
    USD(2),
    EUR(2);

    private final int defaultDisplayFractionDigits;

    CurrencyCode(int defaultDisplayFractionDigits) {
        this.defaultDisplayFractionDigits = defaultDisplayFractionDigits;
    }

    public int getDefaultDisplayFractionDigits() {
        return defaultDisplayFractionDigits;
    }
}


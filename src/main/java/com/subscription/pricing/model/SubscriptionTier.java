package com.subscription.pricing.model;

import java.math.BigDecimal;

/**
 * Represents available subscription tiers and their monthly base rates.
 */
public enum SubscriptionTier {
    BASIC(new BigDecimal("50.00")),
    PRO(new BigDecimal("150.00")),
    ENTERPRISE(new BigDecimal("500.00"));

    private final BigDecimal basePrice;

    SubscriptionTier(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }
}

package com.subscription.pricing.model;

import java.math.BigDecimal;

/**
 * Modern Java 17 record holding the itemized breakdown of subscription pricing calculation.
 */
public record PricingResult(
        SubscriptionTier tier,
        BigDecimal baseRate,
        int activeMonths,
        BigDecimal longevityDiscountPercentage,
        BigDecimal longevityDiscountAmount,
        BigDecimal rateAfterLongevityDiscount,
        String voucherCode,
        BigDecimal voucherDiscountAmount,
        BigDecimal finalPrice
) {
    // Backward-compatible JavaBean getters for seamless framework and API integration
    public SubscriptionTier getTier() {
        return tier();
    }

    public BigDecimal getBaseRate() {
        return baseRate();
    }

    public int getActiveMonths() {
        return activeMonths();
    }

    public BigDecimal getLongevityDiscountPercentage() {
        return longevityDiscountPercentage();
    }

    public BigDecimal getLongevityDiscountAmount() {
        return longevityDiscountAmount();
    }

    public BigDecimal getRateAfterLongevityDiscount() {
        return rateAfterLongevityDiscount();
    }

    public String getVoucherCode() {
        return voucherCode();
    }

    public BigDecimal getVoucherDiscountAmount() {
        return voucherDiscountAmount();
    }

    public BigDecimal getFinalPrice() {
        return finalPrice();
    }
}

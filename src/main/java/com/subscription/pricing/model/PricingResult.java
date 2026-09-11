package com.subscription.pricing.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable value object holding the itemized breakdown of subscription pricing calculation.
 */
public class PricingResult {
    private final SubscriptionTier tier;
    private final BigDecimal baseRate;
    private final int activeMonths;
    private final BigDecimal longevityDiscountPercentage;
    private final BigDecimal longevityDiscountAmount;
    private final BigDecimal rateAfterLongevityDiscount;
    private final String voucherCode;
    private final BigDecimal voucherDiscountAmount;
    private final BigDecimal finalPrice;

    public PricingResult(SubscriptionTier tier,
                         BigDecimal baseRate,
                         int activeMonths,
                         BigDecimal longevityDiscountPercentage,
                         BigDecimal longevityDiscountAmount,
                         BigDecimal rateAfterLongevityDiscount,
                         String voucherCode,
                         BigDecimal voucherDiscountAmount,
                         BigDecimal finalPrice) {
        this.tier = tier;
        this.baseRate = baseRate;
        this.activeMonths = activeMonths;
        this.longevityDiscountPercentage = longevityDiscountPercentage;
        this.longevityDiscountAmount = longevityDiscountAmount;
        this.rateAfterLongevityDiscount = rateAfterLongevityDiscount;
        this.voucherCode = voucherCode;
        this.voucherDiscountAmount = voucherDiscountAmount;
        this.finalPrice = finalPrice;
    }

    public SubscriptionTier getTier() {
        return tier;
    }

    public BigDecimal getBaseRate() {
        return baseRate;
    }

    public int getActiveMonths() {
        return activeMonths;
    }

    public BigDecimal getLongevityDiscountPercentage() {
        return longevityDiscountPercentage;
    }

    public BigDecimal getLongevityDiscountAmount() {
        return longevityDiscountAmount;
    }

    public BigDecimal getRateAfterLongevityDiscount() {
        return rateAfterLongevityDiscount;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public BigDecimal getVoucherDiscountAmount() {
        return voucherDiscountAmount;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PricingResult that = (PricingResult) o;
        return activeMonths == that.activeMonths &&
                tier == that.tier &&
                Objects.equals(baseRate, that.baseRate) &&
                Objects.equals(longevityDiscountPercentage, that.longevityDiscountPercentage) &&
                Objects.equals(longevityDiscountAmount, that.longevityDiscountAmount) &&
                Objects.equals(rateAfterLongevityDiscount, that.rateAfterLongevityDiscount) &&
                Objects.equals(voucherCode, that.voucherCode) &&
                Objects.equals(voucherDiscountAmount, that.voucherDiscountAmount) &&
                Objects.equals(finalPrice, that.finalPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tier, baseRate, activeMonths, longevityDiscountPercentage,
                longevityDiscountAmount, rateAfterLongevityDiscount, voucherCode,
                voucherDiscountAmount, finalPrice);
    }

    @Override
    public String toString() {
        return "PricingResult{" +
                "tier=" + tier +
                ", baseRate=" + baseRate +
                ", activeMonths=" + activeMonths +
                ", longevityDiscountPercentage=" + longevityDiscountPercentage +
                ", longevityDiscountAmount=" + longevityDiscountAmount +
                ", rateAfterLongevityDiscount=" + rateAfterLongevityDiscount +
                ", voucherCode='" + voucherCode + '\'' +
                ", voucherDiscountAmount=" + voucherDiscountAmount +
                ", finalPrice=" + finalPrice +
                '}';
    }
}

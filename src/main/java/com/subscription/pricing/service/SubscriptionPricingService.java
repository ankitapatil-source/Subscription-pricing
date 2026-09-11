package com.subscription.pricing.service;

import com.subscription.pricing.exception.InvalidVoucherException;
import com.subscription.pricing.model.PricingResult;
import com.subscription.pricing.model.SubscriptionTier;
import com.subscription.pricing.model.Voucher;
import com.subscription.pricing.repository.InMemoryVoucherRepository;
import com.subscription.pricing.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Production-grade service responsible for processing subscription tier rates,
 * applying longevity discounts, promotional voucher codes, and enforcing
 * rounding and zero-floor constraints.
 * Refactored using modern Java 17 switch expressions and records.
 */
@Service
public class SubscriptionPricingService {

    private static final BigDecimal TEN_PERCENT = new BigDecimal("0.10");
    private static final BigDecimal TWENTY_FIVE_PERCENT = new BigDecimal("0.25");
    private static final BigDecimal ZERO_PERCENT = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal ZERO_DOLLARS = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final VoucherRepository voucherRepository;
    private final Clock clock;

    /**
     * Default constructor using InMemoryVoucherRepository and system clock.
     * Enables zero-configuration instantiation in plain Java tests.
     */
    public SubscriptionPricingService() {
        this(new InMemoryVoucherRepository(), Clock.systemDefaultZone());
    }

    /**
     * Constructor with custom VoucherRepository and system clock.
     *
     * @param voucherRepository repository for looking up promotional vouchers
     */
    public SubscriptionPricingService(VoucherRepository voucherRepository) {
        this(voucherRepository, Clock.systemDefaultZone());
    }

    /**
     * Full constructor for dependency injection and clock mocking in tests.
     *
     * @param voucherRepository repository for looking up promotional vouchers
     * @param clock             clock used for evaluating voucher expiration
     */
    @Autowired
    public SubscriptionPricingService(VoucherRepository voucherRepository, Clock clock) {
        this.voucherRepository = Objects.requireNonNull(voucherRepository, "VoucherRepository cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    /**
     * Calculates the monthly subscription price without promotional vouchers.
     *
     * @param tier         subscription tier (BASIC, PRO, ENTERPRISE)
     * @param activeMonths number of months the account has been active
     * @return final calculated monthly price, rounded to 2 decimal places with half-up rounding
     */
    public BigDecimal calculatePrice(SubscriptionTier tier, int activeMonths) {
        return calculatePrice(tier, activeMonths, null);
    }

    /**
     * Calculates the monthly subscription price with an optional promotional voucher code.
     *
     * @param tier         subscription tier (BASIC, PRO, ENTERPRISE)
     * @param activeMonths number of months the account has been active
     * @param voucherCode  promotional voucher code, or null/blank if none
     * @return final calculated monthly price, rounded to 2 decimal places with half-up rounding
     * @throws IllegalArgumentException if tier is null or activeMonths is negative
     * @throws InvalidVoucherException  if the voucher code is unrecognized or expired
     */
    public BigDecimal calculatePrice(SubscriptionTier tier, int activeMonths, String voucherCode) {
        return calculateDetailedPrice(tier, activeMonths, voucherCode).finalPrice();
    }

    /**
     * Alias method for calculatePrice(tier, activeMonths).
     */
    public BigDecimal calculateMonthlyPrice(SubscriptionTier tier, int activeMonths) {
        return calculatePrice(tier, activeMonths, null);
    }

    /**
     * Alias method for calculatePrice(tier, activeMonths, voucherCode).
     */
    public BigDecimal calculateMonthlyPrice(SubscriptionTier tier, int activeMonths, String voucherCode) {
        return calculatePrice(tier, activeMonths, voucherCode);
    }

    /**
     * Computes the detailed, itemized breakdown of the pricing calculation without vouchers.
     *
     * @param tier         subscription tier (BASIC, PRO, ENTERPRISE)
     * @param activeMonths number of months the account has been active
     * @return itemized PricingResult
     */
    public PricingResult calculateDetailedPrice(SubscriptionTier tier, int activeMonths) {
        return calculateDetailedPrice(tier, activeMonths, null);
    }

    /**
     * Computes the detailed, itemized breakdown of the pricing calculation.
     * Refactored using Java 17 pattern matching and switch expressions.
     *
     * @param tier         subscription tier (BASIC, PRO, ENTERPRISE)
     * @param activeMonths number of months the account has been active
     * @param voucherCode  promotional voucher code, or null/blank if none
     * @return itemized PricingResult
     * @throws IllegalArgumentException if tier is null or activeMonths is negative
     * @throws InvalidVoucherException  if the voucher code is unrecognized or expired
     */
    public PricingResult calculateDetailedPrice(SubscriptionTier tier, int activeMonths, String voucherCode) {
        if (tier == null) {
            throw new IllegalArgumentException("Subscription tier cannot be null");
        }
        if (activeMonths < 0) {
            throw new IllegalArgumentException("Active months cannot be negative: " + activeMonths);
        }

        BigDecimal baseRate = tier.getBasePrice().setScale(2, RoundingMode.HALF_UP);

        // 1. Resolve longevity discount percentage via clean pattern
        BigDecimal longevityDiscountPercentage = resolveLongevityDiscountPercentage(activeMonths);

        BigDecimal longevityDiscountAmount = baseRate
                .multiply(longevityDiscountPercentage)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal rateAfterLongevityDiscount = baseRate
                .subtract(longevityDiscountAmount)
                .setScale(2, RoundingMode.HALF_UP);

        // 2. Evaluate promotional voucher code using modern Java 17 switch expression
        String normalizedVoucherCode = (voucherCode != null && !voucherCode.isBlank())
                ? voucherCode.strip().toUpperCase()
                : null;

        BigDecimal voucherDiscountAmount = ZERO_DOLLARS;
        BigDecimal calculatedPrice = rateAfterLongevityDiscount;

        if (normalizedVoucherCode != null) {
            Voucher voucher = voucherRepository.findByCode(normalizedVoucherCode)
                    .orElseThrow(() -> new InvalidVoucherException("Invalid voucher code: " + voucherCode));

            LocalDate today = LocalDate.now(clock);
            if (voucher.isExpired(today)) {
                throw new InvalidVoucherException("Voucher is expired: " + voucherCode);
            }

            // Java 17 enhanced switch expression replacing procedural if-else statements
            voucherDiscountAmount = switch (voucher.getType()) {
                case FLAT -> voucher.getValue().setScale(2, RoundingMode.HALF_UP);
                case PERCENTAGE -> rateAfterLongevityDiscount
                        .multiply(voucher.getValue())
                        .setScale(2, RoundingMode.HALF_UP);
            };

            calculatedPrice = rateAfterLongevityDiscount.subtract(voucherDiscountAmount);
        }

        // 3. Enforce Rounding & Zero-Floor Rule using BigDecimal.max
        BigDecimal finalPrice = calculatedPrice
                .setScale(2, RoundingMode.HALF_UP)
                .max(ZERO_DOLLARS);

        return new PricingResult(
                tier,
                baseRate,
                activeMonths,
                longevityDiscountPercentage,
                longevityDiscountAmount,
                rateAfterLongevityDiscount,
                normalizedVoucherCode,
                voucherDiscountAmount,
                finalPrice
        );
    }

    /**
     * Resolves the longevity discount percentage based on active account months.
     *
     * @param activeMonths active months on account
     * @return discount percentage BigDecimal
     */
    private BigDecimal resolveLongevityDiscountPercentage(int activeMonths) {
        if (activeMonths > 36) {
            return TWENTY_FIVE_PERCENT;
        }
        if (activeMonths > 12) {
            return TEN_PERCENT;
        }
        return ZERO_PERCENT;
    }
}

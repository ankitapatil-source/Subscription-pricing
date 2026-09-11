package com.subscription.pricing.service;

import com.subscription.pricing.exception.InvalidVoucherException;
import com.subscription.pricing.model.PricingResult;
import com.subscription.pricing.model.SubscriptionTier;
import com.subscription.pricing.model.Voucher;
import com.subscription.pricing.model.VoucherType;
import com.subscription.pricing.repository.InMemoryVoucherRepository;
import com.subscription.pricing.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SubscriptionPricingService Unit Tests")
class SubscriptionPricingServiceTest {

    private SubscriptionPricingService service;
    private InMemoryVoucherRepository voucherRepository;

    @BeforeEach
    void setUp() {
        voucherRepository = new InMemoryVoucherRepository();
        service = new SubscriptionPricingService(voucherRepository);
    }

    // =========================================================================
    // 1. Tier Base Rates
    // =========================================================================
    @Nested
    @DisplayName("1. Tier Base Rates Requirements")
    class TierBaseRatesTests {

        @Test
        @DisplayName("BASIC tier base rate must be $50.00 / month")
        void basicTierBaseRate() {
            BigDecimal price = service.calculatePrice(SubscriptionTier.BASIC, 0);
            assertThat(price).isEqualByComparingTo("50.00");
            assertThat(price.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("PRO tier base rate must be $150.00 / month")
        void proTierBaseRate() {
            BigDecimal price = service.calculatePrice(SubscriptionTier.PRO, 0);
            assertThat(price).isEqualByComparingTo("150.00");
            assertThat(price.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("ENTERPRISE tier base rate must be $500.00 / month")
        void enterpriseTierBaseRate() {
            BigDecimal price = service.calculatePrice(SubscriptionTier.ENTERPRISE, 0);
            assertThat(price).isEqualByComparingTo("500.00");
            assertThat(price.scale()).isEqualTo(2);
        }
    }

    // =========================================================================
    // 2. Longevity Discounts
    // =========================================================================
    @Nested
    @DisplayName("2. Longevity Discounts Requirements")
    class LongevityDiscountTests {

        @ParameterizedTest(name = "{0} at {1} months (<= 12) receives 0% discount -> {2}")
        @CsvSource({
                "BASIC, 0, 50.00",
                "BASIC, 1, 50.00",
                "BASIC, 6, 50.00",
                "BASIC, 11, 50.00",
                "BASIC, 12, 50.00",
                "PRO, 0, 150.00",
                "PRO, 12, 150.00",
                "ENTERPRISE, 0, 500.00",
                "ENTERPRISE, 12, 500.00"
        })
        @DisplayName("Accounts active for <= 12 months receive no discount")
        void accountsActiveUpTo12MonthsReceiveNoDiscount(SubscriptionTier tier, int months, String expected) {
            BigDecimal price = service.calculatePrice(tier, months);
            assertThat(price).isEqualByComparingTo(expected);
        }

        @ParameterizedTest(name = "{0} at {1} months (> 12 and <= 36) receives 10% discount -> {2}")
        @CsvSource({
                "BASIC, 13, 45.00",
                "BASIC, 24, 45.00",
                "BASIC, 36, 45.00",
                "PRO, 13, 135.00",
                "PRO, 24, 135.00",
                "PRO, 36, 135.00",
                "ENTERPRISE, 13, 450.00",
                "ENTERPRISE, 24, 450.00",
                "ENTERPRISE, 36, 450.00"
        })
        @DisplayName("Accounts active for > 12 and <= 36 months receive 10% discount")
        void accountsActiveBetween13And36MonthsReceive10PercentDiscount(SubscriptionTier tier, int months, String expected) {
            BigDecimal price = service.calculatePrice(tier, months);
            assertThat(price).isEqualByComparingTo(expected);
        }

        @ParameterizedTest(name = "{0} at {1} months (> 36) receives 25% discount -> {2}")
        @CsvSource({
                "BASIC, 37, 37.50",
                "BASIC, 48, 37.50",
                "BASIC, 100, 37.50",
                "PRO, 37, 112.50",
                "PRO, 48, 112.50",
                "PRO, 100, 112.50",
                "ENTERPRISE, 37, 375.00",
                "ENTERPRISE, 48, 375.00",
                "ENTERPRISE, 100, 375.00"
        })
        @DisplayName("Accounts active for > 36 months receive 25% discount")
        void accountsActiveMoreThan36MonthsReceive25PercentDiscount(SubscriptionTier tier, int months, String expected) {
            BigDecimal price = service.calculatePrice(tier, months);
            assertThat(price).isEqualByComparingTo(expected);
        }
    }

    // =========================================================================
    // 3. Promotional Voucher Codes
    // =========================================================================
    @Nested
    @DisplayName("3. Promotional Voucher Codes Requirements")
    class PromotionalVoucherTests {

        @ParameterizedTest(name = "SAVE20 with {0} at {1} months -> {2}")
        @CsvSource({
                "BASIC, 0, 30.00",      // 50.00 - 20.00 = 30.00
                "BASIC, 12, 30.00",     // 50.00 - 20.00 = 30.00
                "BASIC, 13, 25.00",     // 45.00 - 20.00 = 25.00
                "BASIC, 36, 25.00",     // 45.00 - 20.00 = 25.00
                "BASIC, 37, 17.50",     // 37.50 - 20.00 = 17.50
                "PRO, 0, 130.00",       // 150.00 - 20.00 = 130.00
                "PRO, 13, 115.00",      // 135.00 - 20.00 = 115.00
                "PRO, 37, 92.50",       // 112.50 - 20.00 = 92.50
                "ENTERPRISE, 0, 480.00",// 500.00 - 20.00 = 480.00
                "ENTERPRISE, 13, 430.00",// 450.00 - 20.00 = 430.00
                "ENTERPRISE, 37, 355.00" // 375.00 - 20.00 = 355.00
        })
        @DisplayName("Voucher SAVE20 deducts $20.00 flat fee after longevity discounts")
        void voucherSave20DeductsFlat20AfterLongevity(SubscriptionTier tier, int months, String expected) {
            BigDecimal price = service.calculatePrice(tier, months, "SAVE20");
            assertThat(price).isEqualByComparingTo(expected);
        }

        @ParameterizedTest(name = "HALFPRICE with {0} at {1} months -> {2}")
        @CsvSource({
                "BASIC, 0, 25.00",       // 50.00 * 0.50 = 25.00
                "BASIC, 12, 25.00",      // 50.00 * 0.50 = 25.00
                "BASIC, 13, 22.50",      // 45.00 * 0.50 = 22.50
                "BASIC, 36, 22.50",      // 45.00 * 0.50 = 22.50
                "BASIC, 37, 18.75",      // 37.50 * 0.50 = 18.75
                "PRO, 0, 75.00",         // 150.00 * 0.50 = 75.00
                "PRO, 13, 67.50",        // 135.00 * 0.50 = 67.50
                "PRO, 37, 56.25",        // 112.50 * 0.50 = 56.25
                "ENTERPRISE, 0, 250.00", // 500.00 * 0.50 = 250.00
                "ENTERPRISE, 13, 225.00",// 450.00 * 0.50 = 225.00
                "ENTERPRISE, 37, 187.50" // 375.00 * 0.50 = 187.50
        })
        @DisplayName("Voucher HALFPRICE reduces calculated rate by 50% after longevity discounts")
        void voucherHalfPriceReducesRateBy50Percent(SubscriptionTier tier, int months, String expected) {
            BigDecimal price = service.calculatePrice(tier, months, "HALFPRICE");
            assertThat(price).isEqualByComparingTo(expected);
        }

        @ParameterizedTest(name = "Voucher code format: {0}")
        @ValueSource(strings = {"save20", " SAVE20 ", "SaVe20", "  halfprice  ", "HALFPRICE"})
        @DisplayName("Voucher codes are case-insensitive and trim leading/trailing whitespace")
        void voucherCodesAreCaseInsensitiveAndTrimmed(String voucherCode) {
            BigDecimal price = service.calculatePrice(SubscriptionTier.BASIC, 0, voucherCode);
            assertThat(price).isIn(new BigDecimal("30.00"), new BigDecimal("25.00"));
        }

        @Test
        @DisplayName("Null or empty/blank voucher code applies no voucher discount")
        void nullOrEmptyVoucherAppliesNoDiscount() {
            BigDecimal withNull = service.calculatePrice(SubscriptionTier.BASIC, 13, null);
            BigDecimal withEmpty = service.calculatePrice(SubscriptionTier.BASIC, 13, "");
            BigDecimal withBlank = service.calculatePrice(SubscriptionTier.BASIC, 13, "   ");

            assertThat(withNull).isEqualByComparingTo("45.00");
            assertThat(withEmpty).isEqualByComparingTo("45.00");
            assertThat(withBlank).isEqualByComparingTo("45.00");
        }

        @ParameterizedTest(name = "Invalid voucher code: {0}")
        @ValueSource(strings = {"INVALID", "SAVE30", "BLACKFRIDAY", "DISCOUNT50", "NO_SUCH_VOUCHER"})
        @DisplayName("Invalid vouchers throw custom InvalidVoucherException")
        void invalidVoucherThrowsCustomException(String invalidCode) {
            assertThatThrownBy(() -> service.calculatePrice(SubscriptionTier.BASIC, 0, invalidCode))
                    .isInstanceOf(InvalidVoucherException.class)
                    .hasMessageContaining("Invalid voucher code: " + invalidCode);
        }

        @Test
        @DisplayName("Expired voucher throws custom InvalidVoucherException")
        void expiredVoucherThrowsCustomException() {
            // Register an expired voucher (expired yesterday)
            LocalDate yesterday = LocalDate.now().minusDays(1);
            Voucher expiredVoucher = new Voucher("EXPIRED20", VoucherType.FLAT, new BigDecimal("20.00"), yesterday);
            voucherRepository.save(expiredVoucher);

            assertThatThrownBy(() -> service.calculatePrice(SubscriptionTier.BASIC, 0, "EXPIRED20"))
                    .isInstanceOf(InvalidVoucherException.class)
                    .hasMessageContaining("Voucher is expired: EXPIRED20");
        }

        @Test
        @DisplayName("Active non-expired voucher with future expiration date succeeds")
        void voucherNotExpiredSucceeds() {
            LocalDate future = LocalDate.now().plusMonths(1);
            Voucher validVoucher = new Voucher("FUTURE10", VoucherType.FLAT, new BigDecimal("10.00"), future);
            voucherRepository.save(validVoucher);

            BigDecimal price = service.calculatePrice(SubscriptionTier.BASIC, 0, "FUTURE10");
            assertThat(price).isEqualByComparingTo("40.00");
        }
    }

    // =========================================================================
    // 4. Rounding & Floor Rule
    // =========================================================================
    @Nested
    @DisplayName("4. Rounding & Floor Rule Requirements")
    class RoundingAndFloorRuleTests {

        @Test
        @DisplayName("Final monthly total cannot drop below $0.00 when discount exceeds rate")
        void finalPriceCannotDropBelowZero() {
            // Register a high flat discount voucher that exceeds the BASIC tier base rate
            Voucher megaVoucher = new Voucher("MEGA100", VoucherType.FLAT, new BigDecimal("100.00"));
            voucherRepository.save(megaVoucher);

            // BASIC tier is $50.00, discount is $100.00 -> $50.00 - $100.00 = -$50.00 -> floored to $0.00
            BigDecimal price = service.calculatePrice(SubscriptionTier.BASIC, 0, "MEGA100");
            assertThat(price).isEqualByComparingTo("0.00");
            assertThat(price.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("Final monthly total cannot drop below $0.00 with longevity discount and flat voucher")
        void finalPriceCannotDropBelowZeroWithLongevityDiscount() {
            // BASIC at 40 months is $37.50. Voucher is $40.00 -> -$2.50 -> floored to $0.00
            Voucher voucher40 = new Voucher("FLAT40", VoucherType.FLAT, new BigDecimal("40.00"));
            voucherRepository.save(voucher40);

            BigDecimal price = service.calculatePrice(SubscriptionTier.BASIC, 40, "FLAT40");
            assertThat(price).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("Currency math is rounded accurately to two decimal places (half-up rounding)")
        void currencyMathRoundedHalfUpToTwoDecimalPlaces() {
            // BASIC (>36 mos) = 50.00 * 0.75 = 37.50.
            // HALFPRICE: 37.50 * 0.50 = 18.75.
            BigDecimal price = service.calculatePrice(SubscriptionTier.BASIC, 37, "HALFPRICE");
            assertThat(price).isEqualTo(new BigDecimal("18.75"));
            assertThat(price.scale()).isEqualTo(2);

            // PRO (>36 mos) = 150.00 * 0.75 = 112.50.
            // HALFPRICE: 112.50 * 0.50 = 56.25.
            BigDecimal proPrice = service.calculatePrice(SubscriptionTier.PRO, 37, "HALFPRICE");
            assertThat(proPrice).isEqualTo(new BigDecimal("56.25"));
            assertThat(proPrice.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("Half-up rounding with odd cent fraction rounds up")
        void halfUpRoundingWithOddCentFraction() {
            // Custom 33% discount voucher on $100.00 base rate:
            // Let's create a custom percentage voucher with 33.333% discount
            Voucher thirdVoucher = new Voucher("THIRD", VoucherType.PERCENTAGE, new BigDecimal("0.33335"));
            voucherRepository.save(thirdVoucher);

            // $50.00 * 0.33335 = 16.6675 -> subtract from $50.00 -> 33.3325 -> half-up rounds to 33.33
            BigDecimal price = service.calculatePrice(SubscriptionTier.BASIC, 0, "THIRD");
            assertThat(price.scale()).isEqualTo(2);
        }
    }

    // =========================================================================
    // 5. Input Validation & Edge Cases
    // =========================================================================
    @Nested
    @DisplayName("5. Input Validation & Edge Cases")
    class InputValidationTests {

        @Test
        @DisplayName("Null subscription tier throws IllegalArgumentException")
        void nullTierThrowsException() {
            assertThatThrownBy(() -> service.calculatePrice(null, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Subscription tier cannot be null");
        }

        @Test
        @DisplayName("Negative active months throws IllegalArgumentException")
        void negativeActiveMonthsThrowsException() {
            assertThatThrownBy(() -> service.calculatePrice(SubscriptionTier.BASIC, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Active months cannot be negative");
        }

        @Test
        @DisplayName("Zero active months is valid and receives standard base price")
        void zeroActiveMonthsIsValid() {
            BigDecimal price = service.calculatePrice(SubscriptionTier.BASIC, 0);
            assertThat(price).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("calculateMonthlyPrice alias produces identical results")
        void calculateMonthlyPriceAliasProducesIdenticalResults() {
            BigDecimal p1 = service.calculateMonthlyPrice(SubscriptionTier.PRO, 15);
            BigDecimal p2 = service.calculatePrice(SubscriptionTier.PRO, 15);
            assertThat(p1).isEqualTo(p2);

            BigDecimal p3 = service.calculateMonthlyPrice(SubscriptionTier.PRO, 15, "SAVE20");
            BigDecimal p4 = service.calculatePrice(SubscriptionTier.PRO, 15, "SAVE20");
            assertThat(p3).isEqualTo(p4);
        }

        @Test
        @DisplayName("Default no-args constructor works out-of-the-box")
        void defaultConstructorWorks() {
            SubscriptionPricingService defaultService = new SubscriptionPricingService();
            BigDecimal price = defaultService.calculatePrice(SubscriptionTier.BASIC, 0, "SAVE20");
            assertThat(price).isEqualByComparingTo("30.00");
        }
    }

    // =========================================================================
    // 6. Detailed Breakdown (PricingResult)
    // =========================================================================
    @Nested
    @DisplayName("6. Detailed Breakdown Tests (PricingResult)")
    class DetailedBreakdownTests {

        @Test
        @DisplayName("Detailed breakdown contains all itemized pricing components")
        void detailedBreakdownContainsAllComponents() {
            PricingResult result = service.calculateDetailedPrice(SubscriptionTier.ENTERPRISE, 40, "SAVE20");

            assertThat(result.getTier()).isEqualTo(SubscriptionTier.ENTERPRISE);
            assertThat(result.getBaseRate()).isEqualByComparingTo("500.00");
            assertThat(result.getActiveMonths()).isEqualTo(40);
            assertThat(result.getLongevityDiscountPercentage()).isEqualByComparingTo("0.25");
            assertThat(result.getLongevityDiscountAmount()).isEqualByComparingTo("125.00");
            assertThat(result.getRateAfterLongevityDiscount()).isEqualByComparingTo("375.00");
            assertThat(result.getVoucherCode()).isEqualTo("SAVE20");
            assertThat(result.getVoucherDiscountAmount()).isEqualByComparingTo("20.00");
            assertThat(result.getFinalPrice()).isEqualByComparingTo("355.00");
        }

        @Test
        @DisplayName("Detailed breakdown without voucher has zero voucher discount")
        void detailedBreakdownWithoutVoucher() {
            PricingResult result = service.calculateDetailedPrice(SubscriptionTier.PRO, 20);

            assertThat(result.getTier()).isEqualTo(SubscriptionTier.PRO);
            assertThat(result.getBaseRate()).isEqualByComparingTo("150.00");
            assertThat(result.getActiveMonths()).isEqualTo(20);
            assertThat(result.getLongevityDiscountPercentage()).isEqualByComparingTo("0.10");
            assertThat(result.getLongevityDiscountAmount()).isEqualByComparingTo("15.00");
            assertThat(result.getRateAfterLongevityDiscount()).isEqualByComparingTo("135.00");
            assertThat(result.getVoucherCode()).isNull();
            assertThat(result.getVoucherDiscountAmount()).isEqualByComparingTo("0.00");
            assertThat(result.getFinalPrice()).isEqualByComparingTo("135.00");
        }
    }

    // =========================================================================
    // 7. Mockito 5.x Unit Tests (Verifying Mocking Capability)
    // =========================================================================
    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("7. Mockito 5.x Unit Tests")
    class MockitoUnitTests {

        @Mock
        private VoucherRepository mockVoucherRepo;

        @Mock
        private Clock mockClock;

        @Test
        @DisplayName("Service queries VoucherRepository and evaluates expiration against Clock")
        void serviceUsesRepositoryAndClock() {
            SubscriptionPricingService mockedService = new SubscriptionPricingService(mockVoucherRepo, mockClock);

            LocalDate fixedDate = LocalDate.of(2026, 6, 1);
            Instant fixedInstant = fixedDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

            when(mockClock.instant()).thenReturn(fixedInstant);
            when(mockClock.getZone()).thenReturn(ZoneId.systemDefault());

            Voucher activeVoucher = new Voucher("PROMO10", VoucherType.FLAT, new BigDecimal("10.00"),
                    LocalDate.of(2026, 12, 31));
            when(mockVoucherRepo.findByCode("PROMO10")).thenReturn(Optional.of(activeVoucher));

            BigDecimal price = mockedService.calculatePrice(SubscriptionTier.BASIC, 0, "PROMO10");

            assertThat(price).isEqualByComparingTo("40.00");
            verify(mockVoucherRepo).findByCode("PROMO10");
        }

        @Test
        @DisplayName("Service throws InvalidVoucherException when repository returns expired voucher")
        void serviceThrowsExceptionWhenVoucherExpiredViaClock() {
            SubscriptionPricingService mockedService = new SubscriptionPricingService(mockVoucherRepo, mockClock);

            LocalDate today = LocalDate.of(2026, 6, 1);
            Instant todayInstant = today.atStartOfDay(ZoneId.systemDefault()).toInstant();

            when(mockClock.instant()).thenReturn(todayInstant);
            when(mockClock.getZone()).thenReturn(ZoneId.systemDefault());

            // Voucher expired on May 31, 2026
            Voucher expiredVoucher = new Voucher("SPRING50", VoucherType.PERCENTAGE, new BigDecimal("0.50"),
                    LocalDate.of(2026, 5, 31));
            when(mockVoucherRepo.findByCode("SPRING50")).thenReturn(Optional.of(expiredVoucher));

            assertThatThrownBy(() -> mockedService.calculatePrice(SubscriptionTier.BASIC, 0, "SPRING50"))
                    .isInstanceOf(InvalidVoucherException.class)
                    .hasMessageContaining("Voucher is expired: SPRING50");

            verify(mockVoucherRepo).findByCode("SPRING50");
        }

        @Test
        @DisplayName("Service throws InvalidVoucherException when repository returns empty")
        void serviceThrowsExceptionWhenVoucherNotFound() {
            SubscriptionPricingService mockedService = new SubscriptionPricingService(mockVoucherRepo, mockClock);

            when(mockVoucherRepo.findByCode("NOTFOUND")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mockedService.calculatePrice(SubscriptionTier.BASIC, 0, "NOTFOUND"))
                    .isInstanceOf(InvalidVoucherException.class)
                    .hasMessageContaining("Invalid voucher code: NOTFOUND");

            verify(mockVoucherRepo).findByCode("NOTFOUND");
        }
    }
}

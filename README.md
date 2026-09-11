# Subscription Tier & Discount Processor (`SubscriptionPricingService`)

A production-grade Java and Spring Boot service that processes monthly subscription rates based on subscription tiers, customer longevity discounts, promotional voucher codes, currency rounding, and zero-floor constraints.

Developed in accordance with the **Strict Zero-Base Start Rule** (no starter code, pre-built scaffolding, or templates).

---

## Technology Stack

- **Language:** Java 17 (LTS)
- **Framework:** Spring Boot 3.2.5 (`spring-boot-starter`, `spring-boot-starter-test`)
- **Build Tool:** Apache Maven 3.9+
- **Testing:** JUnit 5 (Jupiter 5.10+)
- **Mocking:** Mockito 5.x (`mockito-core`, `mockito-junit-jupiter`)
- **Assertions:** AssertJ 3.x

---

## Core Requirements & Business Logic

### 1. Tier Base Rates
| Tier | Base Rate / Month |
|---|---|
| `BASIC` | **$50.00** |
| `PRO` | **$150.00** |
| `ENTERPRISE` | **$500.00** |

### 2. Longevity Discounts
Discounts applied to the base rate based on account tenure:
- **$\le$ 12 months:** 0% discount (standard rate)
- **> 12 months (13 to 36 months):** 10% discount
- **> 36 months (37+ months):** 25% discount

### 3. Promotional Voucher Codes
Applied after longevity discounts:
- **`SAVE20`:** Deducts an additional **$20.00** flat fee after percentage discounts.
- **`HALFPRICE`:** Reduces calculated rate by **50%** (applied after longevity discounts).
- **Voucher Validation:** Case-insensitive and trimmed. Null or empty voucher codes apply no voucher discount.
- **Error Handling:** Expired or unrecognized vouchers throw custom `InvalidVoucherException`.

### 4. Rounding & Floor Rule
- **Zero-Floor Rule:** The final monthly total cannot drop below **$0.00**.
- **Currency Math:** All calculations use `BigDecimal` with half-up rounding (`RoundingMode.HALF_UP`) rounded accurately to two decimal places (`.setScale(2, RoundingMode.HALF_UP)`).

---

## Project Structure

```
Subscription-Pricing/
├── pom.xml
├── .gitignore
├── README.md
└── src/
    ├── main/
    │   ├── java/com/subscription/pricing/
    │   │   ├── SubscriptionPricingApplication.java
    │   │   ├── config/
    │   │   │   └── SubscriptionPricingConfig.java
    │   │   ├── exception/
    │   │   │   └── InvalidVoucherException.java
    │   │   ├── model/
    │   │   │   ├── SubscriptionTier.java
    │   │   │   ├── Voucher.java
    │   │   │   ├── VoucherType.java
    │   │   │   └── PricingResult.java
    │   │   ├── repository/
    │   │   │   ├── VoucherRepository.java
    │   │   │   └── InMemoryVoucherRepository.java
    │   │   └── service/
    │   │       └── SubscriptionPricingService.java
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/subscription/pricing/
            ├── SubscriptionPricingApplicationTests.java
            ├── repository/
            │   └── InMemoryVoucherRepositoryTest.java
            └── service/
                └── SubscriptionPricingServiceTest.java
```

---

## API Summary

### `SubscriptionPricingService`

```java
// Standard calculation
BigDecimal calculatePrice(SubscriptionTier tier, int activeMonths);
BigDecimal calculatePrice(SubscriptionTier tier, int activeMonths, String voucherCode);

// Aliases
BigDecimal calculateMonthlyPrice(SubscriptionTier tier, int activeMonths);
BigDecimal calculateMonthlyPrice(SubscriptionTier tier, int activeMonths, String voucherCode);

// Itemized breakdown
PricingResult calculateDetailedPrice(SubscriptionTier tier, int activeMonths);
PricingResult calculateDetailedPrice(SubscriptionTier tier, int activeMonths, String voucherCode);
```

---

## Running Automated Tests

Execute all unit and integration tests from the terminal:

```bash
mvn clean test
```

### Test Coverage Highlights (87 tests across all suites)
1. **Tier Base Rates:** Verifies base rates for `BASIC`, `PRO`, and `ENTERPRISE`.
2. **Longevity Discounts:** Parameterized boundary tests (`0`, `1`, `11`, `12`, `13`, `24`, `36`, `37`, `48`, `100` months).
3. **Promotional Vouchers:** Flat fee `SAVE20`, percentage discount `HALFPRICE`, case-insensitivity, whitespace trimming, null/empty bypass.
4. **Invalid & Expired Vouchers:** Throws `InvalidVoucherException` for unknown vouchers or vouchers past their expiration date.
5. **Rounding & Floor Rule:** Floored to `$0.00` when discounts exceed price; half-up rounding to two decimal places.
6. **Input Validation:** Throws `IllegalArgumentException` on null tier or negative active months.
7. **Mockito 5.x Tests:** Mockito `@Mock` of `VoucherRepository` and `Clock` verifying isolation and clock-based expiration.
8. **Spring Boot Integration:** `@SpringBootTest` verifying context startup and autowiring.

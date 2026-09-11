# AI Prompt Logs — Test-Driven Development with AI

## Task 1 — Write Failing Tests First (RED Phase)

### 1. 5-Part Contextual Prompt Submitted to AI

```text
[ROLE]
You are a Senior QA Automation Engineer and Test Architect specializing in Test-Driven Development (TDD), Java 17, JUnit 5 Jupiter, Mockito 5, and AssertJ in Spring Boot microservices.

[TARGET CLASS]
`com.subscription.pricing.service.SubscriptionPricingService`

[BUSINESS RULES]
1. Tier Base Rates:
   - BASIC: $50.00 / month
   - PRO: $150.00 / month
   - ENTERPRISE: $500.00 / month
2. Longevity Discounts (applied directly to the tier base rate):
   - Active duration <= 12 months: 0% discount.
   - Active duration > 12 months and <= 36 months (13 to 36 months): 10% discount on base rate.
   - Active duration > 36 months (37+ months): 25% discount on base rate.
3. Promotional Voucher Codes (applied AFTER longevity discount):
   - "SAVE20": Deducts a flat $20.00 fee after percentage discounts.
   - "HALFPRICE": Reduces the calculated rate by 50% after longevity discounts.
   - Voucher code validation: Must be case-insensitive and trimmed. Null or empty voucher string applies no promotional discount.
   - Expired or invalid vouchers: Must throw custom `InvalidVoucherException`.
4. Rounding & Floor Rules:
   - Zero-Floor Rule: The final monthly rate cannot drop below $0.00 under any circumstances.
   - Currency Math: All calculations must use `BigDecimal` with half-up rounding (`RoundingMode.HALF_UP`) rounded to exactly 2 decimal places (`setScale(2, RoundingMode.HALF_UP)`).

[BOUNDARY CONDITIONS & EDGE CASES]
- Exact longevity month boundaries: 0, 11, 12 months (0% tier), 13 months (10% tier), 36 months (10% tier), 37 months (25% tier), and high longevity (e.g., 100 months).
- Negative tenure: Negative active months (e.g., -1) must throw `IllegalArgumentException`.
- Null tier: Null `SubscriptionTier` must throw `IllegalArgumentException`.
- Voucher format tolerance: "save20", "SAVE20", "  SAVE20  ", "halfprice", "HALFPRICE".
- Voucher failure modes: Expired voucher date, non-existent voucher code string, voucher discount driving price below $0.00 (verifying $0.00 floor).
- Half-up rounding checks on fractional cents.

[OUTPUT CONSTRAINTS]
- Generate a comprehensive unit test suite in Java 17 named `SubscriptionPricingServiceTest.java` in package `com.subscription.pricing.service`.
- Use JUnit 5 Jupiter annotations (`@Test`, `@ParameterizedTest`, `@CsvSource`, `@ValueSource`, `@Nested`, `@DisplayName`).
- Use AssertJ fluent assertions (`assertThat`, `isEqualByComparingTo`, `isEqualTo`, `assertThatThrownBy`).
- Include Mockito unit tests with `@ExtendWith(MockitoExtension.class)`, `@Mock VoucherRepository`, and `@InjectMocks SubscriptionPricingService`.
- Strictly write ONLY the unit test code. Do NOT create the `SubscriptionPricingService` production implementation class.
```

### 2. Failing Test Runner Verification (RED State)
- Command executed: `mvn clean test`
- Result: Compilation Failure (`cannot find symbol: class SubscriptionPricingService`)
- Status: **RED state confirmed.**

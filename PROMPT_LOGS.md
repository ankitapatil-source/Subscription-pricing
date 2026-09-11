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

---

## Task 2 — Critique & Audit AI-Generated Tests

### 1. AI Test Audit Matrix & Identified Flaws

| # | Flaw Category | Location | Identified Anti-Pattern / Issue | Corrective Fix Applied |
|---|---|---|---|---|
| **1** | **Weak / Ambiguous Assertion** | `SubscriptionPricingServiceTest.java:187-194` (`voucherCodesAreCaseInsensitiveAndTrimmed`) | Used `assertThat(price).isIn("30.00", "25.00")`. If `save20` returned 25.00 or `halfprice` returned 30.00, test would still pass falsely. | Converted `@ValueSource` to `@CsvSource` with explicit 1:1 input-to-expected mapping, asserting exact price and scale of 2. |
| **2** | **Incomplete Assertion (Scale Only)** | `SubscriptionPricingServiceTest.java:290-300` (`halfUpRoundingWithOddCentFraction`) | Tested odd fractional cents but only checked `assertThat(price.scale()).isEqualTo(2)`. A stub returning `0.00` would pass. | Added explicit currency amount assertion: `assertThat(price).isEqualByComparingTo("33.33")` alongside scale check. |
| **3** | **Weak Assertion (`assertNotNull`)** | `SubscriptionPricingApplicationTests.java:21-26` (`contextLoads`) | Used basic `assertThat(pricingService).isNotNull()`, which only tests DI instantiation without validating runtime behavior. | Strengthened to assert operational contract execution: calling `calculatePrice(BASIC, 0)` and checking exact rate `$50.00`. |
| **4** | **Missing Interaction Boundaries** | `SubscriptionPricingServiceTest.java:406-465` (`MockitoUnitTests`) | Mock tests verified expected calls but omitted `verifyNoMoreInteractions()`, allowing unintended repo calls. | Added `verifyNoMoreInteractions(mockVoucherRepo)` across all mock test methods to enforce strict interaction contracts. |

### 2. Failing Test Runner Verification After Audit
- Command executed: `mvn clean test`
- Result: Compilation Failure (`cannot find symbol: class SubscriptionPricingService`)
- Status: Confirmed tests continue failing cleanly for missing SUT implementation, not test syntax or improper assertions.

---

## Task 3 — Implement Against Tests (GREEN Phase)

### 1. AI Implementation Prompt Submitted

```text
[ROLE]
You are a Senior Backend Software Engineer specializing in Java 17, Spring Boot 3 microservices, and Clean Architecture.

[TASK]
Implement the minimal production code for `SubscriptionPricingService` in package `com.subscription.pricing.service` to make 100% of the audited failing tests pass cleanly. Do not alter any existing unit tests.

[SPECIFICATION DERIVED FROM AUDITED TESTS]
1. Target Class:
   - Name: `SubscriptionPricingService`
   - Package: `com.subscription.pricing.service`
   - Spring Annotation: `@Service`
2. Constructors:
   - Default no-args constructor initializing `InMemoryVoucherRepository` and `Clock.systemDefaultZone()`.
   - Single-arg constructor taking `VoucherRepository`.
   - `@Autowired` constructor taking `(VoucherRepository voucherRepository, Clock clock)`.
3. Methods & Overloads:
   - `BigDecimal calculatePrice(SubscriptionTier tier, int activeMonths)`
   - `BigDecimal calculatePrice(SubscriptionTier tier, int activeMonths, String voucherCode)`
   - `BigDecimal calculateMonthlyPrice(SubscriptionTier tier, int activeMonths)` (alias)
   - `BigDecimal calculateMonthlyPrice(SubscriptionTier tier, int activeMonths, String voucherCode)` (alias)
   - `PricingResult calculateDetailedPrice(SubscriptionTier tier, int activeMonths)`
   - `PricingResult calculateDetailedPrice(SubscriptionTier tier, int activeMonths, String voucherCode)`
4. Business Rules & Logic:
   - Tier base rates: BASIC ($50.00), PRO ($150.00), ENTERPRISE ($500.00).
   - Longevity discount: <= 12 months (0%), 13-36 months (10%), > 36 months (25%).
   - Longevity discount amount = baseRate * discountPercentage (HALF_UP, 2 decimals).
   - Rate after longevity = baseRate - longevityDiscountAmount.
   - Vouchers (case-insensitive, trimmed):
     * SAVE20: Flat $20.00 reduction.
     * HALFPRICE: 50% discount on rate after longevity.
     * Expired or missing: throw `InvalidVoucherException`.
     * Null or blank: apply $0.00 voucher discount.
   - Floor & Rounding: Final price cannot drop below $0.00, formatted with scale 2 and `RoundingMode.HALF_UP`.
   - Validations: Null tier throws `IllegalArgumentException`, negative activeMonths throws `IllegalArgumentException`.
```

### 2. Passing Test Runner Verification (GREEN State)
- Command executed: `mvn clean test`
- Results:
  ```
  [INFO] Results:
  [INFO] 
  [INFO] Tests run: 87, Failures: 0, Errors: 0, Skipped: 0
  [INFO] 
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] ------------------------------------------------------------------------
  ```
- Status: **GREEN state confirmed. 100% test pass rate achieved with zero test alterations.**

---

## Task 4 — Refactor Safely Under Test Shield (REFACTOR Phase)

### 1. AI Refactoring Prompt Submitted

```text
[ROLE]
You are a Principal Software Architect specializing in Modern Java (Java 17 LTS), Clean Code, and Software Design Patterns.

[TASK]
Refactor the procedural logic in `SubscriptionPricingService.java` and `PricingResult.java` using modern Java 17 language features while operating under the safety net of our existing 87 unit tests. Ensure zero behavioral regressions.

[REFACTORING GOALS]
1. Replace procedural if-else voucher branching with modern Java 17 enhanced switch expressions (`switch (voucher.getType())`).
2. Convert the boilerplate `PricingResult` class into an immutable Java 17 `record`, maintaining backward-compatible JavaBean getter aliases for framework interoperability.
3. Extract longevity discount percentage resolution into a dedicated, clean helper method (`resolveLongevityDiscountPercentage`).
4. Modernize string validation using `String.isBlank()` and `String.strip()`.
5. Enforce zero-floor logic idiomatic to `BigDecimal` using `.max(ZERO_DOLLARS)`.
```

### 2. Multi-File Refactoring Summary
- **`PricingResult.java`**: Converted to a Java 17 `record`, eliminating 80+ lines of boilerplate constructor, `equals`, `hashCode`, and `toString` while preserving full JavaBean compatibility.
- **`SubscriptionPricingService.java`**: Replaced procedural conditionals with Java 17 switch expression, extracted longevity resolution, and utilized `BigDecimal.max(ZERO_DOLLARS)`.

### 3. Verification: Passing Test Runner Output (Zero Regressions)
- Command executed: `mvn clean test`
- Results:
  ```
  [INFO] Results:
  [INFO] 
  [INFO] Tests run: 87, Failures: 0, Errors: 0, Skipped: 0
  [INFO] 
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] ------------------------------------------------------------------------
  ```
- Status: **Zero test regressions. All 87 tests passed cleanly under the refactored implementation.**

---

## Task 5 — Adversarial Edge Case Expansion

### 1. Adversarial QA Prompt Submitted to AI

```text
[ROLE]
You are a Principal Adversarial QA Engineer specializing in financial billing engines, edge-case vulnerability testing, boundary analysis, and mission-critical payment services.

[TASK]
Brainstorm a comprehensive list of potential boundary flaws, race conditions, edge cases, and arithmetic vulnerabilities for `SubscriptionPricingService`. Select at least 4 non-trivial edge cases, implement them as parameterized JUnit 5 tests, and provide thorough engineering justifications for each.

[BRAINSTORM TARGETS]
- Zero-floor guarantees and extreme over-discount thresholds ($0.00 floor constraints).
- Exact expiration date boundary semantics (same-day validity vs expired).
- Numerical extremes, integer overflow, and extreme longevity values (e.g., Integer.MAX_VALUE).
- Percentage discount edge cases (0%, 100%, >100% free passes).
- Fractional-cent arithmetic rounding errors and half-up rounding guarantees.
```

### 2. AI Brainstorm List of Edge Cases
1. **Inclusive Expiration Date Boundary**: Testing a voucher where `expiryDate == today` to guarantee coupons remain redeemable until the end of their expiration day.
2. **Extreme Multi-Tier Over-Discounts**: Stacking massive flat vouchers ($100, $200, $1,000, $10,000) against low base rates (BASIC $50) with longevity discounts to verify strict $0.00 floor enforcement without negative-zero (`-0.00`) or sign errors.
3. **Integer Overflow on Extreme Longevity**: Supplying `Integer.MAX_VALUE` (2,147,483,647 months) and multi-century active durations to prove calculations never overflow 32-bit integers or regress into incorrect tiers.
4. **Percentage Voucher Boundaries (0%, 100%, >100%)**: Testing full free waivers (100% discount) and over-percentage promos (150%) to guarantee exact `$0.00` calculation without floating point drift or negative values.
5. **Micro-Cent Half-Up Precision**: Validating odd fractional cents (e.g. 33.335%) across uneven discount combinations.
6. **Whitespace & Control Character Tolerances**: Testing tab `\t` and newline `\n` padded voucher strings.
7. **Cross-Tier Longevity Thresholds**: Testing exact boundary transitions (12 -> 13 months, 36 -> 37 months) across all three subscription tiers.

### 3. Selected 4 Non-Trivial Edge Cases & Engineering Justifications

1. **Edge Case 1: Same-Day Voucher Expiration Boundary (Inclusive Expiration Contract)**
   - *Implementation:* `voucherExpiringTodayIsStillValid`
   - *Engineering Justification:* In customer billing systems, voucher expiration dates represent inclusive validity ("valid through 23:59:59 of that date"). Using strict `isAfter()` prevents premature coupon revocation on the expiration date itself, preventing false rejections and checkout drop-offs.
2. **Edge Case 2: Extreme Over-Discounts Flooring to Exactly $0.00 Across All Tiers**
   - *Implementation:* `extremeOverDiscountsStrictlyFloorToZero`
   - *Engineering Justification:* Negative totals in payment gateways trigger API rejection or accidental card refunds/credits. This test guarantees that no combination of base rates, longevity reductions, and outsized vouchers ($10,000) can ever produce an invoice total below `$0.00`.
3. **Edge Case 3: Extreme Longevity Duration and Integer.MAX_VALUE Resilience**
   - *Implementation:* `extremeLongevityDoesNotOverflow`
   - *Engineering Justification:* Legacy account migrations or erroneous database values can pass astronomical active month counts. This test ensures the pricing logic handles maximum integer values (`2,147,483,647`) without signed arithmetic overflow or incorrect fallback to 0% discount.
4. **Edge Case 4: Zero, 100%, and Over-100% Percentage Voucher Boundaries**
   - *Implementation:* `percentageVoucherBoundaries`
   - *Engineering Justification:* Promotional marketing campaigns frequently issue 100% trial passes ("FREEPASS") or stacked percentages. This test validates that 100% discount evaluates to exactly `$0.00` and >100% discount cleanly hits the `$0.00` floor without arithmetic aberrations.

### 4. Verification: Passing Test Runner Output (Expanded Test Suite)
- Command executed: `mvn clean test`
- Results:
  ```
  [INFO] Results:
  [INFO] 
  [INFO] Tests run: 103, Failures: 0, Errors: 0, Skipped: 0
  [INFO] 
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] ------------------------------------------------------------------------
  ```
- Status: **All 103 tests (87 base + 16 adversarial edge-case tests) passed cleanly.**

---

## Task 6 — Written Reflection

### 1. Written Reflection Artifact
- **File**: `REFLECTION.md`
- **Word Count**: 409 words (verified within required 300–500 word limit).
- **Core Topics Addressed**:
  1. *AI Productivity & Acceleration*: Rapid generation of parameterized tests, Mockito infrastructure, and modern Java 17 refactoring.
  2. *AI Weak Logic & Anti-Patterns*: Ambiguous assertions (`isIn`), scale-only checks, weak `assertNotNull` validations, and missing mock boundaries.
  3. *TDD Guardrails Against Technical Debt*: Executable machine specifications, minimal scope adherence under GREEN phase, and regression-free Java 17 modernization under the test shield.

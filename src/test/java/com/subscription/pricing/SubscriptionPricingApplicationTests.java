package com.subscription.pricing;

import com.subscription.pricing.model.SubscriptionTier;
import com.subscription.pricing.service.SubscriptionPricingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Spring Boot Integration Tests")
class SubscriptionPricingApplicationTests {

    @Autowired
    private SubscriptionPricingService pricingService;

    @Test
    @DisplayName("Context loads and SubscriptionPricingService is autowired successfully")
    void contextLoads() {
        assertThat(pricingService).isNotNull();
    }

    @Test
    @DisplayName("End-to-end pricing flow works within Spring application context")
    void endToEndPricingFlowInContext() {
        // PRO tier ($150.00) active for 24 months (10% discount -> $135.00) with SAVE20 ($20.00 off -> $115.00)
        BigDecimal price = pricingService.calculatePrice(SubscriptionTier.PRO, 24, "SAVE20");
        assertThat(price).isEqualByComparingTo("115.00");
    }
}

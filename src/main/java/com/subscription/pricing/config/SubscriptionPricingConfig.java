package com.subscription.pricing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Spring configuration providing shared infrastructure beans.
 */
@Configuration
public class SubscriptionPricingConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}

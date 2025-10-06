package com.ars.gateway.security.ratelimiter;

import com.ars.gateway.service.RateLimitConfigService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class RateLimiterConfigInitializer implements ApplicationRunner {
    private final RateLimitConfigService rateLimitConfigService;

    public RateLimiterConfigInitializer(RateLimitConfigService rateLimitConfigService) {
        this.rateLimitConfigService = rateLimitConfigService;
    }

    @Override
    public void run(ApplicationArguments args) {
        rateLimitConfigService.updateRateLimitConfig();
    }
}

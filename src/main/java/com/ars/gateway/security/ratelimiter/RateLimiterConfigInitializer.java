package com.ars.gateway.security.ratelimiter;

import com.ars.gateway.dto.RouteConfigDTO;
import com.ars.gateway.service.RateLimitConfigService;
import com.ars.gateway.service.RouteConfigService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RateLimiterConfigInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(RateLimiterConfigInitializer.class);
    private final RateLimitConfigService rateLimitConfigService;
    private final RouteConfigService routeConfigService;

    public RateLimiterConfigInitializer(RateLimitConfigService rateLimitConfigService,
                                        RouteConfigService routeConfigService) {
        this.rateLimitConfigService = rateLimitConfigService;
        this.routeConfigService = routeConfigService;
    }

    @Override
    public void run(ApplicationArguments args) {
        rateLimitConfigService.refreshRateLimitExcludedApis();
        boolean isInitialized = rateLimitConfigService.refreshRateLimitConfig();

        if (!isInitialized) {
            logStartup();
        }
    }

    private void logStartup() {
        List<RouteConfigDTO> routeConfigs = routeConfigService.getRoutesConfig();
        routeConfigs.forEach(routeConfig -> {
            log.info("[DEFAULT_RATE_LIMIT] - routeId: {}, target: {}", routeConfig.getRouteId(), routeConfig.getUri());
            log.info("[DEFAULT_RATE_LIMIT_PREDICATES] - predicates: {}", routeConfig.getPredicates());
            log.info("[DEFAULT_RATE_LIMIT_CONFIG] - rate limit config: {}", routeConfig.getRate());
        });
    }
}

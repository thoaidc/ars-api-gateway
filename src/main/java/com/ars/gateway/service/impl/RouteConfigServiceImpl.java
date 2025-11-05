package com.ars.gateway.service.impl;

import com.ars.gateway.constants.RateLimitConstants;
import com.ars.gateway.dto.RouteConfigDTO;
import com.ars.gateway.security.ratelimiter.CustomRateLimiter;
import com.ars.gateway.security.ratelimiter.RateLimiterConfig;
import com.ars.gateway.service.RouteConfigService;

import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteConfigServiceImpl implements RouteConfigService {
    private final GatewayProperties gatewayProperties;

    public RouteConfigServiceImpl(GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
    }

    public List<RouteConfigDTO> getRoutesConfig() {
        return gatewayProperties.getRoutes().stream().map(this::convertRouteConfig).toList();
    }

    private RouteConfigDTO convertRouteConfig(RouteDefinition routeDefinition) {
        RouteConfigDTO config = new RouteConfigDTO();
        config.setRouteId(routeDefinition.getId());
        config.setUri(routeDefinition.getUri().toString());
        config.setPredicates(routeDefinition.getPredicates().stream().map(this::convertPredicateValue).toList());
        // Get the CustomRateLimiter filter if available
        config.setRate(convertRateLimiterConfig(routeDefinition));
        return config;
    }

    private String convertPredicateValue(PredicateDefinition predicateDefinition) {
        String predicateName = predicateDefinition.getName();
        String predicateValue = String.join(",", predicateDefinition.getArgs().values());
        return predicateName + "=" + predicateValue;
    }

    private RateLimiterConfig convertRateLimiterConfig(RouteDefinition routeDefinition) {
        return routeDefinition.getFilters()
            .stream()
            .filter(filter -> CustomRateLimiter.class.getSimpleName().equalsIgnoreCase(filter.getName()))
            .findFirst()
            .map(this::convertRateLimiterConfig)
            .orElseGet(RateLimiterConfig::new);
    }

    private RateLimiterConfig convertRateLimiterConfig(FilterDefinition filterDefinition) {
        try {
            RateLimiterConfig rateLimiterConfig = new RateLimiterConfig();
            String banThreshold = filterDefinition.getArgs().get(RateLimitConstants.BAN_THRESHOLD_PROPERTIES);
            String windowSeconds = filterDefinition.getArgs().get(RateLimitConstants.WINDOW_SECONDS_PROPERTIES);
            String banDurationMinutes = filterDefinition.getArgs().get(RateLimitConstants.BAN_DURATION_MINUTES_PROPERTIES);
            rateLimiterConfig.setBanThreshold(Integer.parseInt(banThreshold));
            rateLimiterConfig.setWindowSeconds(Integer.parseInt(windowSeconds));
            rateLimiterConfig.setBanDurationMinutes(Integer.parseInt(banDurationMinutes));
            return rateLimiterConfig;
        } catch (Exception ignored) {
            return null;
        }
    }
}

package com.ars.gateway.constants;

import org.springframework.security.config.web.server.SecurityWebFiltersOrder;

public interface FilterChainConstants {
    interface Order {
        int BEFORE_SPRING_DEFAULT_GLOBAL_ERROR_HANDLER = -2;
        int AFTER_SPRING_DEFAULT_AUTHORIZATION_FILTER = SecurityWebFiltersOrder.AUTHORIZATION.getOrder() + 1;
    }
}

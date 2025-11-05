package com.ars.gateway.dto;

import com.ars.gateway.security.ratelimiter.RateLimiterConfig;

import java.util.List;

@SuppressWarnings("unused")
public class RouteConfigDTO {
    private String routeId;
    private String uri;
    private List<String> predicates;
    private RateLimiterConfig rate = new RateLimiterConfig();

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public List<String> getPredicates() {
        return predicates;
    }

    public void setPredicates(List<String> predicates) {
        this.predicates = predicates;
    }

    public RateLimiterConfig getRate() {
        return rate;
    }

    public void setRate(RateLimiterConfig rate) {
        this.rate = rate;
    }
}

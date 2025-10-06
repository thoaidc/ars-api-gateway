package com.ars.gateway.dto;

import java.util.List;

@SuppressWarnings("unused")
public class RouteConfigDTO {
    private String routeId;
    private String uri;
    private List<String> predicates;
    private RateLimiter rate = new RateLimiter();

    public static class RateLimiter {
        private Integer banThreshold;
        private Integer windowSeconds;
        private Integer banDurationMinutes;

        public Integer getBanThreshold() {
            return banThreshold;
        }

        public void setBanThreshold(Integer banThreshold) {
            this.banThreshold = banThreshold;
        }

        public Integer getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(Integer windowSeconds) {
            this.windowSeconds = windowSeconds;
        }

        public Integer getBanDurationMinutes() {
            return banDurationMinutes;
        }

        public void setBanDurationMinutes(Integer banDurationMinutes) {
            this.banDurationMinutes = banDurationMinutes;
        }
    }

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

    public RateLimiter getRate() {
        return rate;
    }

    public void setRate(RateLimiter rate) {
        this.rate = rate;
    }
}

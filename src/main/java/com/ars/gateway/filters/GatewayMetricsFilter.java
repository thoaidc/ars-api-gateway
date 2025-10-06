package com.ars.gateway.filters;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GatewayMetricsFilter implements GlobalFilter {
    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    private final Map<String, Timer> timerCache = new ConcurrentHashMap<>();

    public GatewayMetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        activeRequests.incrementAndGet();

        long startTime = System.currentTimeMillis();
        long requestSize = exchange.getRequest().getHeaders().getContentLength();

        return chain.filter(exchange)
                .doOnSuccess(aVoid -> recordMetrics(exchange, startTime, requestSize))
                .doOnError(e -> recordMetrics(exchange, startTime, requestSize));
    }

    private void recordMetrics(ServerWebExchange exchange, long startTime, long requestSize) {
        activeRequests.decrementAndGet();
        long durationMs = System.currentTimeMillis() - startTime;
        int status = exchange.getResponse().getStatusCode() != null ? exchange.getResponse().getStatusCode().value() : 0;
        long responseSize = exchange.getResponse().getHeaders().getContentLength();

        Route route = exchange.getAttribute("org.springframework.cloud.gateway.support.ServerWebExchangeUtils.gatewayRoute");
        String routeId = route != null ? route.getId() : null;
        String path = exchange.getRequest().getPath().value();

        recordCounter("gateway_requests_total", "endpoint", path, "status", String.valueOf(status));
        recordTimer("gateway_response_time_seconds", "endpoint", path, "status", String.valueOf(status), durationMs);
        recordSummary("gateway_request_size_bytes", "endpoint", path, requestSize);
        recordSummary("gateway_response_size_bytes", "endpoint", path, responseSize);
        recordCounter("gateway_requests_total", "route", routeId, "status", String.valueOf(status));
        recordTimer("gateway_response_time_seconds", "route", routeId, "status", String.valueOf(status), durationMs);
        recordSummary("gateway_request_size_bytes", "route", routeId, requestSize);
        recordSummary("gateway_response_size_bytes", "route", routeId, responseSize);

        meterRegistry.gauge("gateway_active_requests", activeRequests);
    }

    private void recordCounter(String name, String tagKey1, String tagVal1, String tagKey2, String tagVal2) {
        meterRegistry.counter(name, tagKey1, tagVal1, tagKey2, tagVal2).increment();
    }

    private void recordTimer(String name, String tagKey1, String tagVal1, String tagKey2, String tagVal2, long durationMs) {
        String cacheKey = name + "|" + tagVal1 + "|" + tagVal2;
        Timer timer = timerCache.computeIfAbsent(cacheKey, k ->
                Timer.builder(name)
                        .tags(tagKey1, tagVal1, tagKey2, tagVal2)
                        .publishPercentileHistogram()
                        .register(meterRegistry)
        );
        timer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    private void recordSummary(String name, String tagKey, String tagVal, long value) {
        meterRegistry.summary(name, tagKey, tagVal).record(value);
    }
}

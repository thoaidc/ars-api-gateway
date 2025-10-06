package com.ars.gateway.dto;

import com.ars.gateway.constants.RateLimitConstants;
import java.util.Optional;

/**
 * DTO configures Rate Limit for Spring Cloud Gateway CustomRateLimiter
 * <p>
 * Parameters:
 * <ul>
 * <li>{@code banThreshold}: Threshold of number of requests exceeded in a period of time to ban user/IP</li>
 * <li>{@code windowSeconds}: Time window (in seconds) to count requests compared to banThreshold</li>
 * <li>{@code banDurationMinutes}: Time to ban user/IP (in minutes) when exceeding the threshold</li>
 * </ul>
 * @author thoaidc
 */
@SuppressWarnings("unused")
public class RateLimitConfigDTO {
    private String routeId;
    private int banThreshold = RateLimitConstants.BAN_THRESHOLD;
    private int windowSeconds = RateLimitConstants.WINDOW_SECONDS;
    private int banDurationMinutes = RateLimitConstants.BAN_DURATION_MINUTES;

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public int getBanThreshold() {
        return banThreshold;
    }

    public void setBanThreshold(Integer banThreshold) {
        this.banThreshold = Optional.ofNullable(banThreshold).orElse(RateLimitConstants.BAN_THRESHOLD);
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(Integer windowSeconds) {
        this.windowSeconds = Optional.ofNullable(windowSeconds).orElse(RateLimitConstants.WINDOW_SECONDS);
    }

    public int getBanDurationMinutes() {
        return banDurationMinutes;
    }

    public void setBanDurationMinutes(Integer banDurationMinutes) {
        this.banDurationMinutes = Optional.ofNullable(banDurationMinutes).orElse(RateLimitConstants.BAN_DURATION_MINUTES);
    }
}

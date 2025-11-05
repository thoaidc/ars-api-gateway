package com.ars.gateway.security.ratelimiter;

import com.ars.gateway.constants.RateLimitConstants;

/**
 * Rate Limit for Spring Cloud Gateway CustomRateLimiter
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
public class RateLimiterConfig {
    private int banThreshold = RateLimitConstants.BAN_THRESHOLD;
    private int windowSeconds = RateLimitConstants.WINDOW_SECONDS;
    private int banDurationMinutes = RateLimitConstants.BAN_DURATION_MINUTES;

    public int getBanThreshold() {
        return banThreshold;
    }

    public void setBanThreshold(int banThreshold) {
        this.banThreshold = banThreshold;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public int getBanDurationMinutes() {
        return banDurationMinutes;
    }

    public void setBanDurationMinutes(int banDurationMinutes) {
        this.banDurationMinutes = banDurationMinutes;
    }

    @Override
    public String toString() {
        return "[banThreshold=" + banThreshold + ", windowSeconds=" + windowSeconds + ", banTime=" + banDurationMinutes + "]";
    }
}

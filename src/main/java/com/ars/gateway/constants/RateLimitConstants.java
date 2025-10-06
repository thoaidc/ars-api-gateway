package com.ars.gateway.constants;

/**
 * <ul>
 *  <li>{@link #BAN_THRESHOLD}: Threshold of number of requests exceeded in a period of time to ban user/IP</li>
 *  <li>{@link #WINDOW_SECONDS}: Time window (in seconds) to count requests compared to banThreshold</li>
 *  <li>{@link #BAN_DURATION_MINUTES}: Time to ban user/IP (in minutes) when exceeding the threshold</li>
 * </ul>
 * @author thoaidc
 */
public interface RateLimitConstants {
    int WINDOW_SECONDS = 60; // 1 minute
    int BAN_THRESHOLD = 500;
    int BAN_DURATION_MINUTES = 15; // 15 minutes
    boolean REQUEST_BANNED = false;
    boolean REQUEST_ALLOWED = true;
    String BAN_KEY_PREFIX = "banned:";
    String BANNED_VALUE = "true";
    String RATE_LIMIT_KEY = "rate:";
    String RATE_LIMIT_PROPERTIES_PREFIX = "rate-limiter";
    String CONFIG_RATE_LIMIT_CODE = "rate_limiter_config";
    String WINDOW_SECONDS_PROPERTIES = "rate-limiter.windowSeconds";
    String BAN_THRESHOLD_PROPERTIES = "rate-limiter.banThreshold";
    String BAN_DURATION_MINUTES_PROPERTIES = "rate-limiter.banDurationMinutes";
}

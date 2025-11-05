package com.ars.gateway.service;

import java.util.List;

public interface RateLimitConfigService {
    boolean refreshRateLimitConfig();
    void refreshRateLimitExcludedApis();
    List<String> getDefaultRateExcludedApis();
}

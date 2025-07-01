package com.ars.gateway.config.properties;

import com.ars.gateway.constants.PropertiesConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = PropertiesConstants.AUTHENTICATION_CACHE_CONFIG)
public class AuthenticationCacheProps {

    private boolean enabled;
    private int ttlMinutes = 15;
    private String keyPrefix = "jwt:auth:";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTtlMinutes() {
        return ttlMinutes;
    }

    public void setTtlMinutes(int ttlMinutes) {
        this.ttlMinutes = ttlMinutes;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        if (StringUtils.hasText(keyPrefix)) {
            this.keyPrefix = keyPrefix;
        }
    }
}

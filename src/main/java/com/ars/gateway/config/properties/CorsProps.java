package com.ars.gateway.config.properties;

import com.ars.gateway.constants.PropertiesConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = PropertiesConstants.CORS_CONFIG)
public class CorsProps {

    private Map<String, CorsMapping> corsConfigurations = new HashMap<>();

    public Map<String, CorsMapping> getCorsConfigurations() {
        return corsConfigurations;
    }

    public void setCorsConfigurations(Map<String, CorsMapping> corsConfigurations) {
        this.corsConfigurations = corsConfigurations;
    }

    public static class CorsMapping {
        private List<String> allowedOrigins;
        private List<String> allowedMethods;
        private List<String> allowedHeaders;
        private Boolean allowCredentials;
        private Long maxAge;

        // Getters & Setters
        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public Boolean getAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(Boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public Long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(Long maxAge) {
            this.maxAge = maxAge;
        }
    }
}

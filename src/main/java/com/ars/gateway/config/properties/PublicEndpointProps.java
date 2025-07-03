package com.ars.gateway.config.properties;

import com.ars.gateway.constants.PropertiesConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Optional;

@ConfigurationProperties(prefix = PropertiesConstants.SECURITY_REQUEST_CONFIG)
public class PublicEndpointProps {

    /**
     * Global patterns - apply to entire path (including serviceId) <p>
     * Ex: /auth-service/api/p/login, /user-service/actuator/health
     */
    private String[] publicPatterns;

    public String[] getPublicPatterns() {
        return publicPatterns;
    }

    public void setPublicPatterns(String[] publicPatterns) {
        this.publicPatterns = Optional.ofNullable(publicPatterns).orElse(new String[0]);
    }
}

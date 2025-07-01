package com.ars.gateway.config.properties;

import com.ars.gateway.common.Common;
import com.ars.gateway.constants.PropertiesConstants;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ConfigurationProperties(prefix = PropertiesConstants.SECURITY_REQUEST_CONFIG)
public class PublicEndpointProps {

    /**
     * Global patterns - apply to entire path (including serviceId) <p>
     * Ex: /auth-service/api/p/login, /user-service/actuator/health
     */
    private List<String> publicPatterns = new ArrayList<>();

    public List<String> getPublicPatterns() {
        return publicPatterns;
    }

    public void setPublicPatterns(List<String> publicPatterns) {
        if (Objects.nonNull(publicPatterns) && !publicPatterns.isEmpty()) {
            this.publicPatterns = publicPatterns.stream()
                    .map(Common::normalizePath)
                    .filter(Objects::nonNull)
                    .toList();
        }
    }
}

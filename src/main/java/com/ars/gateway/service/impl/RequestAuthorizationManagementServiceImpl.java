package com.ars.gateway.service.impl;

import com.ars.gateway.security.config.DynamicPublicRequestContext;
import com.ars.gateway.service.RequestAuthorizationManagementService;
import com.dct.model.common.JsonUtils;
import com.dct.model.config.properties.SecurityProps;
import com.dct.model.constants.BaseSecurityConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class RequestAuthorizationManagementServiceImpl implements RequestAuthorizationManagementService {
    private static final Logger log = LoggerFactory.getLogger(RequestAuthorizationManagementServiceImpl.class);
    private final DynamicPublicRequestContext dynamicPublicRequestContext;
    private final List<String> defaultPublicRequestPatterns;
    private final StringRedisTemplate redisTemplate;

    public RequestAuthorizationManagementServiceImpl(DynamicPublicRequestContext dynamicPublicRequestContext,
                                                     SecurityProps securityProps,
                                                     StringRedisTemplate redisTemplate) {
        this.dynamicPublicRequestContext = dynamicPublicRequestContext;
        this.defaultPublicRequestPatterns = Arrays.asList(securityProps.getPublicRequestPatterns());
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void refreshPublicRequestsConfig() {
        try {
            String publicRequestsConfig = redisTemplate.opsForValue().get(BaseSecurityConstants.PUBLIC_REQUEST_CONFIG);
            List<String> requestPatterns = JsonUtils.parseJsonToList(publicRequestsConfig, String.class);

            if (Objects.isNull(requestPatterns) || requestPatterns.isEmpty()) {
                dynamicPublicRequestContext.updatePublicPaths(defaultPublicRequestPatterns);
            } else {
                dynamicPublicRequestContext.updatePublicPaths(requestPatterns);
            }
        } catch (Exception e) {
            log.error("[REFRESH_PUBLIC_REQUESTS_PATTERN_FAILED] - error: ", e);
            log.info("Falling back to default patterns: {}", defaultPublicRequestPatterns);
            dynamicPublicRequestContext.updatePublicPaths(defaultPublicRequestPatterns);
        }
    }

    @Override
    public List<String> getDefaultPublicRequestsConfig() {
        return defaultPublicRequestPatterns;
    }
}

package com.ars.gateway.security.config;

import com.ars.gateway.service.RequestAuthorizationManagementService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SecurityConfigInitializer implements ApplicationRunner {
    private final RequestAuthorizationManagementService requestAuthorizationManagementService;

    public SecurityConfigInitializer(RequestAuthorizationManagementService requestAuthorizationManagementService) {
        this.requestAuthorizationManagementService = requestAuthorizationManagementService;
    }

    @Override
    public void run(ApplicationArguments args) {
        requestAuthorizationManagementService.refreshPublicRequestsConfig();
    }
}

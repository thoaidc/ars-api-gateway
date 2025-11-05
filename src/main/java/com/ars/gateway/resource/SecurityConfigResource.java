package com.ars.gateway.resource;

import com.ars.gateway.service.RequestAuthorizationManagementService;
import com.dct.model.constants.BaseSecurityConstants;
import com.dct.model.dto.response.BaseResponseDTO;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gateway/securities")
public class SecurityConfigResource {
    private final RequestAuthorizationManagementService requestAuthorizationManagementService;

    public SecurityConfigResource(RequestAuthorizationManagementService requestAuthorizationManagementService) {
        this.requestAuthorizationManagementService = requestAuthorizationManagementService;
    }

    @GetMapping("/public-request-patterns")
    @PreAuthorize("hasAuthority('" + BaseSecurityConstants.Role.SUPER_ADMIN + "')")
    public Mono<List<String>> getPublicRequestPatterns() {
        return Mono.just(requestAuthorizationManagementService.getDefaultPublicRequestsConfig());
    }

    @PostMapping("/public-request-patterns")
    @PreAuthorize("hasAuthority('" + BaseSecurityConstants.Role.SUPER_ADMIN + "')")
    public Mono<BaseResponseDTO> refreshPublicRequestPatterns() {
        requestAuthorizationManagementService.refreshPublicRequestsConfig();
        return Mono.just(BaseResponseDTO.builder().ok());
    }
}

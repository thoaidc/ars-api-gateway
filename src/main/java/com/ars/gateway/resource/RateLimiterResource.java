package com.ars.gateway.resource;

import com.ars.gateway.service.RateLimitConfigService;
import com.dct.model.constants.BaseRoleConstants;
import com.dct.model.dto.response.BaseResponseDTO;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gateway/securities/rate-limiter")
public class RateLimiterResource {
    private final RateLimitConfigService rateLimitConfigService;

    public RateLimiterResource(RateLimitConfigService rateLimitConfigService) {
        this.rateLimitConfigService = rateLimitConfigService;
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasAuthority('" + BaseRoleConstants.System.SYSTEM + "')")
    public Mono<BaseResponseDTO> refreshRateLimitConfig() {
        rateLimitConfigService.refreshRateLimitConfig();
        return Mono.just(BaseResponseDTO.builder().ok());
    }

    @GetMapping("/excluded")
    @PreAuthorize("hasAuthority('" + BaseRoleConstants.System.SYSTEM + "')")
    public Mono<List<String>> getDefaultRateExcludedApis() {
        return Mono.just(rateLimitConfigService.getDefaultRateExcludedApis());
    }

    @PostMapping("/excluded")
    @PreAuthorize("hasAuthority('" + BaseRoleConstants.System.SYSTEM + "')")
    public Mono<BaseResponseDTO> updateRateExcludedApis() {
        rateLimitConfigService.refreshRateLimitExcludedApis();
        return Mono.just(BaseResponseDTO.builder().ok());
    }
}

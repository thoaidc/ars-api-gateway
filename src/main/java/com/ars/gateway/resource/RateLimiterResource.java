package com.ars.gateway.resource;

import com.ars.gateway.service.RateLimitConfigService;
import com.dct.model.dto.response.BaseResponseDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/rate-limiter")
public class RateLimiterResource {
    private final RateLimitConfigService rateLimitConfigService;

    public RateLimiterResource(RateLimitConfigService rateLimitConfigService) {
        this.rateLimitConfigService = rateLimitConfigService;
    }

    @PostMapping("/configs/refresh")
    public Mono<BaseResponseDTO> refreshRateLimitConfig() {
        rateLimitConfigService.updateRateLimitConfig();
        return Mono.just(BaseResponseDTO.builder().ok());
    }
}

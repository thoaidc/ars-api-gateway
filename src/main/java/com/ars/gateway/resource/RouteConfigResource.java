package com.ars.gateway.resource;

import com.ars.gateway.dto.RouteConfigDTO;
import com.ars.gateway.service.RouteConfigService;
import com.dct.model.constants.BaseSecurityConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/gateway/securities/routes")
public class RouteConfigResource {
    private final RouteConfigService routeConfigService;

    public RouteConfigResource(RouteConfigService routeConfigService) {
        this.routeConfigService = routeConfigService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + BaseSecurityConstants.Role.SUPER_ADMIN + "')")
    public Flux<RouteConfigDTO> getRoutesConfig() {
        return Flux.fromIterable(routeConfigService.getRoutesConfig());
    }
}

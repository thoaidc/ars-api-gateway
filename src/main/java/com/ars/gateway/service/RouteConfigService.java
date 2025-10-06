package com.ars.gateway.service;

import com.ars.gateway.dto.RouteConfigDTO;
import java.util.List;

public interface RouteConfigService {
    List<RouteConfigDTO> getRoutesConfig();
}

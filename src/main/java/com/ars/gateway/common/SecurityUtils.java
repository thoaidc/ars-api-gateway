package com.ars.gateway.common;

import com.dct.model.constants.BaseSecurityConstants;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;

public class SecurityUtils {

    public static String retrieveTokenFromHeader(ServerHttpRequest request) {
        // Extract from Authorization header
        String authHeader = request.getHeaders().getFirst(BaseSecurityConstants.HEADER.AUTHORIZATION_HEADER);

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BaseSecurityConstants.HEADER.TOKEN_TYPE)) {
            return authHeader.substring(BaseSecurityConstants.HEADER.TOKEN_TYPE.length());
        }

        // Extract from query parameter (for WebSocket)
        return request.getQueryParams().getFirst("token");
    }
}

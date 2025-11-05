package com.ars.gateway.common;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

public class GatewaySecurityUtils {
    /**
     * Builds an {@link AuthorizationDecision} based on whether the current request path is public or requires authentication
     * <p>
     *   <ul>
     *     <li>If {@code isPublic} is {@code true}, the request is always allowed (returns {@code AuthorizationDecision(true)})</li>
     *     <li>
     *         Otherwise, the decision depends on the provided {@link Authentication}:
     *         it grants access if the user is authenticated, or denies if not
     *     </li>
     *   </ul>
     * </p>
     *
     * @param auth a {@link Mono} emitting the current {@link Authentication} object, or empty if no authentication is present
     * @param isPublic  flag indicating whether the current request path is public
     * @return a {@link Mono} emitting the resulting {@link AuthorizationDecision}
     *         — granted if public or authenticated, denied otherwise
     * @author thoaidc
     */
    public static Mono<AuthorizationDecision> buildAuthorizationDecision(Mono<Authentication> auth, boolean isPublic) {
        if (isPublic) {
            return Mono.just(new AuthorizationDecision(true));
        }

        return auth.map(Authentication::isAuthenticated)
                .map(AuthorizationDecision::new)
                .defaultIfEmpty(new AuthorizationDecision(false));
    }
}

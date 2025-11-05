package com.ars.gateway.security.config;

import com.ars.gateway.common.GatewaySecurityUtils;
import com.dct.model.common.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages a dynamic set of "public" request paths that do not require authentication
 *
 * <ul>
 *     <li>
 *         This class allows you to maintain and update a list of public API endpoints at runtime
 *         (for example from a database, Redis, or admin API) without restarting the application
 *     </li>
 *     <li>
 *         It provides methods to check if a request path should bypass authentication and
 *         build an {@link AuthorizationDecision} accordingly
 *     </li>
 * </ul>
 *
 * @author thoaidc
 */
@Component
public class DynamicPublicRequestContext {
    /**
     * A thread-safe set containing the public request paths.
     * Paths stored here are used to determine if a request should be allowed
     * without authentication.
     */
    private final Set<String> publicPaths = ConcurrentHashMap.newKeySet();
    private static final Logger log = LoggerFactory.getLogger(DynamicPublicRequestContext.class);

    /**
     * Checks if the request represented by the given {@link AuthorizationContext} matches any path in the dynamic public paths set
     * <p>
     *    If the path is public, an {@link AuthorizationDecision} granting access is returned.
     *    Otherwise, the decision depends on the provided {@link Authentication}:
     *    it grants access if the user is authenticated, or denies access if not.
     * </p>
     *
     * @param authentication a {@link Mono} emitting the current {@link Authentication} object,
     *                       or empty if no authentication is present
     * @param context        the {@link AuthorizationContext} containing the current request information
     * @return a {@link Mono} emitting the resulting {@link AuthorizationDecision}
     */
    public Mono<AuthorizationDecision> isPublicPath(Mono<Authentication> authentication, AuthorizationContext context) {
        String path = context.getExchange().getRequest().getPath().value();
        boolean isPublicPath = SecurityUtils.checkIfAuthenticationNotRequired(path, publicPaths);
        return GatewaySecurityUtils.buildAuthorizationDecision(authentication, isPublicPath);
    }

    /**
     * Updates the set of dynamic public paths at runtime
     * @param newPaths a collection of new public request paths
     */
    public void updatePublicPaths(Collection<String> newPaths) {
        Set<String> oldPaths = Set.copyOf(publicPaths);
        publicPaths.clear();
        publicPaths.addAll(newPaths);
        Set<String> removed = new HashSet<>(oldPaths);
        removed.removeAll(newPaths);
        Set<String> added = new HashSet<>(newPaths);
        added.removeAll(oldPaths);
        log.info("[UPDATE_PUBLIC_REQUEST_PATTERNS]. Added: {}, Removed: {}, Current: {}", added, removed, publicPaths);
    }
}

package org.briarheart.orchestra.security.web.server.authentication.accesstoken;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Abstraction for saving instances of {@link AccessToken} in {@link ServerWebExchange}.
 *
 * @author Roman Chigvintsev
 * @see AccessToken
 */
public interface ServerAccessTokenRepository {
    /**
     * Loads an access token from the given instance of {@link ServerWebExchange}.
     *
     * @param exchange exchange from which access token should be loaded (must not be {@code null})
     * @return loaded access token or empty {@link Mono} if no access token was associated with the given instance of
     * {@link ServerWebExchange}.
     */
    Mono<? extends AccessToken> loadAccessToken(ServerWebExchange exchange);

    /**
     * Saves an access token in the given instance of {@link ServerWebExchange}.
     *
     * @param accessToken access token to be saved (must not be {@code null})
     * @param exchange    exchange in which the given access token should be saved (must not be {@code null})
     * @return saved access token
     */
    Mono<? extends AccessToken> saveAccessToken(AccessToken accessToken, ServerWebExchange exchange);

    /**
     * Removes an access token from the given instance of {@link ServerWebExchange}.
     *
     * @param exchange exchange from which access token should be removed (must not be {@code null})
     * @return removed access token or empty {@link Mono} if no access token was associated with the given instance of
     * {@link ServerWebExchange}.
     */
    Mono<? extends AccessToken> removeAccessToken(ServerWebExchange exchange);
}

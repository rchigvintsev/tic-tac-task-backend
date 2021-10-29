package org.briarheart.tictactask.security.web.server.authentication.accesstoken;

import org.briarheart.tictactask.user.User;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Service for creating of access tokens.
 *
 * @author Roman Chigvintsev
 * @see AccessToken
 */
public interface AccessTokenService {
    /**
     * Creates new access token based on the given user entity.
     *
     * @param user     user for whom an access token should be created (must not be {@code null})
     * @param exchange web exchange (must not be {@code null})
     * @return new access token
     */
    Mono<? extends AccessToken> createAccessToken(User user, ServerWebExchange exchange);

    /**
     * Creates access token from the given token value.
     *
     * @param tokenValue access token value (must not be {@code null} or empty)
     * @return parsed access token
     * @throws InvalidAccessTokenException if error occurred while parsing access token value
     */
    Mono<? extends AccessToken> parseAccessToken(String tokenValue);
}

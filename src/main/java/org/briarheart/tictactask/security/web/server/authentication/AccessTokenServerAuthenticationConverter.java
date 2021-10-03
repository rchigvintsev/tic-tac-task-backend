package org.briarheart.tictactask.security.web.server.authentication;

import lombok.RequiredArgsConstructor;
import org.briarheart.tictactask.security.web.server.authentication.accesstoken.ServerAccessTokenRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Converts {@link ServerWebExchange} to instance of {@link AccessTokenAuthentication}. This converter either expects
 * to find access token value in authorization header or delegates loading of access token to
 * {@link ServerAccessTokenRepository}.
 * <p>
 * In case of authorization header its value should start with "Bearer " followed by access token value.
 *
 * @author Roman Chigvintsev
 *
 * @see AccessTokenAuthentication
 * @see ServerAccessTokenRepository
 */
@RequiredArgsConstructor
public class AccessTokenServerAuthenticationConverter implements ServerAuthenticationConverter {
    private final ServerAccessTokenRepository accessTokenRepository;

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        Authentication authentication = getAuthenticationFromHeaders(request);
        if (authentication != null) {
            return Mono.just(authentication);
        }
        return accessTokenRepository.loadAccessToken(exchange)
                .map(accessToken -> new AccessTokenAuthentication(accessToken.getTokenValue()));
    }

    private Authentication getAuthenticationFromHeaders(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String tokenValue = authorization.substring("Bearer ".length());
            return tokenValue.isEmpty() ? null : new AccessTokenAuthentication(tokenValue);
        }
        return null;
    }
}

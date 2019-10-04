package org.briarheart.orchestra.security.web.server.authentication;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Converts authorization header to instance of {@link AccessTokenAuthentication}. This converter expects that
 * authorization header value will start with "Bearer " followed by access token value.
 *
 * @author Roman Chigvintsev
 *
 * @see AccessTokenAuthentication
 */
public class AccessTokenServerAuthenticationConverter implements ServerAuthenticationConverter {
    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Mono.empty();
        }

        String accessTokenValue = authorization.substring("Bearer ".length());
        return Mono.justOrEmpty(accessTokenValue.isEmpty() ? null : new AccessTokenAuthentication(accessTokenValue));
    }
}

package org.briarheart.orchestra.security.web.server.authentication;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * This handler issues an access token and then sends HTTP status code "200 OK" as a response.
 *
 * @author Roman Chigvintsev
 */
@RequiredArgsConstructor
public class HttpBasicServerAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {
    private final AccessTokenService accessTokenService;

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ServerWebExchange exchange = webFilterExchange.getExchange();
        return accessTokenService.createAccessToken(user, exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.OK);
        }));
    }
}

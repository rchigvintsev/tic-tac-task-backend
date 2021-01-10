package org.briarheart.orchestra.security.web.server.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * This handler issues an access token and then writes access token claims to HTTP response body along with setting
 * HTTP status code to "200 OK".
 *
 * @author Roman Chigvintsev
 */
@RequiredArgsConstructor
public class HttpStatusFormLoginServerAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {
    private final AccessTokenService accessTokenService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ServerWebExchange exchange = webFilterExchange.getExchange();
        return accessTokenService.createAccessToken(user, exchange)
                .flatMap(accessToken
                        -> Mono.fromCallable(() -> objectMapper.writeValueAsBytes(accessToken.getClaims())))
                .flatMap(encodedClaims -> {
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.OK);
                    HttpHeaders responseHeaders = response.getHeaders();
                    responseHeaders.setContentType(MediaType.APPLICATION_JSON);
                    responseHeaders.setContentLength(encodedClaims.length);
                    return response.writeWith(Mono.just(response.bufferFactory().wrap(encodedClaims)));
                });
    }
}

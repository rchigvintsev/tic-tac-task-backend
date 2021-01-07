package org.briarheart.orchestra.security.web.server.authentication;

import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessToken;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class HttpBasicServerAuthenticationSuccessHandlerTest {
    private HttpBasicServerAuthenticationSuccessHandler handler;
    private AccessTokenService accessTokenService;

    @BeforeEach
    void setUp() {
        AccessToken accessToken = mock(AccessToken.class);

        accessTokenService = mock(AccessTokenService.class);
        doReturn(Mono.just(accessToken)).when(accessTokenService).createAccessToken(any(), any());

        handler = new HttpBasicServerAuthenticationSuccessHandler(accessTokenService);
    }

    @Test
    void shouldCreateAccessTokenOnSuccessfulAuthentication() {
        User user = new User();

        ServerHttpResponse response = mock(ServerHttpResponse.class);

        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getResponse()).thenReturn(response);

        WebFilterExchange filterExchange = mock(WebFilterExchange.class);
        when(filterExchange.getExchange()).thenReturn(exchange);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        handler.onAuthenticationSuccess(filterExchange, authentication).block();
        verify(accessTokenService, times(1)).createAccessToken(user, exchange);
    }

    @Test
    void shouldSetHttpStatusCodeToOkOnSuccessfulAuthentication() {
        ServerHttpResponse response = mock(ServerHttpResponse.class);

        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getResponse()).thenReturn(response);

        WebFilterExchange filterExchange = mock(WebFilterExchange.class);
        when(filterExchange.getExchange()).thenReturn(exchange);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());

        handler.onAuthenticationSuccess(filterExchange, authentication).block();
        verify(response, times(1)).setStatusCode(HttpStatus.OK);
    }
}

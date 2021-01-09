package org.briarheart.orchestra.security.web.server.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessToken;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class HttpBasicServerAuthenticationSuccessHandlerTest {
    private static final String SUBJECT = "alice";

    private HttpBasicServerAuthenticationSuccessHandler handler;
    private AccessTokenService accessTokenService;

    @BeforeEach
    void setUp() {
        AccessToken accessToken = mock(AccessToken.class);
        when(accessToken.getClaims()).thenReturn(Map.of("sub", SUBJECT));

        accessTokenService = mock(AccessTokenService.class);
        doReturn(Mono.just(accessToken)).when(accessTokenService).createAccessToken(any(), any());

        handler = new HttpBasicServerAuthenticationSuccessHandler(accessTokenService, new ObjectMapper());
    }

    @Test
    void shouldCreateAccessTokenOnSuccessfulAuthentication() {
        WebFilterExchange filterExchange = mockWebFilterExchange();
        Authentication authentication = mockAuthentication();
        handler.onAuthenticationSuccess(filterExchange, authentication).block();

        User user = (User) authentication.getPrincipal();
        ServerWebExchange exchange = filterExchange.getExchange();
        verify(accessTokenService, times(1)).createAccessToken(user, exchange);
    }

    @Test
    void shouldSetHttpStatusCodeToOkOnSuccessfulAuthentication() {
        WebFilterExchange filterExchange = mockWebFilterExchange();
        Authentication authentication = mockAuthentication();
        handler.onAuthenticationSuccess(filterExchange, authentication).block();

        ServerHttpResponse response = filterExchange.getExchange().getResponse();
        verify(response, times(1)).setStatusCode(HttpStatus.OK);
    }

    @SuppressWarnings({"unchecked"})
    @Test
    void shouldWriteAccessTokenClaimsToHttpResponseBodyOnSuccessfulAuthentication() throws IOException {
        WebFilterExchange filterExchange = mockWebFilterExchange();
        Authentication authentication = mockAuthentication();
        handler.onAuthenticationSuccess(filterExchange, authentication).block();

        ServerHttpResponse response = filterExchange.getExchange().getResponse();

        ArgumentCaptor<Mono<DataBuffer>> responseBodyPublisherCaptor = ArgumentCaptor.forClass(Mono.class);
        verify(response, times(1)).writeWith(responseBodyPublisherCaptor.capture());
        assertEquals("{\"sub\":\"" + SUBJECT + "\"}", readDataBuffer(responseBodyPublisherCaptor.getValue().block()));
    }

    private WebFilterExchange mockWebFilterExchange() {
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        when(response.getHeaders()).thenReturn(new HttpHeaders());
        when(response.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        when(response.writeWith(any())).thenReturn(Mono.just(true).then());

        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getResponse()).thenReturn(response);

        WebFilterExchange filterExchange = mock(WebFilterExchange.class);
        when(filterExchange.getExchange()).thenReturn(exchange);
        return filterExchange;
    }

    private Authentication mockAuthentication() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        return authentication;
    }

    @SuppressWarnings("UnstableApiUsage")
    private String readDataBuffer(DataBuffer buffer) throws IOException {
        if (buffer == null) {
            return null;
        }
        byte[] bytes = new byte[buffer.capacity()];
        ByteStreams.readFully(buffer.asInputStream(), bytes);
        return new String(bytes);
    }
}

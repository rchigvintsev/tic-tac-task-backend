package org.briarheart.orchestra.security.web.server.authentication;

import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessToken;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.ServerAccessTokenRepository;
import org.briarheart.orchestra.security.web.server.authentication.jwt.MockJwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class AccessTokenServerAuthenticationConverterTest {
    private AccessTokenServerAuthenticationConverter converter;
    private ServerAccessTokenRepository accessTokenRepositoryMock;

    @BeforeEach
    void setUp() {
        accessTokenRepositoryMock = mock(ServerAccessTokenRepository.class);
        converter = new AccessTokenServerAuthenticationConverter(accessTokenRepositoryMock);
    }

    @Test
    void shouldConvertConsideringAuthorizationHeaderFirst() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + MockJwts.DEFAULT_SIGNED_ACCESS_TOKEN_VALUE)
                .build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);
        Authentication authentication = converter.convert(serverWebExchangeMock).block();
        assertTrue(authentication instanceof AccessTokenAuthentication);
        assertEquals(MockJwts.DEFAULT_SIGNED_ACCESS_TOKEN_VALUE, ((AccessTokenAuthentication) authentication).getTokenValue());
    }

    @SuppressWarnings("UnassignedFluxMonoInstance")
    @Test
    void shouldConvertUsingServerAccessTokenRepository() {
        AccessToken accessTokenMock = MockJwts.createMock();
        doReturn(Mono.just(accessTokenMock)).when(accessTokenRepositoryMock).loadAccessToken(any());

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);

        Authentication authentication = converter.convert(serverWebExchangeMock).block();
        assertTrue(authentication instanceof AccessTokenAuthentication);
        assertEquals(MockJwts.DEFAULT_SIGNED_ACCESS_TOKEN_VALUE, ((AccessTokenAuthentication) authentication).getTokenValue());
    }

    @Test
    void shouldReturnNullWhenAccessTokenValueIsNotProvided() {
        when(accessTokenRepositoryMock.loadAccessToken(any())).thenReturn(Mono.empty());
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);
        Authentication authentication = converter.convert(serverWebExchangeMock).block();
        assertNull(authentication);
    }

    @Test
    void shouldReturnNullWhenAuthorizationHeaderValueDoesNotStartWithBearer() {
        when(accessTokenRepositoryMock.loadAccessToken(any())).thenReturn(Mono.empty());
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, MockJwts.DEFAULT_SIGNED_ACCESS_TOKEN_VALUE)
                .build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);
        Authentication authentication = converter.convert(serverWebExchangeMock).block();
        assertNull(authentication);
    }

    @Test
    void shouldReturnNullWhenAuthorizationHeaderValueIsInvalid() {
        when(accessTokenRepositoryMock.loadAccessToken(any())).thenReturn(Mono.empty());
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ")
                .build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);
        Authentication authentication = converter.convert(serverWebExchangeMock).block();
        assertNull(authentication);
    }
}

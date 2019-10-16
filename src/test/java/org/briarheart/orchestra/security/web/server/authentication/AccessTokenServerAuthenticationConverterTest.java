package org.briarheart.orchestra.security.web.server.authentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roman Chigvintsev
 */
class AccessTokenServerAuthenticationConverterTest {
    private static final String ACCESS_TOKEN_VALUE = "T3Q4Sko0U1puVzRmZFUwUWNHM2g=";

    private AccessTokenServerAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new AccessTokenServerAuthenticationConverter();
    }

    @Test
    void shouldConvertAuthorizationHeaderToAuthentication() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN_VALUE)
                .build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);
        Authentication authentication = converter.convert(serverWebExchangeMock).block();
        assertTrue(authentication instanceof AccessTokenAuthentication);
        assertEquals(ACCESS_TOKEN_VALUE, ((AccessTokenAuthentication) authentication).getTokenValue());
    }

    @Test
    void shouldReturnNullWhenAuthorizationHeaderIsNotProvided() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);
        Authentication authentication = converter.convert(serverWebExchangeMock).block();
        assertNull(authentication);
    }

    @Test
    void shouldReturnNullWhenAuthorizationHeaderValueDoesNotStartWithBearer() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN_VALUE)
                .build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);
        Authentication authentication = converter.convert(serverWebExchangeMock).block();
        assertNull(authentication);
    }

    @Test
    void shouldReturnNullWhenAuthorizationHeaderValueIsInvalid() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ")
                .build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);
        Authentication authentication = converter.convert(serverWebExchangeMock).block();
        assertNull(authentication);
    }
}

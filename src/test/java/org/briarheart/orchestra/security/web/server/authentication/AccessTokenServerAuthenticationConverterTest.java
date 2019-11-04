package org.briarheart.orchestra.security.web.server.authentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;

import static org.briarheart.orchestra.security.web.server.authentication.AccessTokenServerAuthenticationConverter.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Chigvintsev
 */
class AccessTokenServerAuthenticationConverterTest {
    private static final String ACCESS_TOKEN_HEADER = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9";
    private static final String ACCESS_TOKEN_PAYLOAD = "eyJzdWIiOiJKb2huIERvZSJ9";
    private static final String ACCESS_TOKEN_SIGNATURE = "U1bAs3wp14vdUsh1FE_yMEfKXa69W65i9IFV9AYxUUMTRv65L8CrwvXQU6-"
            + "jL2n0hydWm43ps9BWvHwDj3z0BQ";
    private static final String ACCESS_TOKEN_VALUE = ACCESS_TOKEN_HEADER + "." + ACCESS_TOKEN_PAYLOAD + "."
            + ACCESS_TOKEN_SIGNATURE;

    private static final String ACCESS_TOKEN_HEADER_COOKIE_NAME = DEFAULT_ACCESS_TOKEN_HEADER_COOKIE_NAME;
    private static final String ACCESS_TOKEN_PAYLOAD_COOKIE_NAME = DEFAULT_ACCESS_TOKEN_PAYLOAD_COOKIE_NAME;
    private static final String ACCESS_TOKEN_SIGNATURE_COOKIE_NAME = DEFAULT_ACCESS_TOKEN_SIGNATURE_COOKIE_NAME;

    private AccessTokenServerAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        AccessTokenService accessTokenServiceMock = mock(AccessTokenService.class);
        when(accessTokenServiceMock.composeAccessTokenValue(anyString(), anyString(), any()))
                .thenReturn(ACCESS_TOKEN_VALUE);

        converter = new AccessTokenServerAuthenticationConverter(accessTokenServiceMock);
        converter.setAccessTokenHeaderCookieName(ACCESS_TOKEN_HEADER_COOKIE_NAME);
        converter.setAccessTokenPayloadCookieName(ACCESS_TOKEN_PAYLOAD_COOKIE_NAME);
        converter.setAccessTokenSignatureCookieName(ACCESS_TOKEN_SIGNATURE_COOKIE_NAME);
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
    void shouldConvertAccessTokenCookieToAuthentication() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .cookie(new HttpCookie(ACCESS_TOKEN_HEADER_COOKIE_NAME, ACCESS_TOKEN_HEADER))
                .cookie(new HttpCookie(ACCESS_TOKEN_PAYLOAD_COOKIE_NAME, ACCESS_TOKEN_PAYLOAD))
                .cookie(new HttpCookie(ACCESS_TOKEN_SIGNATURE_COOKIE_NAME, ACCESS_TOKEN_SIGNATURE))
                .build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);
        Authentication authentication = converter.convert(serverWebExchangeMock).block();
        assertTrue(authentication instanceof AccessTokenAuthentication);
        assertEquals(ACCESS_TOKEN_VALUE, ((AccessTokenAuthentication) authentication).getTokenValue());
    }

    @Test
    void shouldConvertUnsignedAccessTokenCookieToAuthentication() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .cookie(new HttpCookie(ACCESS_TOKEN_HEADER_COOKIE_NAME, ACCESS_TOKEN_HEADER))
                .cookie(new HttpCookie(ACCESS_TOKEN_PAYLOAD_COOKIE_NAME, ACCESS_TOKEN_PAYLOAD))
                .build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);
        Authentication authentication = converter.convert(serverWebExchangeMock).block();
        assertTrue(authentication instanceof AccessTokenAuthentication);
    }

    @Test
    void shouldReturnNullWhenNeitherAuthorizationHeaderNorAccessTokenCookieIsProvided() {
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

    @Test
    void shouldReturnNullWhenAccessTokenHeaderCookieIsMissing() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .cookie(new HttpCookie(ACCESS_TOKEN_PAYLOAD_COOKIE_NAME, ACCESS_TOKEN_PAYLOAD))
                .build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);
        Authentication authentication = converter.convert(serverWebExchangeMock).block();
        assertNull(authentication);
    }

    @Test
    void shouldReturnNullWhenAccessTokenPayloadCookieIsMissing() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .cookie(new HttpCookie(ACCESS_TOKEN_HEADER_COOKIE_NAME, ACCESS_TOKEN_HEADER))
                .build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);
        Authentication authentication = converter.convert(serverWebExchangeMock).block();
        assertNull(authentication);
    }
}

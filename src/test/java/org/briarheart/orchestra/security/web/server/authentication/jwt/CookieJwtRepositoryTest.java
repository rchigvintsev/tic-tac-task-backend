package org.briarheart.orchestra.security.web.server.authentication.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.MultiValueMap;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roman Chigvintsev
 */
class CookieJwtRepositoryTest {
    private static final String ACCESS_TOKEN_HEADER_COOKIE_NAME = "ATH";
    private static final String ACCESS_TOKEN_PAYLOAD_COOKIE_NAME = "ATP";
    private static final String ACCESS_TOKEN_SIGNATURE_COOKIE_NAME = "ATS";

    private static final Duration ACCESS_TOKEN_TIMEOUT = Duration.ofDays(7);

    private CookieJwtRepository repository;

    @BeforeEach
    void setUp() {
        repository = new CookieJwtRepository();
        repository.setAccessTokenHeaderCookieName(ACCESS_TOKEN_HEADER_COOKIE_NAME);
        repository.setAccessTokenPayloadCookieName(ACCESS_TOKEN_PAYLOAD_COOKIE_NAME);
        repository.setAccessTokenSignatureCookieName(ACCESS_TOKEN_SIGNATURE_COOKIE_NAME);
    }

    @Test
    void shouldSaveAccessTokenInCookies() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        repository.saveAccessToken(MockJwts.createMock(), webExchangeMock).block();

        MultiValueMap<String, ResponseCookie> cookies = webExchangeMock.getResponse().getCookies();

        ResponseCookie headerCookie = cookies.getFirst(ACCESS_TOKEN_HEADER_COOKIE_NAME);
        assertNotNull(headerCookie);
        assertEquals(MockJwts.DEFAULT_SIGNED_ACCESS_TOKEN_HEADER, headerCookie.getValue());

        ResponseCookie payloadCookie = cookies.getFirst(ACCESS_TOKEN_PAYLOAD_COOKIE_NAME);
        assertNotNull(payloadCookie);
        assertEquals(MockJwts.DEFAULT_ACCESS_TOKEN_PAYLOAD, payloadCookie.getValue());

        ResponseCookie signatureCookie = cookies.getFirst(ACCESS_TOKEN_SIGNATURE_COOKIE_NAME);
        assertNotNull(signatureCookie);
        assertEquals(MockJwts.DEFAULT_ACCESS_TOKEN_SIGNATURE, signatureCookie.getValue());
    }

    @Test
    void shouldSaveUnsignedAccessTokenInCookies() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        repository.saveAccessToken(MockJwts.createUnsignedMock(), webExchangeMock).block();

        MultiValueMap<String, ResponseCookie> cookies = webExchangeMock.getResponse().getCookies();

        ResponseCookie headerCookie = cookies.getFirst(ACCESS_TOKEN_HEADER_COOKIE_NAME);
        assertNotNull(headerCookie);
        assertEquals(MockJwts.DEFAULT_UNSIGNED_ACCESS_TOKEN_HEADER, headerCookie.getValue());

        ResponseCookie payloadCookie = cookies.getFirst(ACCESS_TOKEN_PAYLOAD_COOKIE_NAME);
        assertNotNull(payloadCookie);
        assertEquals(MockJwts.DEFAULT_ACCESS_TOKEN_PAYLOAD, payloadCookie.getValue());

        ResponseCookie signatureCookie = cookies.getFirst(ACCESS_TOKEN_SIGNATURE_COOKIE_NAME);
        assertNull(signatureCookie);
    }

    @Test
    void shouldMakeAccessTokenCookieHttpOnly() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        repository.saveAccessToken(MockJwts.createMock(), webExchangeMock).block();

        MultiValueMap<String, ResponseCookie> cookies = webExchangeMock.getResponse().getCookies();

        ResponseCookie headerCookie = cookies.getFirst(ACCESS_TOKEN_HEADER_COOKIE_NAME);
        assertNotNull(headerCookie);
        assertTrue(headerCookie.isHttpOnly());

        ResponseCookie signatureCookie = cookies.getFirst(ACCESS_TOKEN_SIGNATURE_COOKIE_NAME);
        assertNotNull(signatureCookie);
        assertTrue(signatureCookie.isHttpOnly());
    }

    @Test
    void shouldLimitAgeOfAccessTokenCookieByExpirationTimeoutOfAccessToken() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        repository.saveAccessToken(MockJwts.createMock(), webExchangeMock).block();

        MultiValueMap<String, ResponseCookie> cookies = webExchangeMock.getResponse().getCookies();

        ResponseCookie headerCookie = cookies.getFirst(ACCESS_TOKEN_HEADER_COOKIE_NAME);
        assertNotNull(headerCookie);
        assertEquals(ACCESS_TOKEN_TIMEOUT, headerCookie.getMaxAge());

        ResponseCookie payloadCookie = cookies.getFirst(ACCESS_TOKEN_PAYLOAD_COOKIE_NAME);
        assertNotNull(payloadCookie);
        assertEquals(ACCESS_TOKEN_TIMEOUT, payloadCookie.getMaxAge());

        ResponseCookie signatureCookie = cookies.getFirst(ACCESS_TOKEN_SIGNATURE_COOKIE_NAME);
        assertNotNull(signatureCookie);
        assertEquals(ACCESS_TOKEN_TIMEOUT, signatureCookie.getMaxAge());
    }

    @Test
    void shouldLoadAccessTokenFromCookies() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .cookie(new HttpCookie(ACCESS_TOKEN_HEADER_COOKIE_NAME, MockJwts.DEFAULT_SIGNED_ACCESS_TOKEN_HEADER))
                .cookie(new HttpCookie(ACCESS_TOKEN_PAYLOAD_COOKIE_NAME, MockJwts.DEFAULT_ACCESS_TOKEN_PAYLOAD))
                .cookie(new HttpCookie(ACCESS_TOKEN_SIGNATURE_COOKIE_NAME, MockJwts.DEFAULT_ACCESS_TOKEN_SIGNATURE))
                .build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);
        Jwt jwt = repository.loadAccessToken(serverWebExchangeMock).block();
        assertNotNull(jwt);
        assertEquals(MockJwts.DEFAULT_SIGNED_ACCESS_TOKEN_VALUE, jwt.getTokenValue());
    }

    @Test
    void shouldLoadUnsignedAccessTokenFromCookies() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .cookie(new HttpCookie(ACCESS_TOKEN_HEADER_COOKIE_NAME, MockJwts.DEFAULT_UNSIGNED_ACCESS_TOKEN_HEADER))
                .cookie(new HttpCookie(ACCESS_TOKEN_PAYLOAD_COOKIE_NAME, MockJwts.DEFAULT_ACCESS_TOKEN_PAYLOAD))
                .build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);
        Jwt jwt = repository.loadAccessToken(serverWebExchangeMock).block();
        assertNotNull(jwt);
        assertTrue(jwt.getSignature().isEmpty());
    }

    @Test
    void shouldReturnNullOnLoadWhenAccessTokenHeaderCookieIsMissing() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .cookie(new HttpCookie(ACCESS_TOKEN_PAYLOAD_COOKIE_NAME, MockJwts.DEFAULT_ACCESS_TOKEN_PAYLOAD))
                .build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);
        Jwt jwt = repository.loadAccessToken(serverWebExchangeMock).block();
        assertNull(jwt);
    }

    @Test
    void shouldReturnNullOnLoadWhenAccessTokenPayloadCookieIsMissing() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .cookie(new HttpCookie(ACCESS_TOKEN_HEADER_COOKIE_NAME, MockJwts.DEFAULT_SIGNED_ACCESS_TOKEN_HEADER))
                .build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);
        Jwt jwt = repository.loadAccessToken(serverWebExchangeMock).block();
        assertNull(jwt);
    }

    @Test
    void shouldRemoveAccessTokenFromCookies() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .cookie(new HttpCookie(ACCESS_TOKEN_HEADER_COOKIE_NAME, MockJwts.DEFAULT_SIGNED_ACCESS_TOKEN_HEADER))
                .cookie(new HttpCookie(ACCESS_TOKEN_PAYLOAD_COOKIE_NAME, MockJwts.DEFAULT_ACCESS_TOKEN_PAYLOAD))
                .build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);

        repository.removeAccessToken(serverWebExchangeMock).block();

        MultiValueMap<String, ResponseCookie> responseCookies = serverWebExchangeMock.getResponse().getCookies();

        ResponseCookie headerCookie = responseCookies.getFirst(ACCESS_TOKEN_HEADER_COOKIE_NAME);
        assertNotNull(headerCookie);
        assertTrue(headerCookie.getValue().isEmpty());
        assertEquals(Duration.ZERO, headerCookie.getMaxAge());

        ResponseCookie payloadCookie = responseCookies.getFirst(ACCESS_TOKEN_PAYLOAD_COOKIE_NAME);
        assertNotNull(payloadCookie);
        assertTrue(payloadCookie.getValue().isEmpty());
        assertEquals(Duration.ZERO, payloadCookie.getMaxAge());

        ResponseCookie signatureCookie = responseCookies.getFirst(ACCESS_TOKEN_SIGNATURE_COOKIE_NAME);
        assertNotNull(signatureCookie);
        assertTrue(signatureCookie.getValue().isEmpty());
        assertEquals(Duration.ZERO, signatureCookie.getMaxAge());
    }
}

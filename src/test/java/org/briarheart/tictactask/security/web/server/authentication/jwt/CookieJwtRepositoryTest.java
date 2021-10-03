package org.briarheart.tictactask.security.web.server.authentication.jwt;

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
    private static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    private static final Duration ACCESS_TOKEN_TIMEOUT = Duration.ofDays(7);

    private CookieJwtRepository repository;

    @BeforeEach
    void setUp() {
        repository = new CookieJwtRepository();
        repository.setAccessTokenCookieName(ACCESS_TOKEN_COOKIE_NAME);
    }

    @Test
    void shouldSaveAccessTokenInCookies() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        repository.saveAccessToken(TestJwts.create(), webExchangeMock).block();

        MultiValueMap<String, ResponseCookie> cookies = webExchangeMock.getResponse().getCookies();
        ResponseCookie accessTokenCookie = cookies.getFirst(ACCESS_TOKEN_COOKIE_NAME);
        assertNotNull(accessTokenCookie);
        assertEquals(TestJwts.DEFAULT_ACCESS_TOKEN_VALUE, accessTokenCookie.getValue());
    }

    @Test
    void shouldThrowExceptionOnAccessTokenSaveWhenAccessTokenIsNull() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> repository.saveAccessToken(null, webExchangeMock));
        assertEquals("Access token must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionOnAccessTokenSaveWhenWebExchangeIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> repository.saveAccessToken(TestJwts.create(), null));
        assertEquals("Server web exchange must not be null", exception.getMessage());
    }

    @Test
    void shouldMakeAccessTokenCookieHttpOnly() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        repository.saveAccessToken(TestJwts.create(), webExchangeMock).block();

        MultiValueMap<String, ResponseCookie> cookies = webExchangeMock.getResponse().getCookies();
        ResponseCookie accessTokenCookie = cookies.getFirst(ACCESS_TOKEN_COOKIE_NAME);
        assertNotNull(accessTokenCookie);
        assertTrue(accessTokenCookie.isHttpOnly());
    }

    @Test
    void shouldLimitAgeOfAccessTokenCookieByExpirationTimeoutOfAccessToken() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        repository.saveAccessToken(TestJwts.create(), webExchangeMock).block();

        MultiValueMap<String, ResponseCookie> cookies = webExchangeMock.getResponse().getCookies();
        ResponseCookie accessTokenCookie = cookies.getFirst(ACCESS_TOKEN_COOKIE_NAME);
        assertNotNull(accessTokenCookie);
        assertEquals(ACCESS_TOKEN_TIMEOUT, accessTokenCookie.getMaxAge());
    }

    @Test
    void shouldLoadAccessTokenFromCookies() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .cookie(new HttpCookie(ACCESS_TOKEN_COOKIE_NAME, TestJwts.DEFAULT_ACCESS_TOKEN_VALUE))
                .build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);
        Jwt jwt = repository.loadAccessToken(serverWebExchangeMock).block();
        assertNotNull(jwt);
        assertEquals(TestJwts.DEFAULT_ACCESS_TOKEN_VALUE, jwt.getTokenValue());
    }

    @Test
    void shouldReturnNullOnAccessTokenLoadWhenAccessTokenCookieIsNotFound() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);
        Jwt jwt = repository.loadAccessToken(serverWebExchangeMock).block();
        assertNull(jwt);
    }

    @Test
    void shouldThrowExceptionOnAccessTokenLoadWhenWebExchangeIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> repository.loadAccessToken(null));
        assertEquals("Server web exchange must not be null", exception.getMessage());
    }

    @Test
    void shouldRemoveAccessTokenFromCookies() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .cookie(new HttpCookie(ACCESS_TOKEN_COOKIE_NAME, TestJwts.DEFAULT_ACCESS_TOKEN_VALUE))
                .build();
        MockServerWebExchange serverWebExchangeMock = MockServerWebExchange.from(requestMock);

        repository.removeAccessToken(serverWebExchangeMock).block();

        MultiValueMap<String, ResponseCookie> responseCookies = serverWebExchangeMock.getResponse().getCookies();

        ResponseCookie accessTokenCookie = responseCookies.getFirst(ACCESS_TOKEN_COOKIE_NAME);
        assertNotNull(accessTokenCookie);
        assertTrue(accessTokenCookie.getValue().isEmpty());
        assertEquals(Duration.ZERO, accessTokenCookie.getMaxAge());
    }

    @Test
    void shouldThrowExceptionOnAccessTokenRemoveWhenWebExchangeIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> repository.removeAccessToken(null));
        assertEquals("Server web exchange must not be null", exception.getMessage());
    }
}

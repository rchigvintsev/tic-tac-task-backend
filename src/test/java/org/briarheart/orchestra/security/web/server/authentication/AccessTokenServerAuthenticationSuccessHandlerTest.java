package org.briarheart.orchestra.security.web.server.authentication;

import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.security.oauth2.core.user.OAuth2UserAttributeAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.briarheart.orchestra.security.web.server.authentication.AccessTokenServerAuthenticationConverter.*;
import static org.briarheart.orchestra.security.web.server.authentication.AccessTokenServerAuthenticationSuccessHandler.DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Chigvintsev
 */
class AccessTokenServerAuthenticationSuccessHandlerTest {
    private static final String ACCESS_TOKEN_HEADER = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9";
    private static final String ACCESS_TOKEN_PAYLOAD = "eyJzdWIiOiJKb2huIERvZSJ9";
    private static final String ACCESS_TOKEN_SIGNATURE = "U1bAs3wp14vdUsh1FE_yMEfKXa69W65i9IFV9AYxUUMTRv65L8CrwvXQU6-"
            + "jL2n0hydWm43ps9BWvHwDj3z0BQ";
    private static final String ACCESS_TOKEN_VALUE = ACCESS_TOKEN_HEADER + "." + ACCESS_TOKEN_PAYLOAD + "."
            + ACCESS_TOKEN_SIGNATURE;

    private static final String ACCESS_TOKEN_HEADER_COOKIE_NAME = DEFAULT_ACCESS_TOKEN_HEADER_COOKIE_NAME;
    private static final String ACCESS_TOKEN_PAYLOAD_COOKIE_NAME = DEFAULT_ACCESS_TOKEN_PAYLOAD_COOKIE_NAME;
    private static final String ACCESS_TOKEN_SIGNATURE_COOKIE_NAME = DEFAULT_ACCESS_TOKEN_SIGNATURE_COOKIE_NAME;

    private static final Duration ACCESS_TOKEN_TIMEOUT = Duration.ofDays(7);

    private ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepositoryMock;
    private UserRepository userRepositoryMock;

    private AccessTokenServerAuthenticationSuccessHandler handler;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        authorizationRequestRepositoryMock = mock(ServerAuthorizationRequestRepository.class);
        userRepositoryMock = mock(UserRepository.class);

        AccessToken accessTokenMock = mock(AccessToken.class);
        when(accessTokenMock.getHeader()).thenReturn(ACCESS_TOKEN_HEADER);
        when(accessTokenMock.getPayload()).thenReturn(ACCESS_TOKEN_PAYLOAD);
        when(accessTokenMock.getSignature()).thenReturn(ACCESS_TOKEN_SIGNATURE);
        when(accessTokenMock.getTokenValue()).thenReturn(ACCESS_TOKEN_VALUE);

        Instant now = Instant.now();
        when(accessTokenMock.getIssuedAt()).thenReturn(now);
        when(accessTokenMock.getExpiration()).thenReturn(now.plus(ACCESS_TOKEN_TIMEOUT));

        AccessTokenService accessTokenServiceMock = mock(AccessTokenService.class);
        when(accessTokenServiceMock.createAccessToken(any())).thenReturn(accessTokenMock);

        handler = new AccessTokenServerAuthenticationSuccessHandler(authorizationRequestRepositoryMock,
                userRepositoryMock, accessTokenServiceMock);
        handler.setClientRedirectUriParameterName("client-redirect-uri");
        handler.setAccessTokenHeaderCookieName(ACCESS_TOKEN_HEADER_COOKIE_NAME);
        handler.setAccessTokenPayloadCookieName(ACCESS_TOKEN_PAYLOAD_COOKIE_NAME);
        handler.setAccessTokenSignatureCookieName(ACCESS_TOKEN_SIGNATURE_COOKIE_NAME);
    }

    @Test
    void shouldRedirectToUriSpecifiedByClient() {
        final String CLIENT_REDIRECT_URI = "http://callback.me";
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("http://authorize.me")
                .additionalParameters(Map.of(DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME, CLIENT_REDIRECT_URI))
                .build();
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(authorizationRequest));
        when(userRepositoryMock.findById(anyString())).thenReturn(Mono.just(new User()));

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        WebFilterExchange webFilterExchangeMock = mock(WebFilterExchange.class);
        when(webFilterExchangeMock.getExchange()).thenReturn(webExchangeMock);

        OAuth2UserAttributeAccessor attrAccessorMock = mock(OAuth2UserAttributeAccessor.class);
        when(attrAccessorMock.getEmail()).thenReturn("white.rabbit@mail.com");

        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getPrincipal()).thenReturn(attrAccessorMock);

        handler.onAuthenticationSuccess(webFilterExchangeMock, authenticationMock).block();
        URI location = webExchangeMock.getResponse().getHeaders().getLocation();
        assertNotNull(location);
        assertEquals(CLIENT_REDIRECT_URI, location.toString());
    }

    @Test
    void shouldPassAccessTokenInCookies() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("http://authorize.me")
                .additionalParameters(Map.of(DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME, "http://callback.me"))
                .build();
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(authorizationRequest));
        when(userRepositoryMock.findById(anyString())).thenReturn(Mono.just(new User()));

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        WebFilterExchange webFilterExchangeMock = mock(WebFilterExchange.class);
        when(webFilterExchangeMock.getExchange()).thenReturn(webExchangeMock);

        OAuth2UserAttributeAccessor attrAccessorMock = mock(OAuth2UserAttributeAccessor.class);
        when(attrAccessorMock.getEmail()).thenReturn("white.rabbit@mail.com");

        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getPrincipal()).thenReturn(attrAccessorMock);

        handler.onAuthenticationSuccess(webFilterExchangeMock, authenticationMock).block();

        MultiValueMap<String, ResponseCookie> cookies = webExchangeMock.getResponse().getCookies();

        ResponseCookie headerCookie = cookies.getFirst(ACCESS_TOKEN_HEADER_COOKIE_NAME);
        assertNotNull(headerCookie);
        assertEquals(ACCESS_TOKEN_HEADER, headerCookie.getValue());

        ResponseCookie payloadCookie = cookies.getFirst(ACCESS_TOKEN_PAYLOAD_COOKIE_NAME);
        assertNotNull(payloadCookie);
        assertEquals(ACCESS_TOKEN_PAYLOAD, payloadCookie.getValue());

        ResponseCookie signatureCookie = cookies.getFirst(ACCESS_TOKEN_SIGNATURE_COOKIE_NAME);
        assertNotNull(signatureCookie);
        assertEquals(ACCESS_TOKEN_SIGNATURE, signatureCookie.getValue());
    }

    @Test
    void shouldMakeAccessTokenCookieHttpOnly() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("http://authorize.me")
                .additionalParameters(Map.of(DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME, "http://callback.me"))
                .build();
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(authorizationRequest));
        when(userRepositoryMock.findById(anyString())).thenReturn(Mono.just(new User()));

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        WebFilterExchange webFilterExchangeMock = mock(WebFilterExchange.class);
        when(webFilterExchangeMock.getExchange()).thenReturn(webExchangeMock);

        OAuth2UserAttributeAccessor attrAccessorMock = mock(OAuth2UserAttributeAccessor.class);
        when(attrAccessorMock.getEmail()).thenReturn("white.rabbit@mail.com");

        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getPrincipal()).thenReturn(attrAccessorMock);

        handler.onAuthenticationSuccess(webFilterExchangeMock, authenticationMock).block();

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
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("http://authorize.me")
                .additionalParameters(Map.of(DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME, "http://callback.me"))
                .build();
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(authorizationRequest));
        when(userRepositoryMock.findById(anyString())).thenReturn(Mono.just(new User()));

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        WebFilterExchange webFilterExchangeMock = mock(WebFilterExchange.class);
        when(webFilterExchangeMock.getExchange()).thenReturn(webExchangeMock);

        OAuth2UserAttributeAccessor attrAccessorMock = mock(OAuth2UserAttributeAccessor.class);
        when(attrAccessorMock.getEmail()).thenReturn("white.rabbit@mail.com");

        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getPrincipal()).thenReturn(attrAccessorMock);

        handler.onAuthenticationSuccess(webFilterExchangeMock, authenticationMock).block();

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
    void shouldThrowExceptionWhenAuthorizationRequestIsNotAvailable() {
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any())).thenReturn(Mono.empty());

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        WebFilterExchange webFilterExchangeMock = mock(WebFilterExchange.class);
        when(webFilterExchangeMock.getExchange()).thenReturn(webExchangeMock);

        Authentication authenticationMock = mock(Authentication.class);

        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class, () ->
                handler.onAuthenticationSuccess(webFilterExchangeMock, authenticationMock).block());
        assertEquals("Failed to load authorization request", e.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenClientRedirectUriIsNotDetermined() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("http://authorize.me")
                .build();
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(authorizationRequest));

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        WebFilterExchange webFilterExchangeMock = mock(WebFilterExchange.class);
        when(webFilterExchangeMock.getExchange()).thenReturn(webExchangeMock);

        Authentication authenticationMock = mock(Authentication.class);

        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class, () ->
                handler.onAuthenticationSuccess(webFilterExchangeMock, authenticationMock).block());
        assertEquals("Failed to determine client redirect URI", e.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUserIsNotFound() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("http://authorize.me")
                .additionalParameters(Map.of(DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME, "http://callback.me"))
                .build();
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(authorizationRequest));
        when(userRepositoryMock.findById(anyString())).thenReturn(Mono.empty());

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        WebFilterExchange webFilterExchangeMock = mock(WebFilterExchange.class);
        when(webFilterExchangeMock.getExchange()).thenReturn(webExchangeMock);

        OAuth2UserAttributeAccessor attrAccessorMock = mock(OAuth2UserAttributeAccessor.class);
        when(attrAccessorMock.getEmail()).thenReturn("white.rabbit@mail.com");

        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getPrincipal()).thenReturn(attrAccessorMock);

        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class, () ->
                handler.onAuthenticationSuccess(webFilterExchangeMock, authenticationMock).block());
        assertTrue(e.getMessage().startsWith("User is not found by email "));
    }
}

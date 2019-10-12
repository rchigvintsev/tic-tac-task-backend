package org.briarheart.orchestra.security.web.server.authentication;

import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.WebFilterExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

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
    private static final String ACCESS_TOKEN_VALUE = "eyJ0eXAiOiJKV1Qi.eyJpc3MiOiJUZXN0IElzc3VlciIs._bgerZbhKkdqLcp8N9";

    private ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepositoryMock;
    private UserRepository userRepositoryMock;

    private AccessTokenServerAuthenticationSuccessHandler handler;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        authorizationRequestRepositoryMock = mock(ServerAuthorizationRequestRepository.class);
        userRepositoryMock = mock(UserRepository.class);

        AccessToken accessTokenMock = mock(AccessToken.class);
        when(accessTokenMock.getTokenValue()).thenReturn(ACCESS_TOKEN_VALUE);

        AccessTokenService accessTokenServiceMock = mock(AccessTokenService.class);
        when(accessTokenServiceMock.createAccessToken(any())).thenReturn(accessTokenMock);

        handler = new AccessTokenServerAuthenticationSuccessHandler(authorizationRequestRepositoryMock,
                userRepositoryMock, accessTokenServiceMock);
        handler.setClientRedirectUriParameterName("client-redirect-uri");
    }

    @Test
    void shouldPassAccessTokenInRedirectUriQueryString() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("http://authorize.me")
                .additionalParameters(Map.of("client-redirect-uri", "http://callback.me"))
                .build();
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(authorizationRequest));
        when(userRepositoryMock.findById(anyString())).thenReturn(Mono.just(new User()));

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        WebFilterExchange webFilterExchangeMock = mock(WebFilterExchange.class);
        when(webFilterExchangeMock.getExchange()).thenReturn(webExchangeMock);

        OidcUser oidcUserMock = mock(OidcUser.class);
        when(oidcUserMock.getEmail()).thenReturn("white.rabbit@mail.com");
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getPrincipal()).thenReturn(oidcUserMock);

        handler.onAuthenticationSuccess(webFilterExchangeMock, authenticationMock).block();
        URI location = webExchangeMock.getResponse().getHeaders().getLocation();
        assertNotNull(location);
        assertEquals("access-token=" + ACCESS_TOKEN_VALUE, location.getQuery());
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

        OidcUser oidcUserMock = mock(OidcUser.class);
        when(oidcUserMock.getEmail()).thenReturn("white.rabbit@mail.com");
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getPrincipal()).thenReturn(oidcUserMock);

        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class, () ->
                handler.onAuthenticationSuccess(webFilterExchangeMock, authenticationMock).block());
        assertTrue(e.getMessage().startsWith("User is not found by email "));
    }
}

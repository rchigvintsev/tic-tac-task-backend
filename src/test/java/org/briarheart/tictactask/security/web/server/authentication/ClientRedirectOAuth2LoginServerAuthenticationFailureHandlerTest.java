package org.briarheart.tictactask.security.web.server.authentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.server.WebFilterExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

import static org.briarheart.tictactask.security.web.server.authentication.AbstractClientRedirectOAuth2LoginServerAuthenticationHandler.DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Chigvintsev
 */
class ClientRedirectOAuth2LoginServerAuthenticationFailureHandlerTest {
    private static final String CLIENT_REDIRECT_URI = "https://callback.me";

    private ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepositoryMock;
    private ClientRedirectOAuth2LoginServerAuthenticationFailureHandler handler;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        authorizationRequestRepositoryMock = mock(ServerAuthorizationRequestRepository.class);
        handler = new ClientRedirectOAuth2LoginServerAuthenticationFailureHandler(authorizationRequestRepositoryMock,
                CLIENT_REDIRECT_URI);
        handler.setClientRedirectUriParameterName(DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME);
    }

    @Test
    void shouldSendRedirect() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("https://authorize.me")
                .additionalParameters(Map.of(DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME, CLIENT_REDIRECT_URI))
                .build();
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(authorizationRequest));

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        WebFilterExchange webFilterExchangeMock = mock(WebFilterExchange.class);
        when(webFilterExchangeMock.getExchange()).thenReturn(webExchangeMock);

        OAuth2Error oAuth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST);
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(oAuth2Error);

        handler.onAuthenticationFailure(webFilterExchangeMock, exception).block();
        HttpStatus statusCode = webExchangeMock.getResponse().getStatusCode();
        assertEquals(HttpStatus.FOUND, statusCode);
    }

    @Test
    void shouldAppendErrorQueryParameterToClientSpecifiedRedirectUri() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("https://authorize.me")
                .additionalParameters(Map.of(DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME, CLIENT_REDIRECT_URI))
                .build();
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(authorizationRequest));

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        WebFilterExchange webFilterExchangeMock = mock(WebFilterExchange.class);
        when(webFilterExchangeMock.getExchange()).thenReturn(webExchangeMock);

        AuthenticationException exceptionMock = mock(AuthenticationException.class);

        handler.onAuthenticationFailure(webFilterExchangeMock, exceptionMock).block();
        URI location = webExchangeMock.getResponse().getHeaders().getLocation();
        assertNotNull(location);
        assertEquals(CLIENT_REDIRECT_URI + "?error=true", location.toString());
    }

    @Test
    void shouldNotAppendErrorQueryParameterToRedirectUriWhenUserDeniedAccess() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("https://authorize.me")
                .additionalParameters(Map.of(DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME, CLIENT_REDIRECT_URI))
                .build();
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(authorizationRequest));

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        WebFilterExchange webFilterExchangeMock = mock(WebFilterExchange.class);
        when(webFilterExchangeMock.getExchange()).thenReturn(webExchangeMock);

        OAuth2Error oAuth2Error = new OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED);
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(oAuth2Error);

        handler.onAuthenticationFailure(webFilterExchangeMock, exception).block();
        URI location = webExchangeMock.getResponse().getHeaders().getLocation();
        assertNotNull(location);
        assertEquals(CLIENT_REDIRECT_URI, location.toString());
    }

    @Test
    void shouldThrowExceptionWhenAuthorizationRequestIsNotAvailable() {
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any())).thenReturn(Mono.empty());

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        WebFilterExchange webFilterExchangeMock = mock(WebFilterExchange.class);
        when(webFilterExchangeMock.getExchange()).thenReturn(webExchangeMock);

        AuthenticationException exceptionMock = mock(AuthenticationException.class);

        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class, () ->
                handler.onAuthenticationFailure(webFilterExchangeMock, exceptionMock).block());
        assertEquals("Failed to load authorization request", e.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenClientRedirectUriIsNotDetermined() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("https://authorize.me")
                .build();
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(authorizationRequest));

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        WebFilterExchange webFilterExchangeMock = mock(WebFilterExchange.class);
        when(webFilterExchangeMock.getExchange()).thenReturn(webExchangeMock);

        AuthenticationException exceptionMock = mock(AuthenticationException.class);

        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class, () ->
                handler.onAuthenticationFailure(webFilterExchangeMock, exceptionMock).block());
        assertEquals("Failed to determine client redirect URI", e.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenClientRedirectUriIsNotValid() {
        String clientRedirectUri = "https://invalid.uri";
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("https://authorize.me")
                .additionalParameters(Map.of(DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME, clientRedirectUri))
                .build();
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(authorizationRequest));

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        WebFilterExchange webFilterExchangeMock = mock(WebFilterExchange.class);
        when(webFilterExchangeMock.getExchange()).thenReturn(webExchangeMock);

        AuthenticationException exceptionMock = mock(AuthenticationException.class);

        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class, () ->
                handler.onAuthenticationFailure(webFilterExchangeMock, exceptionMock).block());
        assertEquals("Redirect to \"" + clientRedirectUri + "\" is not allowed", e.getMessage());
    }
}

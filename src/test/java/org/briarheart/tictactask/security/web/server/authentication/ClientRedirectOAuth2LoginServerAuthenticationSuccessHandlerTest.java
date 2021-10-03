package org.briarheart.tictactask.security.web.server.authentication;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.briarheart.tictactask.data.UserRepository;
import org.briarheart.tictactask.model.User;
import org.briarheart.tictactask.security.oauth2.core.user.OAuth2UserAttributeAccessor;
import org.briarheart.tictactask.security.web.server.authentication.accesstoken.AccessToken;
import org.briarheart.tictactask.security.web.server.authentication.accesstoken.AccessTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

import static org.briarheart.tictactask.security.web.server.authentication.ClientRedirectOAuth2LoginServerAuthenticationSuccessHandler.DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class ClientRedirectOAuth2LoginServerAuthenticationSuccessHandlerTest {
    private static final String CLIENT_REDIRECT_URI = "https://callback.me";

    private ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepositoryMock;
    private UserRepository userRepositoryMock;
    private AccessTokenService accessTokenServiceMock;
    private ObjectMapper objectMapperMock;

    private ClientRedirectOAuth2LoginServerAuthenticationSuccessHandler handler;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        authorizationRequestRepositoryMock = mock(ServerAuthorizationRequestRepository.class);
        userRepositoryMock = mock(UserRepository.class);
        AccessToken accessTokenMock = mock(AccessToken.class);
        accessTokenServiceMock = mock(AccessTokenService.class);
        doReturn(Mono.just(accessTokenMock)).when(accessTokenServiceMock).createAccessToken(any(), any());
        objectMapperMock = spy(ObjectMapper.class);

        handler = new ClientRedirectOAuth2LoginServerAuthenticationSuccessHandler(authorizationRequestRepositoryMock,
                CLIENT_REDIRECT_URI, userRepositoryMock, accessTokenServiceMock, objectMapperMock);
        handler.setClientRedirectUriParameterName(DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME);
    }

    @Test
    void shouldRedirectToUriSpecifiedByClient() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("https://authorize.me")
                .additionalParameters(Map.of(DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME, CLIENT_REDIRECT_URI))
                .build();
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(authorizationRequest));
        when(userRepositoryMock.findByEmail(anyString())).thenReturn(Mono.just(new User()));

        WebFilterExchange webFilterExchangeMock = mockWebFilterExchange();
        Authentication authenticationMock = mockAuthentication();

        handler.onAuthenticationSuccess(webFilterExchangeMock, authenticationMock).block();
        URI location = webFilterExchangeMock.getExchange().getResponse().getHeaders().getLocation();
        assertNotNull(location);
        assertTrue(location.toString().startsWith(CLIENT_REDIRECT_URI));
    }

    @Test
    void shouldCreateAccessToken() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("https://authorize.me")
                .additionalParameters(Map.of(DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME, CLIENT_REDIRECT_URI))
                .build();
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(authorizationRequest));
        User user = new User();
        when(userRepositoryMock.findByEmail(anyString())).thenReturn(Mono.just(user));

        WebFilterExchange webFilterExchangeMock = mockWebFilterExchange();
        Authentication authenticationMock = mockAuthentication();

        handler.onAuthenticationSuccess(webFilterExchangeMock, authenticationMock).block();
        URI location = webFilterExchangeMock.getExchange().getResponse().getHeaders().getLocation();
        assertNotNull(location);
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUri(location).build().getQueryParams();
        assertNotNull(queryParams.get("access_token_claims"));
    }

    @Test
    void shouldIncludeAccessTokenClaimsInRedirectUri() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("https://authorize.me")
                .additionalParameters(Map.of(DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME, CLIENT_REDIRECT_URI))
                .build();
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(authorizationRequest));
        User user = new User();
        when(userRepositoryMock.findByEmail(anyString())).thenReturn(Mono.just(user));

        WebFilterExchange webFilterExchangeMock = mockWebFilterExchange();
        Authentication authenticationMock = mockAuthentication();

        handler.onAuthenticationSuccess(webFilterExchangeMock, authenticationMock).block();
        verify(accessTokenServiceMock, times(1)).createAccessToken(user, webFilterExchangeMock.getExchange());
        verifyNoMoreInteractions(accessTokenServiceMock);
    }

    @Test
    void shouldThrowExceptionWhenAuthorizationRequestIsNotAvailable() {
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any())).thenReturn(Mono.empty());
        WebFilterExchange webFilterExchangeMock = mockWebFilterExchange();
        Authentication authenticationMock = mock(Authentication.class);
        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class, () ->
                handler.onAuthenticationSuccess(webFilterExchangeMock, authenticationMock).block());
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

        WebFilterExchange webFilterExchangeMock = mockWebFilterExchange();
        Authentication authenticationMock = mock(Authentication.class);

        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class, () ->
                handler.onAuthenticationSuccess(webFilterExchangeMock, authenticationMock).block());
        assertEquals("Failed to determine client redirect URI", e.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUserIsNotFound() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("https://authorize.me")
                .additionalParameters(Map.of(DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME, CLIENT_REDIRECT_URI))
                .build();
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(authorizationRequest));
        when(userRepositoryMock.findByEmail(anyString())).thenReturn(Mono.empty());

        WebFilterExchange webFilterExchangeMock = mockWebFilterExchange();
        Authentication authenticationMock = mockAuthentication();

        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class, () ->
                handler.onAuthenticationSuccess(webFilterExchangeMock, authenticationMock).block());
        assertTrue(e.getMessage().startsWith("User is not found by email "));
    }

    @Test
    void shouldThrowExceptionWhenClaimsCouldNotBeSerialized() throws JsonProcessingException {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("https://authorize.me")
                .additionalParameters(Map.of(DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME, CLIENT_REDIRECT_URI))
                .build();
        when(authorizationRequestRepositoryMock.loadAuthorizationRequest(any()))
                .thenReturn(Mono.just(authorizationRequest));
        when(userRepositoryMock.findByEmail(anyString())).thenReturn(Mono.just(new User()));
        doThrow(new JsonGenerationException("Could not generate JSON", (JsonGenerator) null))
                .when(objectMapperMock).writeValueAsBytes(any());

        WebFilterExchange webFilterExchangeMock = mockWebFilterExchange();
        Authentication authenticationMock = mockAuthentication();

        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class, () ->
                handler.onAuthenticationSuccess(webFilterExchangeMock, authenticationMock).block());
        assertEquals("Failed to serialize access token claims", e.getError().getDescription());
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

        WebFilterExchange webFilterExchangeMock = mockWebFilterExchange();
        Authentication authenticationMock = mock(Authentication.class);

        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class, () ->
                handler.onAuthenticationSuccess(webFilterExchangeMock, authenticationMock).block());
        assertEquals("Redirect to \"" + clientRedirectUri + "\" is not allowed", e.getMessage());
    }

    private WebFilterExchange mockWebFilterExchange() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        WebFilterExchange webFilterExchangeMock = mock(WebFilterExchange.class);
        when(webFilterExchangeMock.getExchange()).thenReturn(webExchangeMock);
        return webFilterExchangeMock;
    }

    private Authentication mockAuthentication() {
        OAuth2UserAttributeAccessor attrAccessorMock = mock(OAuth2UserAttributeAccessor.class);
        when(attrAccessorMock.getEmail()).thenReturn("white.rabbit@mail.com");
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getPrincipal()).thenReturn(attrAccessorMock);
        return authenticationMock;
    }
}

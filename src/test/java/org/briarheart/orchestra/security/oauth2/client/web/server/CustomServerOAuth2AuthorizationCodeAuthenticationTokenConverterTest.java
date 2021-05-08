package org.briarheart.orchestra.security.oauth2.client.web.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Chigvintsev
 */
class CustomServerOAuth2AuthorizationCodeAuthenticationTokenConverterTest {
    private static final String REGISTRATION_ID = "test";
    private static final String CLIENT_ID = "wSN8ZMHhUB";

    private ReactiveClientRegistrationRepository clientRegistrationRepository;
    private OAuth2AuthorizationResponseConverter authorizationResponseConverter;
    private ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;
    private CustomServerOAuth2AuthorizationCodeAuthenticationTokenConverter converter;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        clientRegistrationRepository = mock(ReactiveClientRegistrationRepository.class);
        authorizationResponseConverter = mock(OAuth2AuthorizationResponseConverter.class);
        authorizationRequestRepository = mock(ServerAuthorizationRequestRepository.class);
        Map<String, OAuth2AuthorizationResponseConverter> responseConverters
                = Map.of(REGISTRATION_ID, authorizationResponseConverter);
        converter = new CustomServerOAuth2AuthorizationCodeAuthenticationTokenConverter(clientRegistrationRepository,
                responseConverters);
        converter.setAuthorizationRequestRepository(authorizationRequestRepository);
    }

    @Test
    void shouldThrowExceptionOnConstructWhenClientRegistrationRepositoryIsNull() {
        Map<String, OAuth2AuthorizationResponseConverter> responseConverters
                = Map.of(REGISTRATION_ID, authorizationResponseConverter);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new CustomServerOAuth2AuthorizationCodeAuthenticationTokenConverter(null, responseConverters));
        assertEquals("Client registration repository must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnConstructWhenAuthorizationResponseConvertersAreNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new CustomServerOAuth2AuthorizationCodeAuthenticationTokenConverter(clientRegistrationRepository,
                        null));
        assertEquals("OAuth 2.0 authorization response converters must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnAuthorizationRequestRepositorySetWhenRepositoryIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> converter.setAuthorizationRequestRepository(null));
        assertEquals("Authorization request repository must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnDefaultAuthorizationResponseConverterSetWhenConverterIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> converter.setDefaultAuthorizationResponseConverter(null));
        assertEquals("Default OAuth 2.0 authorization response converter must not be null", e.getMessage());
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldConvert() {
        String authorizationUri = "https://server.com/authorize";
        String tokenUri = "https://server.com/token";
        String redirectUri = "https://client.com/authorization/callback";
        String redirectUriTemplate = "https://client.com/authorization/callback?code={code}";

        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getQueryParams()).thenReturn(new LinkedMultiValueMap<>());
        when(request.getURI()).thenReturn(URI.create(redirectUri));

        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getRequest()).thenReturn(request);

        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId(CLIENT_ID)
                .authorizationUri(authorizationUri)
                .redirectUri(redirectUri)
                .attributes(Map.of(OAuth2ParameterNames.REGISTRATION_ID, REGISTRATION_ID))
                .build();
        when(authorizationRequestRepository.removeAuthorizationRequest(exchange))
                .thenReturn(Mono.just(authorizationRequest));

        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientId(CLIENT_ID)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUriTemplate(redirectUriTemplate)
                .authorizationUri(authorizationUri)
                .tokenUri(tokenUri)
                .build();
        when(clientRegistrationRepository.findByRegistrationId(REGISTRATION_ID))
                .thenReturn(Mono.just(clientRegistration));

        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success("pBqTFlVe2P")
                .redirectUri(redirectUri)
                .build();
        when(authorizationResponseConverter.convert(any(MultiValueMap.class), anyString()))
                .thenReturn(authorizationResponse);

        Authentication authentication = converter.convert(exchange).block();
        assertNotNull(authentication);
    }

    @Test
    void shouldThrowExceptionOnConvertWhenAuthorizationRequestIsNotFound() {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(authorizationRequestRepository.removeAuthorizationRequest(exchange)).thenReturn(Mono.empty());

        OAuth2AuthorizationException e = assertThrows(OAuth2AuthorizationException.class,
                () -> converter.convert(exchange).block());
        assertEquals("authorization_request_not_found", e.getError().getErrorCode());
    }

    @Test
    void shouldThrowExceptionOnConvertWhenRegistrationIdIsNotFoundInRequestAttributes() {
        String authorizationUri = "https://server.com/authorize";
        String redirectUri = "https://client.com/authorization/callback";

        ServerWebExchange exchange = mock(ServerWebExchange.class);

        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId(CLIENT_ID)
                .authorizationUri(authorizationUri)
                .redirectUri(redirectUri)
                .attributes(Map.of())
                .build();
        when(authorizationRequestRepository.removeAuthorizationRequest(exchange))
                .thenReturn(Mono.just(authorizationRequest));

        OAuth2AuthorizationException e = assertThrows(OAuth2AuthorizationException.class,
                () -> converter.convert(exchange).block());
        assertEquals("client_registration_not_found", e.getError().getErrorCode());
    }

    @Test
    void shouldThrowExceptionOnConvertWhenRegistrationRegistrationIsNotFoundById() {
        String authorizationUri = "https://server.com/authorize";
        String redirectUri = "https://client.com/authorization/callback";

        ServerWebExchange exchange = mock(ServerWebExchange.class);

        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId(CLIENT_ID)
                .authorizationUri(authorizationUri)
                .redirectUri(redirectUri)
                .attributes(Map.of(OAuth2ParameterNames.REGISTRATION_ID, REGISTRATION_ID))
                .build();
        when(authorizationRequestRepository.removeAuthorizationRequest(exchange))
                .thenReturn(Mono.just(authorizationRequest));

        when(clientRegistrationRepository.findByRegistrationId(REGISTRATION_ID)).thenReturn(Mono.empty());

        OAuth2AuthorizationException e = assertThrows(OAuth2AuthorizationException.class,
                () -> converter.convert(exchange).block());
        assertEquals("client_registration_not_found", e.getError().getErrorCode());
    }
}

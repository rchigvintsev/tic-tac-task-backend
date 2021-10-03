package org.briarheart.tictactask.security.oauth2.client.web.server;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionOAuth2ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Converts from a {@link ServerWebExchange} to an {@link OAuth2LoginAuthenticationToken} that can be authenticated.
 * This converter uses {@link OAuth2AuthorizationResponseConverter} strategy to convert OAuth 2.0 authorization
 * callback request to instance of {@link OAuth2AuthorizationResponse}.
 *
 * @author Roman Chigvintsev
 */
public class CustomServerOAuth2AuthorizationCodeAuthenticationTokenConverter
        implements ServerAuthenticationConverter {
    private static final String AUTHORIZATION_REQUEST_NOT_FOUND_ERROR_CODE = "authorization_request_not_found";
    private static final String CLIENT_REGISTRATION_NOT_FOUND_ERROR_CODE = "client_registration_not_found";

    private final ReactiveClientRegistrationRepository clientRegistrationRepository;
    private final Map<String, OAuth2AuthorizationResponseConverter> authorizationResponseConverters;

    private ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository
            = new WebSessionOAuth2ServerAuthorizationRequestRepository();
    private OAuth2AuthorizationResponseConverter defaultAuthorizationResponseConverter
            = new DefaultOAuth2AuthorizationResponseConverter();

    public CustomServerOAuth2AuthorizationCodeAuthenticationTokenConverter(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            Map<String, OAuth2AuthorizationResponseConverter> authorizationResponseConverters
    ) {
        Assert.notNull(clientRegistrationRepository, "Client registration repository must not be null");
        Assert.notNull(authorizationResponseConverters, "OAuth 2.0 authorization response converters must not be null");

        this.clientRegistrationRepository = clientRegistrationRepository;
        this.authorizationResponseConverters = authorizationResponseConverters;
    }

    /**
     * Sets the {@link ServerAuthorizationRequestRepository} to be used. The default is
     * {@link WebSessionOAuth2ServerAuthorizationRequestRepository}.
     *
     * @param authorizationRequestRepository the repository to use
     */
    public void setAuthorizationRequestRepository(
            ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository
    ) {
        Assert.notNull(authorizationRequestRepository, "Authorization request repository must not be null");
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    public void setDefaultAuthorizationResponseConverter(
            OAuth2AuthorizationResponseConverter defaultAuthorizationResponseConverter
    ) {
        Assert.notNull(defaultAuthorizationResponseConverter,
                "Default OAuth 2.0 authorization response converter must not be null");
        this.defaultAuthorizationResponseConverter = defaultAuthorizationResponseConverter;
    }

    @Override
    public Mono<Authentication> convert(ServerWebExchange serverWebExchange) {
        return authorizationRequestRepository.removeAuthorizationRequest(serverWebExchange)
                .switchIfEmpty(oauth2AuthorizationException(AUTHORIZATION_REQUEST_NOT_FOUND_ERROR_CODE))
                .flatMap(authorizationRequest -> authenticationRequest(serverWebExchange, authorizationRequest));
    }

    private <T> Mono<T> oauth2AuthorizationException(String errorCode) {
        return Mono.error(new OAuth2AuthorizationException(new OAuth2Error(errorCode)));
    }

    private Mono<OAuth2AuthorizationCodeAuthenticationToken> authenticationRequest(
            ServerWebExchange exchange,
            OAuth2AuthorizationRequest authorizationRequest
    ) {
        return Mono.just(authorizationRequest)
                .map(OAuth2AuthorizationRequest::getAttributes)
                .flatMap(attributes -> {
                    String id = (String) attributes.get(OAuth2ParameterNames.REGISTRATION_ID);
                    if (id == null) {
                        return oauth2AuthorizationException(CLIENT_REGISTRATION_NOT_FOUND_ERROR_CODE);
                    }
                    return clientRegistrationRepository.findByRegistrationId(id);
                })
                .switchIfEmpty(oauth2AuthorizationException(CLIENT_REGISTRATION_NOT_FOUND_ERROR_CODE))
                .map(clientRegistration -> {
                    OAuth2AuthorizationResponse authorizationResponse = convertResponse(clientRegistration, exchange);
                    return new OAuth2AuthorizationCodeAuthenticationToken(
                            clientRegistration,
                            new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse)
                    );
                });
    }

    private OAuth2AuthorizationResponse convertResponse(ClientRegistration clientRegistration,
                                                        ServerWebExchange exchange) {
        MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
        String redirectUri = UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
                .query(null)
                .build()
                .toUriString();
        OAuth2AuthorizationResponseConverter responseConverter = authorizationResponseConverters.getOrDefault(
                clientRegistration.getRegistrationId(),
                defaultAuthorizationResponseConverter
        );
        return responseConverter.convert(queryParams, redirectUri);
    }
}

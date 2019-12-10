package org.briarheart.orchestra.security.web.server.authentication;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Base class for reactive authentication handlers that perform redirect to URI specified by client.
 * <p>
 * This handler takes URI for redirect from additional parameters of saved authorization request. By default
 * {@link #DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME} is used as parameter key. If there is no saved authorization
 * request or URI for redirect is not found in the loaded authorization request {@link OAuth2AuthenticationException}
 * with code {@link OAuth2ErrorCodes#INVALID_REQUEST} will be thrown.
 *
 * @author Roman Chigvintsev
 */
@RequiredArgsConstructor
public abstract class AbstractClientRedirectUriServerAuthenticationHandler {
    public static final String DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME = "client-redirect-uri";

    private final ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;

    private String clientRedirectUriParameterName = DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME;

    @Getter
    @Setter
    @NonNull
    private ServerRedirectStrategy redirectStrategy = new DefaultServerRedirectStrategy();

    public void setClientRedirectUriParameterName(String clientRedirectUriParameterName) {
        Assert.hasText(clientRedirectUriParameterName, "Client redirect URI parameter name must not be null or empty");
        this.clientRedirectUriParameterName = clientRedirectUriParameterName;
    }

    protected Mono<String> determineRedirectLocation(ServerWebExchange exchange) {
        return authorizationRequestRepository.loadAuthorizationRequest(exchange)
                .switchIfEmpty(Mono.error(() -> {
                    OAuth2Error oAuth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST,
                            "Failed to load authorization request", null);
                    return new OAuth2AuthenticationException(oAuth2Error);
                }))
                .flatMap(this::getRedirectLocation)
                .switchIfEmpty(Mono.error(() -> {
                    OAuth2Error oAuth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST,
                            "Failed to determine client redirect URI", null);
                    return new OAuth2AuthenticationException(oAuth2Error);
                }));
    }

    private Mono<String> getRedirectLocation(OAuth2AuthorizationRequest authorizationRequest) {
        Object location = authorizationRequest.getAdditionalParameters().get(clientRedirectUriParameterName);
        // TODO: check that client redirect URI is authorized
        return location == null ? Mono.empty() : Mono.just(location.toString());
    }
}

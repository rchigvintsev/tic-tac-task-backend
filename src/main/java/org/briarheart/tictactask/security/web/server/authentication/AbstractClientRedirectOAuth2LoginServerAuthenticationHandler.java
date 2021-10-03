package org.briarheart.tictactask.security.web.server.authentication;

import lombok.Getter;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Base class for reactive authentication handlers that perform redirect to URI specified by client.
 * <p>
 * This handler takes URI for redirect from additional parameters of saved authorization request. By default
 * {@link #DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME} is used as parameter key. If there is no saved authorization
 * request, URI for redirect is not found in the loaded authorization request or redirect to the specified URI is not
 * allowed {@link OAuth2AuthenticationException} with code {@link OAuth2ErrorCodes#INVALID_REQUEST} will be thrown.
 *
 * @author Roman Chigvintsev
 */
public abstract class AbstractClientRedirectOAuth2LoginServerAuthenticationHandler {
    public static final String DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME = "client-redirect-uri";

    private final ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;
    private final String clientRedirectUriTemplate;
    private final PathMatcher pathMatcher = new AntPathMatcher();

    private String clientRedirectUriParameterName = DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME;
    @Getter
    private ServerRedirectStrategy redirectStrategy = new DefaultServerRedirectStrategy();

    protected AbstractClientRedirectOAuth2LoginServerAuthenticationHandler(
            ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository,
            String clientRedirectUriTemplate
    ) {
        Assert.notNull(authorizationRequestRepository, "Authorization request repository must not be null");
        Assert.hasText(clientRedirectUriTemplate, "Client redirect URI template must not be null or empty");

        this.authorizationRequestRepository = authorizationRequestRepository;
        this.clientRedirectUriTemplate = clientRedirectUriTemplate;
    }

    public void setClientRedirectUriParameterName(String clientRedirectUriParameterName) {
        Assert.hasText(clientRedirectUriParameterName, "Client redirect URI parameter name must not be null or empty");
        this.clientRedirectUriParameterName = clientRedirectUriParameterName;
    }

    public void setRedirectStrategy(ServerRedirectStrategy redirectStrategy) {
        Assert.notNull(redirectStrategy, "Redirect strategy must not be null");
        this.redirectStrategy = redirectStrategy;
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
                }))
                .map(this::validateRedirectLocation);
    }

    private Mono<String> getRedirectLocation(OAuth2AuthorizationRequest authorizationRequest) {
        Object location = authorizationRequest.getAdditionalParameters().get(clientRedirectUriParameterName);
        return Mono.justOrEmpty(Objects.toString(location, null));
    }

    private String validateRedirectLocation(String location) {
        if (!pathMatcher.match(clientRedirectUriTemplate, location)) {
            OAuth2Error oauth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST,
                    "Redirect to \"" + location + "\" is not allowed", null);
            throw new OAuth2AuthenticationException(oauth2Error);
        }
        return location;
    }
}

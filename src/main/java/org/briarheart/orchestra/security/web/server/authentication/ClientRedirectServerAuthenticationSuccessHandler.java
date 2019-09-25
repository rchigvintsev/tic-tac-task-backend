package org.briarheart.orchestra.security.web.server.authentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * This handler performs a redirect on authentication success. Location to redirect is taken from the saved OAuth2
 * authorization request. Particularly from request's additional parameters.
 *
 * @author Roman Chigvintsev
 *
 * @see OAuth2AuthorizationRequest
 */
public class ClientRedirectServerAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {
    private final ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;
    private final String clientRedirectUriParameterName;

    private ServerRedirectStrategy redirectStrategy = new DefaultServerRedirectStrategy();

    /**
     * Creates new instance of this class with the given authorization request repository and client redirect URI
     * parameter name.
     *
     * @param authorizationRequestRepository given authorization request repository (must not be {@code null})
     * @param clientRedirectUriParameterName client redirect URI parameter name (must not be {@code null} or empty)
     */
    public ClientRedirectServerAuthenticationSuccessHandler(
            ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository,
            String clientRedirectUriParameterName
    ) {
        Assert.notNull(authorizationRequestRepository, "Authorization request repository must not be null");
        Assert.hasText(clientRedirectUriParameterName, "Client redirect URI parameter name must not be null or empty");

        this.authorizationRequestRepository = authorizationRequestRepository;
        this.clientRedirectUriParameterName = clientRedirectUriParameterName;
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        return authorizationRequestRepository.loadAuthorizationRequest(exchange)
                .switchIfEmpty(Mono.error(() -> {
                    OAuth2Error oAuth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST,
                            "Failed to load authorization request", null);
                    return new OAuth2AuthenticationException(oAuth2Error);
                }))
                .flatMap(authorizationRequest -> {
                    Object location = authorizationRequest.getAdditionalParameters()
                            .get(clientRedirectUriParameterName);
                    if (location == null) {
                        return Mono.error(() -> {
                            OAuth2Error oAuth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST,
                                    "Failed to determine client redirect URI", null);
                            return new OAuth2AuthenticationException(oAuth2Error);
                        });
                    }
                    return redirectStrategy.sendRedirect(exchange, URI.create(location.toString()));
                });
    }

    /**
     * Sets redirect strategy to use.
     *
     * @param redirectStrategy redirect strategy to use
     */
    public void setRedirectStrategy(ServerRedirectStrategy redirectStrategy) {
        Assert.notNull(redirectStrategy, "Redirect strategy cannot be null");
        this.redirectStrategy = redirectStrategy;
    }
}

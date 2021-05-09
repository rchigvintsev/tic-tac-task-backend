package org.briarheart.orchestra.security.web.server.authentication;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

/**
 * This handler appends query parameter &quot;error&quot; with value &quot;true&quot; to the client specified redirect
 * URI and then performs a redirect. If the given authentication exception is an instance of
 * {@link OAuth2AuthenticationException} with error code {@link OAuth2ErrorCodes#ACCESS_DENIED} query parameter
 * &quot;error&quot; will not be added and redirect will be performed to the original redirect URI.
 *
 * @author Roman Chigvintsev
 */
@Slf4j
public class ClientRedirectOAuth2LoginServerAuthenticationFailureHandler
        extends AbstractClientRedirectOAuth2LoginServerAuthenticationHandler
        implements ServerAuthenticationFailureHandler {
    public ClientRedirectOAuth2LoginServerAuthenticationFailureHandler(
            ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository,
            String clientRedirectUriTemplate
    ) {
        super(authorizationRequestRepository, clientRedirectUriTemplate);
    }

    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange webFilterExchange, AuthenticationException exception) {
        log.debug(exception.getMessage(), exception);
        ServerWebExchange serverExchange = webFilterExchange.getExchange();
        return determineRedirectLocation(serverExchange)
                .map(location -> {
                    if (exception instanceof OAuth2AuthenticationException) {
                        OAuth2Error oAuth2Error = ((OAuth2AuthenticationException) exception).getError();
                        if (OAuth2ErrorCodes.ACCESS_DENIED.equals(oAuth2Error.getErrorCode())) {
                            // Just return user to login page if he denied access
                            return URI.create(location);
                        }
                    }
                    return UriComponentsBuilder.fromUriString(location).queryParam("error", "true").build(Map.of());
                })
                .flatMap(location -> getRedirectStrategy().sendRedirect(serverExchange, location));
    }
}

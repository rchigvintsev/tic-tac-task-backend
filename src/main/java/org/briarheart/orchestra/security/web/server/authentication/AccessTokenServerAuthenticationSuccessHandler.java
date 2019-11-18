package org.briarheart.orchestra.security.web.server.authentication;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.security.oauth2.core.user.OAuth2UserAttributeAccessor;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessToken;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessTokenService;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.ServerAccessTokenRepository;
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
import reactor.util.function.Tuple2;

import java.net.URI;

/**
 * This handler issues an access token and then performs a redirect passing token value in the response.
 * <p>
 * Location to redirect is taken from the saved OAuth2 authorization request. Particularly from request's additional
 * parameters.
 *
 * @author Roman Chigvintsev
 *
 * @see OAuth2AuthorizationRequest
 */
@RequiredArgsConstructor
public class AccessTokenServerAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {
    public static final String DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME = "client-redirect-uri";

    private final ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;
    private final UserRepository userRepository;
    private final AccessTokenService accessTokenService;
    private final ServerAccessTokenRepository accessTokenRepository;

    private String clientRedirectUriParameterName = DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME;

    @Setter
    @NonNull
    private ServerRedirectStrategy redirectStrategy = new DefaultServerRedirectStrategy();

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
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
                .zipWhen(location -> findUser(authentication))
                .switchIfEmpty(Mono.error(() -> {
                    OAuth2UserAttributeAccessor attrAccessor;
                    attrAccessor = (OAuth2UserAttributeAccessor) authentication.getPrincipal();
                    String errorMessage = "User is not found by email \"" + attrAccessor.getEmail() + "\"";
                    OAuth2Error oAuth2Error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR, errorMessage, null);
                    return new OAuth2AuthenticationException(oAuth2Error);
                }))
                .flatMap(locationAndUser -> saveAccessTokenAndSendRedirect(exchange, locationAndUser));
    }

    public void setClientRedirectUriParameterName(String clientRedirectUriParameterName) {
        Assert.hasText(clientRedirectUriParameterName, "Client redirect URI parameter name must not be null or empty");
        this.clientRedirectUriParameterName = clientRedirectUriParameterName;
    }

    private Mono<? extends String> getRedirectLocation(OAuth2AuthorizationRequest authorizationRequest) {
        Object location = authorizationRequest.getAdditionalParameters()
                .get(clientRedirectUriParameterName);
        // TODO: check that client redirect URI is authorized
        return location == null ? Mono.empty() : Mono.just(location.toString());
    }

    private Mono<? extends User> findUser(Authentication authentication) {
        return userRepository.findById(((OAuth2UserAttributeAccessor) authentication.getPrincipal()).getEmail());
    }

    private Mono<? extends Void> saveAccessTokenAndSendRedirect(
            ServerWebExchange exchange,
            Tuple2<? extends String, ? extends User> locationAndUser
    ) {
        AccessToken accessToken = accessTokenService.createAccessToken(locationAndUser.getT2());
        return accessTokenRepository.saveAccessToken(accessToken, exchange)
                .then(redirectStrategy.sendRedirect(exchange, URI.create(locationAndUser.getT1())));
    }
}

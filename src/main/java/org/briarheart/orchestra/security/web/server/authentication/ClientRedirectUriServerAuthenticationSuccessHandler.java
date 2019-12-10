package org.briarheart.orchestra.security.web.server.authentication;

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
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.net.URI;

/**
 * This handler issues an access token and then performs a redirect passing token value in the response.
 *
 * @author Roman Chigvintsev
 *
 * @see OAuth2AuthorizationRequest
 */
public class ClientRedirectUriServerAuthenticationSuccessHandler
        extends AbstractClientRedirectUriServerAuthenticationHandler
        implements ServerAuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final AccessTokenService accessTokenService;
    private final ServerAccessTokenRepository accessTokenRepository;

    public ClientRedirectUriServerAuthenticationSuccessHandler(
            ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository,
            UserRepository userRepository,
            AccessTokenService accessTokenService,
            ServerAccessTokenRepository accessTokenRepository
    ) {
        super(authorizationRequestRepository);
        this.userRepository = userRepository;
        this.accessTokenService = accessTokenService;
        this.accessTokenRepository = accessTokenRepository;
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        return determineRedirectLocation(exchange)
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

    private Mono<? extends User> findUser(Authentication authentication) {
        return userRepository.findById(((OAuth2UserAttributeAccessor) authentication.getPrincipal()).getEmail());
    }

    private Mono<? extends Void> saveAccessTokenAndSendRedirect(
            ServerWebExchange exchange,
            Tuple2<? extends String, ? extends User> locationAndUser
    ) {
        AccessToken accessToken = accessTokenService.createAccessToken(locationAndUser.getT2());
        return accessTokenRepository.saveAccessToken(accessToken, exchange)
                .then(getRedirectStrategy().sendRedirect(exchange, URI.create(locationAndUser.getT1())));
    }
}

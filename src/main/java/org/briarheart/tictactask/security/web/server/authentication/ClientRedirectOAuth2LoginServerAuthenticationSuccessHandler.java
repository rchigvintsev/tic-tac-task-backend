package org.briarheart.tictactask.security.web.server.authentication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.briarheart.tictactask.security.oauth2.core.user.OAuth2UserAttributeAccessor;
import org.briarheart.tictactask.security.web.server.authentication.accesstoken.AccessToken;
import org.briarheart.tictactask.security.web.server.authentication.accesstoken.AccessTokenService;
import org.briarheart.tictactask.user.User;
import org.briarheart.tictactask.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.net.URI;
import java.util.Base64;
import java.util.Collections;

/**
 * This handler issues an access token and then performs a redirect passing token value in the response.
 * Additionally it includes BASE64-encoded JSON-representation of access token claims in redirect URI as
 * a query parameter with name "access_token_claims".
 *
 * @author Roman Chigvintsev
 * @see OAuth2AuthorizationRequest
 */
public class ClientRedirectOAuth2LoginServerAuthenticationSuccessHandler
        extends AbstractClientRedirectOAuth2LoginServerAuthenticationHandler
        implements ServerAuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final AccessTokenService accessTokenService;
    private final ObjectMapper objectMapper;

    public ClientRedirectOAuth2LoginServerAuthenticationSuccessHandler(
            ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository,
            String clientRedirectUriTemplate,
            UserRepository userRepository,
            AccessTokenService accessTokenService,
            ObjectMapper objectMapper
    ) {
        super(authorizationRequestRepository, clientRedirectUriTemplate);
        this.userRepository = userRepository;
        this.accessTokenService = accessTokenService;
        this.objectMapper = objectMapper;
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
                .zipWhen(
                        locationAndUser -> accessTokenService.createAccessToken(locationAndUser.getT2(), exchange),
                        (locationAndUser, accessToken) -> Tuples.of(locationAndUser.getT1(), accessToken)
                )
                .flatMap(locationAndToken
                        -> addClientPrincipalToRedirectUri(locationAndToken.getT1(), locationAndToken.getT2()))
                .onErrorMap(JsonProcessingException.class, e -> {
                    String errorMessage = "Failed to serialize access token claims";
                    OAuth2Error oAuth2Error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR, errorMessage, null);
                    return new OAuth2AuthenticationException(oAuth2Error, e);
                })
                .flatMap(location -> sendRedirect(exchange, location));
    }

    private Mono<User> findUser(Authentication authentication) {
        return userRepository.findByEmail(((OAuth2UserAttributeAccessor) authentication.getPrincipal()).getEmail());
    }

    private Mono<URI> addClientPrincipalToRedirectUri(String redirectLocation, AccessToken accessToken) {
        return Mono.fromCallable(() -> {
            byte[] claims = objectMapper.writeValueAsBytes(accessToken.getClaims());
            return UriComponentsBuilder.fromUriString(redirectLocation)
                    .queryParam("access_token_claims", Base64.getUrlEncoder().encodeToString(claims))
                    .build(Collections.emptyMap());
        });
    }

    private Mono<Void> sendRedirect(ServerWebExchange exchange, URI location) {
        return getRedirectStrategy().sendRedirect(exchange, location);
    }
}

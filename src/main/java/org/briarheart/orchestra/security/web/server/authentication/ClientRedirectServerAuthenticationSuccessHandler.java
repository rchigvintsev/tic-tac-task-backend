package org.briarheart.orchestra.security.web.server.authentication;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.NonNull;
import lombok.Setter;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * This handler issues an access token and then performs a redirect on authentication success passing token in query
 * string. Location to redirect is taken from the saved OAuth2 authorization request. Particularly from request's
 * additional parameters.
 *
 * @author Roman Chigvintsev
 *
 * @see OAuth2AuthorizationRequest
 */
public class ClientRedirectServerAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {
    public static final String DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME = "client-redirect-uri";

    private static final long DEFAULT_ACCESS_TOKEN_VALIDITY_SECONDS = 300;

    private final ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;
    private final UserRepository userRepository;
    private final String accessTokenSigningKey;

    private String clientRedirectUriParameterName = DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME;

    @Setter
    private long accessTokenValiditySeconds = DEFAULT_ACCESS_TOKEN_VALIDITY_SECONDS;

    @Setter
    @NonNull
    private ServerRedirectStrategy redirectStrategy = new DefaultServerRedirectStrategy();

    /**
     * Creates new instance of this class with the given authorization request repository, user repository and
     * access token signing key.
     *
     * @param authorizationRequestRepository given authorization request repository (must not be {@code null})
     * @param userRepository user repository (must not be {@code null})
     * @param accessTokenSigningKey access token signing key (must not be {@code null} or empty)
     */
    public ClientRedirectServerAuthenticationSuccessHandler(
            ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository,
            UserRepository userRepository,
            String accessTokenSigningKey
    ) {
        Assert.notNull(authorizationRequestRepository, "Authorization request repository must not be null");
        Assert.notNull(userRepository, "User repository must not be null");
        Assert.hasText(accessTokenSigningKey, "Access token signing key must not be null or empty");

        this.authorizationRequestRepository = authorizationRequestRepository;
        this.userRepository = userRepository;
        this.accessTokenSigningKey = accessTokenSigningKey;
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
                .flatMap(this::getRedirectLocation)
                .switchIfEmpty(Mono.error(() -> {
                    OAuth2Error oAuth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST,
                            "Failed to determine client redirect URI", null);
                    return new OAuth2AuthenticationException(oAuth2Error);
                }))
                .zipWhen(location -> findUser(authentication))
                .switchIfEmpty(Mono.error(() -> {
                    OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
                    OAuth2Error oAuth2Error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
                            "User is not found by email '" + oidcUser.getEmail() + "'", null);
                    return new OAuth2AuthenticationException(oAuth2Error);
                }))
                .flatMap(locationAndUser -> sendRedirect(exchange, locationAndUser));
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
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        return userRepository.findById(oidcUser.getEmail());
    }

    private Mono<? extends Void> sendRedirect(ServerWebExchange exchange,
                                              Tuple2<? extends String, ? extends User> locationAndUser) {
        URI redirectUri = UriComponentsBuilder.fromUriString(locationAndUser.getT1())
                .queryParam("access-token", buildAccessToken(locationAndUser.getT2()))
                .build()
                .toUri();
        return redirectStrategy.sendRedirect(exchange, redirectUri);
    }

    private String buildAccessToken(User user) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime expiration = now.plus(accessTokenValiditySeconds, ChronoUnit.SECONDS);
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(Date.from(now.toInstant(ZoneOffset.UTC)))
                .setExpiration(Date.from(expiration.toInstant(ZoneOffset.UTC)))
                .signWith(SignatureAlgorithm.HS512, accessTokenSigningKey)
                .compact();
    }
}

package org.briarheart.orchestra.security.web.server.authentication;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.security.oauth2.core.user.OAuth2UserAttributeAccessor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
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
import java.time.Duration;

import static org.briarheart.orchestra.security.web.server.authentication.AccessTokenServerAuthenticationConverter.*;

/**
 * This handler issues an access token and then performs a redirect passing token value in the response.
 * <p>
 * Handler splits token value into three parts - header, payload and signature - and stores those parts in cookies with
 * names {@link #accessTokenHeaderCookieName}, {@link #accessTokenPayloadCookieName} and
 * {@link #accessTokenSignatureCookieName} correspondingly. Each cookie except payload cookie has {@code httpOnly} flag
 * being set to {@code true} and age of each cookie is limited by expiration timeout of an access token.
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

    private String accessTokenHeaderCookieName = DEFAULT_ACCESS_TOKEN_HEADER_COOKIE_NAME;
    private String accessTokenPayloadCookieName = DEFAULT_ACCESS_TOKEN_PAYLOAD_COOKIE_NAME;
    private String accessTokenSignatureCookieName = DEFAULT_ACCESS_TOKEN_SIGNATURE_COOKIE_NAME;

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
                .flatMap(locationAndUser -> sendRedirect(exchange, locationAndUser));
    }

    public void setClientRedirectUriParameterName(String clientRedirectUriParameterName) {
        Assert.hasText(clientRedirectUriParameterName, "Client redirect URI parameter name must not be null or empty");
        this.clientRedirectUriParameterName = clientRedirectUriParameterName;
    }

    public void setAccessTokenHeaderCookieName(String accessTokenHeaderCookieName) {
        Assert.hasText(accessTokenHeaderCookieName, "Access token header cookie name must not be null or empty");
        this.accessTokenHeaderCookieName = accessTokenHeaderCookieName;
    }

    public void setAccessTokenPayloadCookieName(String accessTokenPayloadCookieName) {
        Assert.hasText(accessTokenPayloadCookieName, "Access token payload cookie name must not be null or empty");
        this.accessTokenPayloadCookieName = accessTokenPayloadCookieName;
    }

    public void setAccessTokenSignatureCookieName(String accessTokenSignatureCookieName) {
        Assert.hasText(accessTokenSignatureCookieName, "Access token signature cookie name must not be null or empty");
        this.accessTokenSignatureCookieName = accessTokenSignatureCookieName;
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

    private Mono<? extends Void> sendRedirect(ServerWebExchange exchange,
                                              Tuple2<? extends String, ? extends User> locationAndUser) {
        ServerHttpResponse response = exchange.getResponse();
        AccessToken accessToken = accessTokenService.createAccessToken(locationAndUser.getT2());
        Duration maxAge = Duration.between(accessToken.getIssuedAt(), accessToken.getExpiration());
        ResponseCookie headerCookie = ResponseCookie.from(accessTokenHeaderCookieName, accessToken.getHeader())
                .path("/")
                .httpOnly(true)
                .maxAge(maxAge)
                .build();
        response.addCookie(headerCookie);
        ResponseCookie payloadCookie = ResponseCookie.from(accessTokenPayloadCookieName, accessToken.getPayload())
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addCookie(payloadCookie);
        ResponseCookie signatureCookie = ResponseCookie.from(accessTokenSignatureCookieName, accessToken.getSignature())
                .path("/")
                .httpOnly(true)
                .maxAge(maxAge)
                .build();
        response.addCookie(signatureCookie);
        return redirectStrategy.sendRedirect(exchange, URI.create(locationAndUser.getT1()));
    }
}

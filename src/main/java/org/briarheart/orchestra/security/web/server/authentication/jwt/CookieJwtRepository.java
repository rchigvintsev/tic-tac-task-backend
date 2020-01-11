package org.briarheart.orchestra.security.web.server.authentication.jwt;

import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessToken;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.ServerAccessTokenRepository;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Implementation of {@link ServerAccessTokenRepository} that stores access tokens in cookies.
 * <p>
 * This repository creates cookie with {@code httpOnly} flag being set to {@code true} and age limited by expiration
 * timeout of an access token.
 *
 * @author Roman Chigvintsev
 * @see ServerAccessTokenRepository
 */
public class CookieJwtRepository implements ServerAccessTokenRepository {
    private static final String DEFAULT_ACCESS_TOKEN_COOKIE_NAME = "access_token";

    private String accessTokenCookieName = DEFAULT_ACCESS_TOKEN_COOKIE_NAME;

    @Override
    public Mono<Jwt> loadAccessToken(ServerWebExchange exchange) {
        Assert.notNull(exchange, "Server web exchange must not be null");
        return Mono.fromCallable(() -> {
            MultiValueMap<String, HttpCookie> cookies = exchange.getRequest().getCookies();
            HttpCookie accessTokenCookie = cookies.getFirst(accessTokenCookieName);
            if (accessTokenCookie != null) {
                return new Jwt.Builder(accessTokenCookie.getValue()).build();
            }
            return null;
        });
    }

    @Override
    public Mono<? extends AccessToken> saveAccessToken(AccessToken accessToken, ServerWebExchange exchange) {
        Assert.notNull(accessToken, "Access token must not be null");
        Assert.notNull(exchange, "Server web exchange must not be null");

        return Mono.fromCallable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            Duration maxAge = Duration.between(accessToken.getIssuedAt(), accessToken.getExpiration());
            ResponseCookie accessTokenCookie = ResponseCookie.from(accessTokenCookieName, accessToken.getTokenValue())
                    .path("/")
                    .httpOnly(true)
                    .maxAge(maxAge)
                    .build();
            response.addCookie(accessTokenCookie);
            return accessToken;
        });
    }

    @Override
    public Mono<Jwt> removeAccessToken(ServerWebExchange exchange) {
        return loadAccessToken(exchange).map(token -> {
            ServerHttpResponse response = exchange.getResponse();
            ResponseCookie accessTokenCookie = ResponseCookie.from(accessTokenCookieName, "")
                    .path("/")
                    .httpOnly(true)
                    .maxAge(0)
                    .build();
            response.addCookie(accessTokenCookie);
            return token;
        });
    }

    public void setAccessTokenCookieName(String accessTokenCookieName) {
        Assert.hasText(accessTokenCookieName, "Access token cookie name must not be null or empty");
        this.accessTokenCookieName = accessTokenCookieName;
    }
}

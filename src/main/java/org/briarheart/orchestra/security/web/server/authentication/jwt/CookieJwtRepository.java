package org.briarheart.orchestra.security.web.server.authentication.jwt;

import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessToken;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.ServerAccessTokenRepository;
import org.briarheart.orchestra.util.Strings;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Implementation of {@link ServerAccessTokenRepository} that stores JWTs in cookies.
 * <p>
 * This repository splits access token value into three parts - header, payload and signature - and stores those parts
 * in cookies with names {@link #accessTokenHeaderCookieName}, {@link #accessTokenPayloadCookieName} and
 * {@link #accessTokenSignatureCookieName} correspondingly. Each cookie except payload cookie has {@code httpOnly} flag
 * being set to {@code true} and age of each cookie is limited by expiration timeout of an access token.
 * <p>
 * Access token to be stored may be unsigned (method {@link Jwt#getSignature()} returns {@code null} or empty string).
 * In this case signature cookie will not be created.
 *
 * @author Roman Chigvintsev
 * @see ServerAccessTokenRepository
 */
public class CookieJwtRepository implements ServerAccessTokenRepository {
    private static final String DEFAULT_ACCESS_TOKEN_HEADER_COOKIE_NAME = "ATH";
    private static final String DEFAULT_ACCESS_TOKEN_PAYLOAD_COOKIE_NAME = "ATP";
    private static final String DEFAULT_ACCESS_TOKEN_SIGNATURE_COOKIE_NAME = "ATS";

    private String accessTokenHeaderCookieName = DEFAULT_ACCESS_TOKEN_HEADER_COOKIE_NAME;
    private String accessTokenPayloadCookieName = DEFAULT_ACCESS_TOKEN_PAYLOAD_COOKIE_NAME;
    private String accessTokenSignatureCookieName = DEFAULT_ACCESS_TOKEN_SIGNATURE_COOKIE_NAME;

    @Override
    public Mono<Jwt> loadAccessToken(ServerWebExchange exchange) {
        Assert.notNull(exchange, "Server web exchange must not be null");
        return Mono.fromCallable(() -> {
            MultiValueMap<String, HttpCookie> cookies = exchange.getRequest().getCookies();

            String header = null;
            HttpCookie headerCookie = cookies.getFirst(accessTokenHeaderCookieName);
            if (headerCookie != null) {
                header = headerCookie.getValue();
            }

            String payload = null;
            HttpCookie payloadCookie = cookies.getFirst(accessTokenPayloadCookieName);
            if (payloadCookie != null) {
                payload = payloadCookie.getValue();
            }

            String signature = null;
            HttpCookie signatureCookie = cookies.getFirst(accessTokenSignatureCookieName);
            if (signatureCookie != null) {
                signature = signatureCookie.getValue();
            }

            if (Strings.hasText(header, payload)) {
                return new Jwt.Builder(header, payload, signature).build();
            }

            return null;
        });
    }

    @Override
    public Mono<Void> saveAccessToken(AccessToken accessToken, ServerWebExchange exchange) {
        Assert.notNull(accessToken, "Access token must not be null");
        Assert.isInstanceOf(Jwt.class, accessToken, "Access token must be instance of " + Jwt.class.getName());

        Jwt jwt = (Jwt) accessToken;

        return Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            Duration maxAge = Duration.between(accessToken.getIssuedAt(), accessToken.getExpiration());

            String header = jwt.getHeader();
            ResponseCookie headerCookie = ResponseCookie.from(accessTokenHeaderCookieName, header)
                    .path("/")
                    .httpOnly(true)
                    .maxAge(maxAge)
                    .build();
            response.addCookie(headerCookie);

            String payload = jwt.getPayload();
            ResponseCookie payloadCookie = ResponseCookie.from(accessTokenPayloadCookieName, payload)
                    .path("/")
                    .maxAge(maxAge)
                    .build();
            response.addCookie(payloadCookie);

            String signature = jwt.getSignature();
            if (StringUtils.hasText(signature)) {
                ResponseCookie signatureCookie = ResponseCookie.from(accessTokenSignatureCookieName, signature)
                        .path("/")
                        .httpOnly(true)
                        .maxAge(maxAge)
                        .build();
                response.addCookie(signatureCookie);
            }
        });
    }

    @Override
    public Mono<Jwt> removeAccessToken(ServerWebExchange exchange) {
        return loadAccessToken(exchange).map(token -> {
            ServerHttpResponse response = exchange.getResponse();
            ResponseCookie headerCookie = ResponseCookie.from(accessTokenHeaderCookieName, "")
                    .path("/")
                    .httpOnly(true)
                    .maxAge(0)
                    .build();
            response.addCookie(headerCookie);
            ResponseCookie payloadCookie = ResponseCookie.from(accessTokenPayloadCookieName, "")
                    .path("/")
                    .maxAge(0)
                    .build();
            response.addCookie(payloadCookie);
            ResponseCookie signatureCookie = ResponseCookie.from(accessTokenSignatureCookieName, "")
                    .path("/")
                    .httpOnly(true)
                    .maxAge(0)
                    .build();
            response.addCookie(signatureCookie);
            return token;
        });
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
}

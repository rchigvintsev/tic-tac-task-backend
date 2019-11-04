package org.briarheart.orchestra.security.web.server.authentication;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.util.Strings;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Converts {@link ServerWebExchange} to instance of {@link AccessTokenAuthentication}. This converter expects to find
 * access token value either in authorization header or in cookies.
 * <p>
 * In case of authorization header its value should start with "Bearer " followed by access token value.
 * <p>
 * In case of cookies access token value should be split into three parts - header, payload and signature - stored in
 * cookies with names {@link #accessTokenHeaderCookieName}, {@link #accessTokenPayloadCookieName} and
 * {@link #accessTokenSignatureCookieName} correspondingly.
 *
 * @author Roman Chigvintsev
 *
 * @see AccessTokenAuthentication
 */
@RequiredArgsConstructor
public class AccessTokenServerAuthenticationConverter implements ServerAuthenticationConverter {
    public static final String DEFAULT_ACCESS_TOKEN_HEADER_COOKIE_NAME = "ATH";
    public static final String DEFAULT_ACCESS_TOKEN_PAYLOAD_COOKIE_NAME = "ATP";
    public static final String DEFAULT_ACCESS_TOKEN_SIGNATURE_COOKIE_NAME = "ATS";

    private final AccessTokenService accessTokenService;

    private String accessTokenHeaderCookieName = DEFAULT_ACCESS_TOKEN_HEADER_COOKIE_NAME;
    private String accessTokenPayloadCookieName = DEFAULT_ACCESS_TOKEN_PAYLOAD_COOKIE_NAME;
    private String accessTokenSignatureCookieName = DEFAULT_ACCESS_TOKEN_SIGNATURE_COOKIE_NAME;

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        Authentication token = getTokenFromHeaders(request);
        if (token != null) {
            return Mono.just(token);
        }
        return Mono.justOrEmpty(getTokenFromCookies(request));
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

    private AccessTokenAuthentication getTokenFromHeaders(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String tokenValue = authorization.substring("Bearer ".length());
            return tokenValue.isEmpty() ? null : new AccessTokenAuthentication(tokenValue);
        }
        return null;
    }

    private AccessTokenAuthentication getTokenFromCookies(ServerHttpRequest request) {
        MultiValueMap<String, HttpCookie> cookies = request.getCookies();

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
            String tokenValue = accessTokenService.composeAccessTokenValue(header, payload, signature);
            return new AccessTokenAuthentication(tokenValue);
        }
        return null;
    }
}

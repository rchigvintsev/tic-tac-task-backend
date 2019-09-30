package org.briarheart.orchestra.security.web.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import org.briarheart.orchestra.security.oauth2.core.endpoint.OAuth2AuthorizationRequestData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Base64;

/**
 * An implementation of an {@link ServerAuthorizationRequestRepository} that stores {@link OAuth2AuthorizationRequest}
 * in cookies. To store authorization request this class creates cookie with default name
 * "oauth2-authorization-request". Authorization request is stored as a URL-encoded JSON string.
 * <p>
 * This repository requires the client to specify redirect URI in query string. Client redirect URI is used to
 * determine location where request will be redirected after successful authentication.
 *
 * @author Roman Chigvintsev
 *
 * @see AuthorizationRequestRepository
 * @see OAuth2AuthorizationRequest
 * @see OAuth2AuthorizationRequestData
 */
public class CookieOAuth2ServerAuthorizationRequestRepository
        implements ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    private static final Logger log = LoggerFactory.getLogger(CookieOAuth2ServerAuthorizationRequestRepository.class);

    private static final String DEFAULT_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2-authorization-request";
    private static final int DEFAULT_COOKIE_MAX_AGE = 180;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String clientRedirectUriParameterName;

    private String authorizationRequestCookieName = DEFAULT_AUTHORIZATION_REQUEST_COOKIE_NAME;

    @Setter
    private int cookieMaxAge = DEFAULT_COOKIE_MAX_AGE;

    /**
     * Creates new instance of this class with the given client redirect URI parameter name.
     *
     * @param clientRedirectUriParameterName name of query string parameter containing client redirect URI
     *                                       (must not be {@code null} or empty)
     */
    public CookieOAuth2ServerAuthorizationRequestRepository(String clientRedirectUriParameterName) {
        Assert.hasText(clientRedirectUriParameterName, "Client redirect URI parameter name must not be null or empty");
        this.clientRedirectUriParameterName = clientRedirectUriParameterName;
    }

    @Override
    public Mono<OAuth2AuthorizationRequest> loadAuthorizationRequest(ServerWebExchange exchange) {
        return Mono.fromCallable(() -> loadAuthorizationRequestFromCookies(exchange));
    }

    @Override
    public Mono<Void> saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                               ServerWebExchange exchange) {
        Assert.notNull(authorizationRequest, "Authorization request cannot be null");
        String clientRedirectUri = getClientRedirectUri(exchange);
        Assert.hasText(clientRedirectUri, "Client redirect URI must be specified");
        return Mono.fromRunnable(() ->
                saveAuthorizationRequestToCookie(authorizationRequest, clientRedirectUri, exchange));
    }

    @Override
    public Mono<OAuth2AuthorizationRequest> removeAuthorizationRequest(ServerWebExchange exchange) {
        return Mono.fromCallable(() -> removeAuthorizationRequestFromCookies(exchange));
    }

    public void setAuthorizationRequestCookieName(String authorizationRequestCookieName) {
        Assert.hasText(authorizationRequestCookieName, "Authorization request cookie name cannot be null or empty");
        this.authorizationRequestCookieName = authorizationRequestCookieName;
    }

    private String getClientRedirectUri(ServerWebExchange exchange) {
        return exchange.getRequest().getQueryParams().getFirst(clientRedirectUriParameterName);
    }

    private void saveAuthorizationRequestToCookie(OAuth2AuthorizationRequest authorizationRequest,
                                                  String clientRedirectUri,
                                                  ServerWebExchange exchange) {
        String serializedRequest = serializeAuthorizationRequest(authorizationRequest, clientRedirectUri);
        ResponseCookie requestCookie = ResponseCookie.from(authorizationRequestCookieName, serializedRequest)
                .path("/")
                .httpOnly(true)
                .maxAge(cookieMaxAge)
                .build();
        exchange.getResponse().addCookie(requestCookie);
    }

    private OAuth2AuthorizationRequest loadAuthorizationRequestFromCookies(ServerWebExchange exchange) {
        MultiValueMap<String, HttpCookie> cookies = exchange.getRequest().getCookies();
        HttpCookie authorizationRequestCookie = cookies.getFirst(authorizationRequestCookieName);
        if (authorizationRequestCookie != null) {
            return deserializeAuthorizationRequest(authorizationRequestCookie.getValue());
        }
        return null;
    }

    private OAuth2AuthorizationRequest removeAuthorizationRequestFromCookies(ServerWebExchange exchange) {
        MultiValueMap<String, HttpCookie> cookies = exchange.getRequest().getCookies();
        HttpCookie authorizationRequestCookie = cookies.getFirst(authorizationRequestCookieName);
        if (authorizationRequestCookie != null) {
            OAuth2AuthorizationRequest request = deserializeAuthorizationRequest(authorizationRequestCookie.getValue());
            ResponseCookie responseCookie = ResponseCookie.from(authorizationRequestCookieName, "")
                    .path("/")
                    .httpOnly(true)
                    .maxAge(0)
                    .build();
            exchange.getResponse().addCookie(responseCookie);
            return request;
        }
        return null;
    }

    private String serializeAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                                 String clientRedirectUri) {
        OAuth2AuthorizationRequestData requestData = new OAuth2AuthorizationRequestData(authorizationRequest);
        requestData.getAdditionalParameters().put(clientRedirectUriParameterName, clientRedirectUri);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(requestData);
            return Base64.getUrlEncoder().encodeToString(bytes);
        } catch (JsonProcessingException e) {
            OAuth2Error oAuth2Error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
                    "Failed to serialize OAuth2 authorization request to JSON", null);
            throw new OAuth2AuthenticationException(oAuth2Error, e);
        }
    }

    private OAuth2AuthorizationRequest deserializeAuthorizationRequest(String serializedAuthorizationRequest) {
        try {
            byte[] decodedRequest = Base64.getUrlDecoder().decode(serializedAuthorizationRequest);
            OAuth2AuthorizationRequestData requestData = objectMapper.readValue(decodedRequest,
                    OAuth2AuthorizationRequestData.class);
            return requestData.toAuthorizationRequest();
        } catch (Exception e) {
            log.error("Failed to deserialize OAuth2 authorization request from JSON", e);
        }
        return null;
    }
}

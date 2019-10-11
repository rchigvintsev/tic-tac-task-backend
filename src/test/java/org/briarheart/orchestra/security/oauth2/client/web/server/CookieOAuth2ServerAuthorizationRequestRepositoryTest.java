package org.briarheart.orchestra.security.oauth2.client.web.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class CookieOAuth2ServerAuthorizationRequestRepositoryTest {
    private CookieOAuth2ServerAuthorizationRequestRepository repository;

    @BeforeEach
    void setUp() {
        repository = new CookieOAuth2ServerAuthorizationRequestRepository("client-redirect-uri");
        repository.setAuthorizationRequestCookieName("oauth2-authorization-request");
    }

    @Test
    void shouldSaveAuthorizationRequestToCookies() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("http://authorize.me")
                .build();
        ResponseCookie responseCookie = saveAuthorizationRequestToCookies(authorizationRequest);
        assertNotNull(responseCookie);
        assertFalse(responseCookie.getValue().isEmpty());
    }

    @Test
    void shouldThrowExceptionOnAuthorizationRequestSaveWhenClientRedirectUriIsMissing() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("http://authorize.me")
                .build();

        ServerHttpRequest requestMock = mock(ServerHttpRequest.class);
        when(requestMock.getQueryParams()).thenReturn(new LinkedMultiValueMap<>());

        ServerWebExchange webExchangeMock = mock(ServerWebExchange.class);
        when(webExchangeMock.getRequest()).thenReturn(requestMock);

        assertThrows(ClientRedirectUriMissingException.class, () ->
                repository.saveAuthorizationRequest(authorizationRequest, webExchangeMock).block());
    }

    @Test
    void shouldThrowExceptionOnAuthorizationRequestSaveWhenSerializationErrorOccurs() {
        Object badAttributeValue = mock(Object.class);
        when(badAttributeValue.toString()).thenThrow(RuntimeException.class);

        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("http://authorize.me")
                .attributes(Map.of("bad-attribute", badAttributeValue))
                .build();

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.put(repository.getClientRedirectUriParameterName(), List.of("http://callback.me"));

        ServerHttpRequest requestMock = mock(ServerHttpRequest.class);
        when(requestMock.getQueryParams()).thenReturn(queryParams);

        ServerWebExchange webExchangeMock = mock(ServerWebExchange.class);
        when(webExchangeMock.getRequest()).thenReturn(requestMock);

        assertThrows(OAuth2AuthenticationException.class, () ->
                repository.saveAuthorizationRequest(authorizationRequest, webExchangeMock).block());
    }

    @Test
    void shouldLoadAuthorizationRequestFromCookies() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("http://authorize.me")
                .build();

        ResponseCookie responseCookie = saveAuthorizationRequestToCookies(authorizationRequest);

        MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
        HttpCookie requestCookie = new HttpCookie(repository.getAuthorizationRequestCookieName(),
                responseCookie.getValue());
        cookies.put(repository.getAuthorizationRequestCookieName(), List.of(requestCookie));

        ServerHttpRequest requestMock = mock(ServerHttpRequest.class);
        when(requestMock.getCookies()).thenReturn(cookies);

        ServerHttpResponse responseMock = mock(ServerHttpResponse.class);

        ServerWebExchange webExchangeMock = mock(ServerWebExchange.class);
        when(webExchangeMock.getRequest()).thenReturn(requestMock);
        when(webExchangeMock.getResponse()).thenReturn(responseMock);

        OAuth2AuthorizationRequest loadedRequest = repository.loadAuthorizationRequest(webExchangeMock).block();
        assertNotNull(loadedRequest);
        assertEquals(authorizationRequest.getClientId(), loadedRequest.getClientId());
        assertEquals(authorizationRequest.getAuthorizationUri(), loadedRequest.getAuthorizationUri());
    }

    @Test
    void shouldReturnNullOnAuthorizationRequestLoadWhenDeserializationErrorOccurs() {
        String requestCookieValue = Base64.getUrlEncoder().encodeToString("{".getBytes(StandardCharsets.UTF_8));
        HttpCookie requestCookie = new HttpCookie(repository.getAuthorizationRequestCookieName(), requestCookieValue);

        MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
        cookies.put(repository.getAuthorizationRequestCookieName(), List.of(requestCookie));

        ServerHttpRequest requestMock = mock(ServerHttpRequest.class);
        when(requestMock.getCookies()).thenReturn(cookies);

        ServerWebExchange webExchangeMock = mock(ServerWebExchange.class);
        when(webExchangeMock.getRequest()).thenReturn(requestMock);

        OAuth2AuthorizationRequest loadedRequest = repository.loadAuthorizationRequest(webExchangeMock).block();
        assertNull(loadedRequest);
    }

    @Test
    void shouldReturnNullOnAuthorizationRequestLoadWhenCorrespondingCookieIsNotFound() {
        ServerHttpRequest requestMock = mock(ServerHttpRequest.class);
        when(requestMock.getCookies()).thenReturn(new LinkedMultiValueMap<>());

        ServerWebExchange webExchangeMock = mock(ServerWebExchange.class);
        when(webExchangeMock.getRequest()).thenReturn(requestMock);

        OAuth2AuthorizationRequest loadedRequest = repository.loadAuthorizationRequest(webExchangeMock).block();
        assertNull(loadedRequest);
    }

    @Test
    void shouldRemoveAuthorizationRequestFromCookies() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("http://authorize.me")
                .build();

        ResponseCookie responseCookie = saveAuthorizationRequestToCookies(authorizationRequest);

        MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
        HttpCookie requestCookie = new HttpCookie(repository.getAuthorizationRequestCookieName(),
                responseCookie.getValue());
        cookies.put(repository.getAuthorizationRequestCookieName(), List.of(requestCookie));

        ServerHttpRequest requestMock = mock(ServerHttpRequest.class);
        when(requestMock.getCookies()).thenReturn(cookies);

        ServerHttpResponse responseMock = mock(ServerHttpResponse.class);

        ServerWebExchange webExchangeMock = mock(ServerWebExchange.class);
        when(webExchangeMock.getRequest()).thenReturn(requestMock);
        when(webExchangeMock.getResponse()).thenReturn(responseMock);

        repository.removeAuthorizationRequest(webExchangeMock).block();

        ResponseCookie removeRequestCookie = getAuthorizationRequestResponseCookie(responseMock);
        assertNotNull(removeRequestCookie);
        assertTrue(removeRequestCookie.getValue().isEmpty());
    }

    @Test
    void shouldReturnRemovedAuthorizationRequest() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("http://authorize.me")
                .build();

        ResponseCookie responseCookie = saveAuthorizationRequestToCookies(authorizationRequest);

        MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
        HttpCookie requestCookie = new HttpCookie(repository.getAuthorizationRequestCookieName(),
                responseCookie.getValue());
        cookies.put(repository.getAuthorizationRequestCookieName(), List.of(requestCookie));

        ServerHttpRequest requestMock = mock(ServerHttpRequest.class);
        when(requestMock.getCookies()).thenReturn(cookies);

        ServerHttpResponse responseMock = mock(ServerHttpResponse.class);

        ServerWebExchange webExchangeMock = mock(ServerWebExchange.class);
        when(webExchangeMock.getRequest()).thenReturn(requestMock);
        when(webExchangeMock.getResponse()).thenReturn(responseMock);

        OAuth2AuthorizationRequest removedRequest = repository.removeAuthorizationRequest(webExchangeMock).block();
        assertNotNull(removedRequest);
        assertEquals(authorizationRequest.getClientId(), removedRequest.getClientId());
        assertEquals(authorizationRequest.getAuthorizationUri(), removedRequest.getAuthorizationUri());
    }

    @Test
    void shouldReturnNullOnAuthorizationRequestRemoveWhenCorrespondingCookieIsNotFound() {
        ServerHttpRequest requestMock = mock(ServerHttpRequest.class);
        when(requestMock.getCookies()).thenReturn(new LinkedMultiValueMap<>());

        ServerWebExchange webExchangeMock = mock(ServerWebExchange.class);
        when(webExchangeMock.getRequest()).thenReturn(requestMock);

        OAuth2AuthorizationRequest loadedRequest = repository.removeAuthorizationRequest(webExchangeMock).block();
        assertNull(loadedRequest);
    }

    private ResponseCookie saveAuthorizationRequestToCookies(OAuth2AuthorizationRequest authorizationRequest) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.put(repository.getClientRedirectUriParameterName(), List.of("http://callback.me"));

        ServerHttpRequest requestMock = mock(ServerHttpRequest.class);
        when(requestMock.getQueryParams()).thenReturn(queryParams);

        ServerHttpResponse responseMock = mock(ServerHttpResponse.class);

        ServerWebExchange webExchangeMock = mock(ServerWebExchange.class);
        when(webExchangeMock.getRequest()).thenReturn(requestMock);
        when(webExchangeMock.getResponse()).thenReturn(responseMock);

        repository.saveAuthorizationRequest(authorizationRequest, webExchangeMock).block();
        ResponseCookie responseCookie = getAuthorizationRequestResponseCookie(responseMock);
        assertNotNull(responseCookie);
        return responseCookie;
    }

    private ResponseCookie getAuthorizationRequestResponseCookie(ServerHttpResponse responseMock) {
        ArgumentCaptor<ResponseCookie> cookieCaptor = ArgumentCaptor.forClass(ResponseCookie.class);
        verify(responseMock).addCookie(cookieCaptor.capture());
        ResponseCookie responseCookie = cookieCaptor.getValue();
        if (repository.getAuthorizationRequestCookieName().equals(responseCookie.getName())) {
            return responseCookie;
        }
        return null;
    }
}

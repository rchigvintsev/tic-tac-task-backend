package org.briarheart.orchestra.security.oauth2.client.web.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.briarheart.orchestra.security.oauth2.core.endpoint.OAuth2AuthorizationRequestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class CookieOAuth2ServerAuthorizationRequestRepositoryTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private CookieOAuth2ServerAuthorizationRequestRepository repository;

    @BeforeEach
    void setUp() {
        repository = new CookieOAuth2ServerAuthorizationRequestRepository("client-redirect-uri");
    }

    @Test
    void shouldSaveAuthorizationRequestToCookies() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("http://authorize.me")
                .build();

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.put(repository.getClientRedirectUriParameterName(), List.of("http://callback.me"));

        ServerHttpRequest requestMock = mock(ServerHttpRequest.class);
        when(requestMock.getQueryParams()).thenReturn(queryParams);

        ServerHttpResponse responseMock = mock(ServerHttpResponse.class);

        ServerWebExchange webExchangeMock = mock(ServerWebExchange.class);
        when(webExchangeMock.getRequest()).thenReturn(requestMock);
        when(webExchangeMock.getResponse()).thenReturn(responseMock);

        repository.saveAuthorizationRequest(authorizationRequest, webExchangeMock).block();

        ArgumentCaptor<ResponseCookie> cookieCaptor = ArgumentCaptor.forClass(ResponseCookie.class);
        verify(responseMock).addCookie(cookieCaptor.capture());
        ResponseCookie requestCookie = cookieCaptor.getValue();
        assertEquals(repository.getAuthorizationRequestCookieName(), requestCookie.getName());
        assertFalse(requestCookie.getValue().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenClientRedirectUriIsNotSpecified() {
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
    void shouldLoadAuthorizationRequestFromCookies() throws JsonProcessingException {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("http://authorize.me")
                .build();
        String requestCookieValue = serializeAuthorizationRequest(authorizationRequest);
        HttpCookie requestCookie = new HttpCookie(repository.getAuthorizationRequestCookieName(), requestCookieValue);

        MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
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
    void shouldRemoveAuthorizationRequestFromCookies() throws JsonProcessingException {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("http://authorize.me")
                .build();
        String requestCookieValue = serializeAuthorizationRequest(authorizationRequest);
        HttpCookie requestCookie = new HttpCookie(repository.getAuthorizationRequestCookieName(), requestCookieValue);

        MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
        cookies.put(repository.getAuthorizationRequestCookieName(), List.of(requestCookie));

        ServerHttpRequest requestMock = mock(ServerHttpRequest.class);
        when(requestMock.getCookies()).thenReturn(cookies);

        ServerHttpResponse responseMock = mock(ServerHttpResponse.class);

        ServerWebExchange webExchangeMock = mock(ServerWebExchange.class);
        when(webExchangeMock.getRequest()).thenReturn(requestMock);
        when(webExchangeMock.getResponse()).thenReturn(responseMock);

        repository.removeAuthorizationRequest(webExchangeMock).block();

        ArgumentCaptor<ResponseCookie> cookieCaptor = ArgumentCaptor.forClass(ResponseCookie.class);
        verify(responseMock).addCookie(cookieCaptor.capture());
        ResponseCookie removeRequestCookie = cookieCaptor.getValue();
        assertEquals(repository.getAuthorizationRequestCookieName(), removeRequestCookie.getName());
        assertTrue(removeRequestCookie.getValue().isEmpty());
    }

    @Test
    void shouldReturnRemovedAuthorizationRequest() throws JsonProcessingException {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client")
                .authorizationUri("http://authorize.me")
                .build();
        String requestCookieValue = serializeAuthorizationRequest(authorizationRequest);
        HttpCookie requestCookie = new HttpCookie(repository.getAuthorizationRequestCookieName(), requestCookieValue);

        MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
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

    private String serializeAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest)
            throws JsonProcessingException {
        OAuth2AuthorizationRequestData requestData = new OAuth2AuthorizationRequestData(authorizationRequest);
        byte[] bytes = objectMapper.writeValueAsBytes(requestData);
        return Base64.getUrlEncoder().encodeToString(bytes);
    }
}

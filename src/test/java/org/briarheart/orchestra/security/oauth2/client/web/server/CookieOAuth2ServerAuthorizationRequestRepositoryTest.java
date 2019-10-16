package org.briarheart.orchestra.security.oauth2.client.web.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Chigvintsev
 */
class CookieOAuth2ServerAuthorizationRequestRepositoryTest {
    private static final String VALID_AUTHORIZATION_REQUEST_AUTHORIZATION_CODE_COOKIE_VALUE = "eyJhdXRob3JpemF0aW9uVX" +
            "JpIjoiaHR0cDovL2F1dGhvcml6ZS5tZSIsImdyYW50VHlwZSI6ImF1dGhvcml6YXRpb25fY29kZSIsImNsaWVudElkIjoidGVzdC1jbG" +
            "llbnQiLCJyZWRpcmVjdFVyaSI6bnVsbCwic2NvcGVzIjpbXSwic3RhdGUiOm51bGwsImFkZGl0aW9uYWxQYXJhbWV0ZXJzIjp7ImNsaW" +
            "VudC1yZWRpcmVjdC11cmkiOiJodHRwOi8vY2FsbGJhY2subWUifSwiYXV0aG9yaXphdGlvblJlcXVlc3RVcmkiOiJodHRwOi8vYXV0aG" +
            "9yaXplLm1lP3Jlc3BvbnNlX3R5cGU9Y29kZSZjbGllbnRfaWQ9dGVzdC1jbGllbnQiLCJhdHRyaWJ1dGVzIjp7fX0=";
    private static final String VALID_AUTHORIZATION_REQUEST_IMPLICIT_COOKIE_VALUE = "eyJhdXRob3JpemF0aW9uVXJpIjoiaHR0" +
            "cDovL2F1dGhvcml6ZS5tZSIsImdyYW50VHlwZSI6ImltcGxpY2l0IiwiY2xpZW50SWQiOiJ0ZXN0LWNsaWVudCIsInJlZGlyZWN0VXJp" +
            "IjoiaHR0cDovL2V4YW1wbGUuY29tIiwic2NvcGVzIjpbXSwic3RhdGUiOm51bGwsImFkZGl0aW9uYWxQYXJhbWV0ZXJzIjp7ImNsaWVu" +
            "dC1yZWRpcmVjdC11cmkiOiJodHRwOi8vY2FsbGJhY2subWUifSwiYXV0aG9yaXphdGlvblJlcXVlc3RVcmkiOiJodHRwOi8vYXV0aG9y" +
            "aXplLm1lP3Jlc3BvbnNlX3R5cGU9dG9rZW4mY2xpZW50X2lkPXRlc3QtY2xpZW50IiwiYXR0cmlidXRlcyI6e319";
    private static final String VALID_AUTHORIZATION_REQUEST_PASSWORD_COOKIE_VALUE = "eyJhdXRob3JpemF0aW9uVXJpIjoiaHR0" +
            "cDovL2F1dGhvcml6ZS5tZSIsImdyYW50VHlwZSI6InBhc3N3b3JkIiwiY2xpZW50SWQiOiJ0ZXN0LWNsaWVudCIsInJlZGlyZWN0VXJp" +
            "IjpudWxsLCJzY29wZXMiOltdLCJzdGF0ZSI6bnVsbCwiYWRkaXRpb25hbFBhcmFtZXRlcnMiOnsiY2xpZW50LXJlZGlyZWN0LXVyaSI6" +
            "Imh0dHA6Ly9jYWxsYmFjay5tZSJ9LCJhdXRob3JpemF0aW9uUmVxdWVzdFVyaSI6Imh0dHA6Ly9hdXRob3JpemUubWU_cmVzcG9uc2Vf" +
            "dHlwZT10b2tlbiZjbGllbnRfaWQ9dGVzdC1jbGllbnQiLCJhdHRyaWJ1dGVzIjp7fX0";
    private static final String INVALID_AUTHORIZATION_REQUEST_COOKIE_VALUE = "ew==";

    private static final String CLIENT_ID = "test-client";
    private static final String AUTHORIZATION_URI = "http://authorize.me";
    private static final String CLIENT_REDIRECT_URI = "http://callback.me";

    private CookieOAuth2ServerAuthorizationRequestRepository repository;

    @BeforeEach
    void setUp() {
        repository = new CookieOAuth2ServerAuthorizationRequestRepository("client-redirect-uri");
        repository.setAuthorizationRequestCookieName("oauth2-authorization-request");
    }

    @Test
    void shouldSaveAuthorizationRequestToCookies() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId(CLIENT_ID)
                .authorizationUri(AUTHORIZATION_URI)
                .build();

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .queryParam(repository.getClientRedirectUriParameterName(), CLIENT_REDIRECT_URI)
                .build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        repository.saveAuthorizationRequest(authorizationRequest, webExchangeMock).block();
        ResponseCookie saveRequestCookie = webExchangeMock.getResponse().getCookies()
                .getFirst(repository.getAuthorizationRequestCookieName());
        assertNotNull(saveRequestCookie);
        assertFalse(saveRequestCookie.getValue().isEmpty());
    }

    @Test
    void shouldThrowExceptionOnAuthorizationRequestSaveWhenClientRedirectUriIsMissing() {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId(CLIENT_ID)
                .authorizationUri(AUTHORIZATION_URI)
                .build();

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        assertThrows(ClientRedirectUriMissingException.class, () ->
                repository.saveAuthorizationRequest(authorizationRequest, webExchangeMock).block());
    }

    @Test
    void shouldThrowExceptionOnAuthorizationRequestSaveWhenSerializationErrorOccurs() {
        Object badAttributeValue = mock(Object.class);
        when(badAttributeValue.toString()).thenThrow(RuntimeException.class);

        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId(CLIENT_ID)
                .authorizationUri(AUTHORIZATION_URI)
                .attributes(Map.of("bad-attribute", badAttributeValue))
                .build();

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/")
                .queryParam(repository.getClientRedirectUriParameterName(), CLIENT_REDIRECT_URI)
                .build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        assertThrows(OAuth2AuthenticationException.class, () ->
                repository.saveAuthorizationRequest(authorizationRequest, webExchangeMock).block());
    }

    @Test
    void shouldLoadAuthorizationRequestWithAuthorizationCodeGrandTypeFromCookies() {
        HttpCookie requestCookie = new HttpCookie(repository.getAuthorizationRequestCookieName(),
                VALID_AUTHORIZATION_REQUEST_AUTHORIZATION_CODE_COOKIE_VALUE);
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").cookie(requestCookie).build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        OAuth2AuthorizationRequest loadedRequest = repository.loadAuthorizationRequest(webExchangeMock).block();
        assertNotNull(loadedRequest);
        assertEquals(AuthorizationGrantType.AUTHORIZATION_CODE, loadedRequest.getGrantType());
        assertEquals(CLIENT_ID, loadedRequest.getClientId());
        assertEquals(AUTHORIZATION_URI, loadedRequest.getAuthorizationUri());
    }

    @Test
    void shouldLoadAuthorizationRequestWithImplicitGrandTypeFromCookies() {
        HttpCookie requestCookie = new HttpCookie(repository.getAuthorizationRequestCookieName(),
                VALID_AUTHORIZATION_REQUEST_IMPLICIT_COOKIE_VALUE);
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").cookie(requestCookie).build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        OAuth2AuthorizationRequest loadedRequest = repository.loadAuthorizationRequest(webExchangeMock).block();
        assertNotNull(loadedRequest);
        assertEquals(AuthorizationGrantType.IMPLICIT, loadedRequest.getGrantType());
        assertEquals(CLIENT_ID, loadedRequest.getClientId());
        assertEquals(AUTHORIZATION_URI, loadedRequest.getAuthorizationUri());
    }

    @Test
    void shouldReturnNullOnAuthorizationRequestLoadWhenGrantTypeIsNotSupported() {
        HttpCookie requestCookie = new HttpCookie(repository.getAuthorizationRequestCookieName(),
                VALID_AUTHORIZATION_REQUEST_PASSWORD_COOKIE_VALUE);
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").cookie(requestCookie).build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        OAuth2AuthorizationRequest loadedRequest = repository.loadAuthorizationRequest(webExchangeMock).block();
        assertNull(loadedRequest);
    }

    @Test
    void shouldReturnNullOnAuthorizationRequestLoadWhenDeserializationErrorOccurs() {
        HttpCookie requestCookie = new HttpCookie(repository.getAuthorizationRequestCookieName(),
                INVALID_AUTHORIZATION_REQUEST_COOKIE_VALUE);
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").cookie(requestCookie).build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        OAuth2AuthorizationRequest loadedRequest = repository.loadAuthorizationRequest(webExchangeMock).block();
        assertNull(loadedRequest);
    }

    @Test
    void shouldReturnNullOnAuthorizationRequestLoadWhenCorrespondingCookieIsNotFound() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        OAuth2AuthorizationRequest loadedRequest = repository.loadAuthorizationRequest(webExchangeMock).block();
        assertNull(loadedRequest);
    }

    @Test
    void shouldRemoveAuthorizationRequestFromCookies() {
        HttpCookie requestCookie = new HttpCookie(repository.getAuthorizationRequestCookieName(),
                VALID_AUTHORIZATION_REQUEST_AUTHORIZATION_CODE_COOKIE_VALUE);
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").cookie(requestCookie).build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        repository.removeAuthorizationRequest(webExchangeMock).block();
        ResponseCookie removeRequestCookie = webExchangeMock.getResponse().getCookies()
                .getFirst(repository.getAuthorizationRequestCookieName());
        assertNotNull(removeRequestCookie);
        assertTrue(removeRequestCookie.getValue().isEmpty());
    }

    @Test
    void shouldReturnRemovedAuthorizationRequest() {
        HttpCookie requestCookie = new HttpCookie(repository.getAuthorizationRequestCookieName(),
                VALID_AUTHORIZATION_REQUEST_AUTHORIZATION_CODE_COOKIE_VALUE);
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").cookie(requestCookie).build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        OAuth2AuthorizationRequest removedRequest = repository.removeAuthorizationRequest(webExchangeMock).block();
        assertNotNull(removedRequest);
        assertEquals(CLIENT_ID, removedRequest.getClientId());
        assertEquals(AUTHORIZATION_URI, removedRequest.getAuthorizationUri());
    }

    @Test
    void shouldReturnNullOnAuthorizationRequestRemoveWhenCorrespondingCookieIsNotFound() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        OAuth2AuthorizationRequest loadedRequest = repository.removeAuthorizationRequest(webExchangeMock).block();
        assertNull(loadedRequest);
    }
}

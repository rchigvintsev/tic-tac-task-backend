package org.briarheart.orchestra.security.oauth2.client.userinfo;

import org.briarheart.orchestra.security.oauth2.client.oidc.userinfo.MockOidcUserRequest;
import org.briarheart.orchestra.security.oauth2.client.registration.TestClientRegistrations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails.UserInfoEndpoint;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Chigvintsev
 */
class VkReactiveOAuth2UserServiceTest {
    private static final Map<String, Object> ATTRIBUTES = Map.ofEntries(
            entry("id", "1"),
            entry("email", "test@example.com"),
            entry("first_name", "John"),
            entry("last_name", "Doe"),
            entry("photo_100", "http://example.com/picture")
    );
    private static final Map<String, Object> CLAIMS = Map.of("id", "1");

    private VkReactiveOAuth2UserService service;
    private WebClient.ResponseSpec responseSpecMock;

    @BeforeEach
    void setUp() {
        responseSpecMock = mock(WebClient.ResponseSpec.class, Answers.RETURNS_SELF);
        when(responseSpecMock.bodyToMono(Mockito.<ParameterizedTypeReference<? super Map<String, Object>>>any()))
                .thenReturn(Mono.just(Map.of("response", List.of(ATTRIBUTES))));

        WebClient.RequestBodyUriSpec requestBodyUriSpecMock = mock(WebClient.RequestBodyUriSpec.class,
                Answers.RETURNS_SELF);
        when(requestBodyUriSpecMock.retrieve()).thenReturn(responseSpecMock);

        WebClient webClientMock = mock(WebClient.class);
        when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);

        service = new VkReactiveOAuth2UserService();
        service.setWebClient(webClientMock);
    }

    @Test
    void shouldLoadUser() {
        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        OAuth2UserRequest requestMock = new MockOidcUserRequest(clientRegistration, Map.of("id", "1"));
        OAuth2User user = service.loadUser(requestMock).block();
        assertNotNull(user);
        assertEquals("test@example.com", user.getAttribute("email"));
    }

    @Test
    void shouldThrowExceptionOnUserLoadWhenOAuth2UserRequestIsNull() {
        assertThrows(IllegalArgumentException.class, () -> service.loadUser(null));
    }

    @Test
    void shouldThrowExceptionOnUserLoadWhenUserInfoEndpointUriIsUndefined() {
        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration()
                .userInfoUri(null)
                .build();
        OAuth2UserRequest requestMock = new MockOidcUserRequest(clientRegistration, Map.of("id", "1"));
        OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class,
                service.loadUser(requestMock)::block);
        assertEquals("missing_user_info_uri", exception.getError().getErrorCode());
    }

    @Test
    void shouldThrowExceptionOnUserLoadWhenUsernameAttributeNameIsUndefined() {
        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration()
                .userNameAttributeName(null)
                .build();
        OAuth2UserRequest requestMock = new MockOidcUserRequest(clientRegistration, Map.of("id", "1"));
        OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class,
                service.loadUser(requestMock)::block);
        assertEquals("missing_user_name_attribute", exception.getError().getErrorCode());
    }

    @Test
    void shouldThrowExceptionOnUserLoadWhenResponseStatusCodeIsNotOk() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"oops\" "
                + "error_description=\"Something went wrong\" "
                + "scope=\"bad\"");

        ClientResponse.Headers headersMock = mock(ClientResponse.Headers.class);
        when(headersMock.asHttpHeaders()).thenReturn(httpHeaders);

        ClientResponse errorResponseMock = mock(ClientResponse.class);
        when(errorResponseMock.headers()).thenReturn(headersMock);

        when(responseSpecMock.onStatus(any(), any())).thenAnswer(invocation -> {
            Predicate<HttpStatus> predicate = invocation.getArgument(0);
            if (predicate.test(HttpStatus.I_AM_A_TEAPOT)) {
                Function<ClientResponse, Mono<? extends Throwable>> callback = invocation.getArgument(1);
                callback.apply(errorResponseMock).block();
            }
            return null;
        });

        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        OAuth2UserRequest requestMock = new MockOidcUserRequest(clientRegistration, Map.of("id", "1"));
        OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class,
                service.loadUser(requestMock)::block);
        assertEquals("invalid_user_info_response", exception.getError().getErrorCode());
        assertEquals("Something went wrong", exception.getError().getDescription());
    }

    @Test
    void shouldThrowExceptionOnUserLoadWhenResponseStatusCodeIsNotOkAndResponseBodyContainsErrorInfo() {
        ClientResponse.Headers headersMock = mock(ClientResponse.Headers.class);
        when(headersMock.asHttpHeaders()).thenReturn(new HttpHeaders());

        Map<String, String> params = Map.of(
                "error", "oops",
                "error_description", "Something went wrong",
                "scope", "bad"
        );

        ClientResponse errorResponseMock = mock(ClientResponse.class);
        when(errorResponseMock.headers()).thenReturn(headersMock);
        when(errorResponseMock.bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<? super Map<String, String>>>any()))
                .thenReturn(Mono.just(params));

        when(responseSpecMock.onStatus(any(), any())).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<? extends Throwable>> callback = invocation.getArgument(1);
            callback.apply(errorResponseMock).block();
            return null;
        });

        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        OAuth2UserRequest requestMock = new MockOidcUserRequest(clientRegistration, Map.of("id", "1"));
        OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class,
                service.loadUser(requestMock)::block);
        assertEquals("invalid_user_info_response", exception.getError().getErrorCode());
        assertEquals("Something went wrong", exception.getError().getDescription());
    }

    @Test
    void shouldThrowExceptionOnUserLoadWhenResponseAttributeIsMissing() {
        when(responseSpecMock.bodyToMono(Mockito.<ParameterizedTypeReference<? super Map<String, Object>>>any()))
                .thenReturn(Mono.just(Collections.emptyMap()));
        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        OAuth2UserRequest requestMock = new MockOidcUserRequest(clientRegistration, Map.of("id", "1"));
        OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class,
                service.loadUser(requestMock)::block);
        assertEquals("invalid_user_info_response", exception.getError().getErrorCode());
        assertEquals("Response attribute is missing", exception.getError().getDescription());
    }

    @Test
    void shouldThrowExceptionOnUserLoadWhenIOErrorOccurred() {
        when(responseSpecMock.bodyToMono(Mockito.<ParameterizedTypeReference<? super Map<String, Object>>>any()))
                .thenReturn(Mono.error(new IOException("Some I/O error occurred")));
        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        OAuth2UserRequest requestMock = new MockOidcUserRequest(clientRegistration, Map.of("id", "1"));
        AuthenticationServiceException exception = assertThrows(AuthenticationServiceException.class,
                service.loadUser(requestMock)::block);
        UserInfoEndpoint userInfoEndpoint = clientRegistration.getProviderDetails().getUserInfoEndpoint();
        assertEquals("Unable to access the user info endpoint " + userInfoEndpoint.getUri(), exception.getMessage());
    }

    @Test
    void shouldThrowExceptionOnUserLoadWhenRuntimeErrorOccurred() {
        when(responseSpecMock.bodyToMono(Mockito.<ParameterizedTypeReference<? super Map<String, Object>>>any()))
                .thenReturn(Mono.error(new RuntimeException("Some runtime error occurred")));
        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        OAuth2UserRequest requestMock = new MockOidcUserRequest(clientRegistration, Map.of("id", "1"));
        OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class,
                service.loadUser(requestMock)::block);
        assertEquals("invalid_user_info_response", exception.getError().getErrorCode());
        assertEquals("An error occurred while reading user info success response: Some runtime error occurred",
                exception.getError().getDescription());
    }

    @Test
    void shouldThrowExceptionOnUserLoadWhenResponseAttributeDoesNotHaveAnyValue() {
        when(responseSpecMock.bodyToMono(Mockito.<ParameterizedTypeReference<? super Map<String, Object>>>any()))
                .thenReturn(Mono.just(Map.of("response", Collections.emptyList())));
        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        OAuth2UserRequest requestMock = new MockOidcUserRequest(clientRegistration, Map.of("id", "1"));
        OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class,
                service.loadUser(requestMock)::block);
        assertEquals("invalid_user_info_response", exception.getError().getErrorCode());
        assertEquals("Response attribute does not have any value", exception.getError().getDescription());
    }

    @Test
    void shouldThrowExceptionOnUserLoadWhenResponseAttributeHasMoreThenOneValue() {
        when(responseSpecMock.bodyToMono(Mockito.<ParameterizedTypeReference<? super Map<String, Object>>>any()))
                .thenReturn(Mono.just(Map.of("response", List.of(ATTRIBUTES, ATTRIBUTES))));
        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        OAuth2UserRequest requestMock = new MockOidcUserRequest(clientRegistration, Map.of("id", "1"));
        OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class,
                service.loadUser(requestMock)::block);
        assertEquals("invalid_user_info_response", exception.getError().getErrorCode());
        assertEquals("Response attribute has more then one value", exception.getError().getDescription());
    }

    @Test
    void shouldTakeEmailFromOAuth2UserRequestOnUserLoad() {
        Map<String, Object> attrs = new HashMap<>(ATTRIBUTES);
        attrs.remove("email");
        when(responseSpecMock.bodyToMono(Mockito.<ParameterizedTypeReference<? super Map<String, Object>>>any()))
                .thenReturn(Mono.just(Map.of("response", List.of(attrs))));

        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        Map<String, Object> additionalParams = Map.of("email", "john.doe@example.com");
        OAuth2UserRequest requestMock = new MockOidcUserRequest(clientRegistration, CLAIMS, additionalParams);
        OAuth2User user = service.loadUser(requestMock).block();
        assertNotNull(user);
        assertEquals(additionalParams.get("email"), user.getAttribute("email"));
    }
}

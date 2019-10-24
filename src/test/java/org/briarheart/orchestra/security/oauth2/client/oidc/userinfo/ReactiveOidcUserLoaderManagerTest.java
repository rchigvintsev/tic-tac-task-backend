package org.briarheart.orchestra.security.oauth2.client.oidc.userinfo;

import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.security.oauth2.client.registration.TestClientRegistrations;
import org.briarheart.orchestra.security.oauth2.client.userinfo.ReactiveOAuth2UserLoader;
import org.briarheart.orchestra.security.oauth2.client.userinfo.ReactiveOAuth2UserLoaderManager;
import org.briarheart.orchestra.security.oauth2.core.user.OAuth2UserAttributeAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class ReactiveOidcUserLoaderManagerTest {
    private static final Map<String, Object> CLAIMS = Map.ofEntries(
            entry("id", "1"),
            entry("sub", "test"),
            entry("email", "test@example.com"),
            entry("name", "John Doe"),
            entry("picture", "http://example.com/picture")
    );
    private static final ClientRegistration CLIENT_REGISTRATION = TestClientRegistrations.clientRegistration().build();

    private ReactiveOAuth2UserLoader<OidcUserRequest, OidcUser> userLoaderMock;
    private UserRepository userRepositoryMock;
    private ReactiveOAuth2UserLoaderManager<OidcUserRequest, OidcUser> manager;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        userRepositoryMock = mock(UserRepository.class);

        OidcIdToken oidcIdTokenMock = mock(OidcIdToken.class);
        when(oidcIdTokenMock.getClaims()).thenReturn(CLAIMS);

        userLoaderMock = mock(ReactiveOAuth2UserLoader.class);
        when(userLoaderMock.supports(CLIENT_REGISTRATION)).thenReturn(true);

        manager = new ReactiveOAuth2UserLoaderManager<>(List.of(userLoaderMock), userRepositoryMock);
    }

    @SuppressWarnings("UnassignedFluxMonoInstance")
    @Test
    void shouldSaveNewUserInDatabase() {
        OidcUser oidcUserMock = mockOidcUser(CLAIMS);
        when(userLoaderMock.loadUser(any())).thenReturn(Mono.just(oidcUserMock));

        when(userRepositoryMock.findById(anyString())).thenReturn(Mono.empty());
        when(userRepositoryMock.save(any())).then((Answer<Mono<User>>) invoc -> Mono.just(invoc.getArgument(0)));

        MockOidcUserRequest userRequestMock = new MockOidcUserRequest(CLIENT_REGISTRATION, CLAIMS);

        manager.loadUser(userRequestMock).block();
        verify(userRepositoryMock, times(1)).save(any());
    }

    @SuppressWarnings("UnassignedFluxMonoInstance")
    @Test
    void shouldUpdateExistingUserInDatabase() {
        OidcUser oidcUserMock = mockOidcUser(CLAIMS);
        when(userLoaderMock.loadUser(any())).thenReturn(Mono.just(oidcUserMock));

        User user = new User();
        when(userRepositoryMock.findById(anyString())).thenReturn(Mono.just(user));
        when(userRepositoryMock.save(any())).then((Answer<Mono<User>>) invoc -> Mono.just(invoc.getArgument(0)));

        MockOidcUserRequest userRequestMock = new MockOidcUserRequest(CLIENT_REGISTRATION, CLAIMS);

        manager.loadUser(userRequestMock).block();
        verify(userRepositoryMock, times(1)).save(any());
        assertEquals(1, user.getVersion());
        assertEquals(CLAIMS.get("name"), user.getFullName());
        assertEquals(CLAIMS.get("picture"), user.getImageUrl());
    }

    @SuppressWarnings("UnassignedFluxMonoInstance")
    @Test
    void shouldDoNothingWhenExistingUserDataIsActual() {
        OidcUser oidcUserMock = mockOidcUser(CLAIMS);
        when(userLoaderMock.loadUser(any())).thenReturn(Mono.just(oidcUserMock));

        User user = new User();
        user.setFullName(CLAIMS.get("name").toString());
        user.setImageUrl(CLAIMS.get("picture").toString());

        when(userRepositoryMock.findById(anyString())).thenReturn(Mono.just(user));
        when(userRepositoryMock.save(any())).then((Answer<Mono<User>>) invoc -> Mono.just(invoc.getArgument(0)));

        MockOidcUserRequest userRequestMock = new MockOidcUserRequest(CLIENT_REGISTRATION, CLAIMS);

        manager.loadUser(userRequestMock).block();
        verify(userRepositoryMock, times(0)).save(any());
    }

    @Test
    void shouldThrowExceptionWhenLoadedUserAttributesDoNotIncludeEmail() {
        OidcUser oidcUserMock = mockOidcUser(Collections.emptyMap());
        when(userLoaderMock.loadUser(any())).thenReturn(Mono.just(oidcUserMock));
        MockOidcUserRequest userRequestMock = new MockOidcUserRequest(CLIENT_REGISTRATION, CLAIMS);
        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class,
                () -> manager.loadUser(userRequestMock).block());
        assertEquals("OAuth2 user \"" + oidcUserMock.getName() + "\" does not have an email", e.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUserLoaderSupportingGivenClientRegistrationIsNotFound() {
        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration()
                .registrationId("foo-bar")
                .build();
        MockOidcUserRequest userRequestMock = new MockOidcUserRequest(clientRegistration, CLAIMS);
        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class,
                () -> manager.loadUser(userRequestMock).block());
        assertEquals("OAuth2 user loader that supports client registration with id \""
                + clientRegistration.getRegistrationId() + "\" is not found", e.getMessage());
    }

    private OidcUser mockOidcUser(Map<String, Object> claims) {
        OidcUser oidcUserMock = mock(OidcUser.class, withSettings().extraInterfaces(OAuth2UserAttributeAccessor.class));
        when(((OAuth2UserAttributeAccessor) oidcUserMock).getEmail())
                .thenReturn(Objects.toString(claims.get("email"), null));
        when(((OAuth2UserAttributeAccessor) oidcUserMock).getFullName())
                .thenReturn(Objects.toString(claims.get("name"), null));
        when(((OAuth2UserAttributeAccessor) oidcUserMock).getPicture())
                .thenReturn(Objects.toString(claims.get("picture"), null));
        return oidcUserMock;
    }
}

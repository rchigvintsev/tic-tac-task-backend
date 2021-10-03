package org.briarheart.tictactask.security.oauth2.client.userinfo;

import org.briarheart.tictactask.security.oauth2.client.oidc.userinfo.MockOidcUserRequest;
import org.briarheart.tictactask.security.oauth2.client.registration.TestClientRegistrations;
import org.briarheart.tictactask.security.oauth2.core.user.GoogleOAuth2User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class GoogleReactiveOAuth2UserLoaderTest {
    private static final Map<String, Object> CLAIMS = Map.ofEntries(
            entry("id", "1"),
            entry("sub", "test"),
            entry("email", "test@example.com"),
            entry("name", "John Doe"),
            entry("picture", "http://example.com/picture")
    );

    private GoogleReactiveOAuth2UserLoader loader;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        OidcIdToken oidcIdTokenMock = mock(OidcIdToken.class);
        doReturn(CLAIMS).when(oidcIdTokenMock).getClaims();

        OidcUserInfo oidcUserInfoMock = mock(OidcUserInfo.class);

        OidcUser oidcUserMock = mock(OidcUser.class);
        doReturn(Set.of(new SimpleGrantedAuthority("user"))).when(oidcUserMock).getAuthorities();
        doReturn(oidcIdTokenMock).when(oidcUserMock).getIdToken();
        doReturn(oidcUserInfoMock).when(oidcUserMock).getUserInfo();

        ReactiveOAuth2UserService<OidcUserRequest, OidcUser> userServiceMock = mock(ReactiveOAuth2UserService.class);
        when(userServiceMock.loadUser(any())).thenReturn(Mono.just(oidcUserMock));

        loader = new GoogleReactiveOAuth2UserLoader(userServiceMock);
    }

    @Test
    void shouldSupportOnlyGoogleClientRegistration() {
        ClientRegistration googleRegistration = TestClientRegistrations.clientRegistration()
                .registrationId("google")
                .build();
        assertTrue(loader.supports(googleRegistration));

        ClientRegistration otherRegistration = TestClientRegistrations.clientRegistration().build();
        assertFalse(loader.supports(otherRegistration));
    }

    @Test
    void shouldReturnInstanceOfGoogleOAuth2User() {
        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        OidcUserRequest requestMock = new MockOidcUserRequest(clientRegistration, CLAIMS);
        OidcUser user = loader.loadUser(requestMock).block();
        assertTrue(user instanceof GoogleOAuth2User);
        assertEquals(CLAIMS.get("email"), user.getEmail());
        assertEquals(CLAIMS.get("name"), user.getFullName());
        assertEquals(CLAIMS.get("picture"), user.getPicture());
    }

    @Test
    void shouldUseProvidedUserNameAttributeName() {
        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        OidcUserRequest requestMock = new MockOidcUserRequest(clientRegistration, CLAIMS);
        OidcUser user = loader.loadUser(requestMock).block();
        assertNotNull(user);
        assertEquals(CLAIMS.get("id"), user.getName());
    }

    @Test
    void shouldUseSubjectAsUserNameWhenUserNameAttributeNameIsNotSpecified() {
        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration()
                .userNameAttributeName(null)
                .build();
        OidcUserRequest requestMock = new MockOidcUserRequest(clientRegistration, CLAIMS);
        OidcUser user = loader.loadUser(requestMock).block();
        assertNotNull(user);
        assertEquals(CLAIMS.get("sub"), user.getName());
    }
}

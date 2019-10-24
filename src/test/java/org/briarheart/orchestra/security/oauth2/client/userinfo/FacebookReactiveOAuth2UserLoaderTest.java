package org.briarheart.orchestra.security.oauth2.client.userinfo;

import org.briarheart.orchestra.security.oauth2.client.oidc.userinfo.MockOidcUserRequest;
import org.briarheart.orchestra.security.oauth2.client.registration.TestClientRegistrations;
import org.briarheart.orchestra.security.oauth2.core.user.FacebookOAuth2User;
import org.briarheart.orchestra.security.oauth2.core.user.OAuth2UserAttributeAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class FacebookReactiveOAuth2UserLoaderTest {
    private static final Map<String, Object> ATTRIBUTES = Map.ofEntries(
            entry("id", "1"),
            entry("email", "test@example.com"),
            entry("name", "John Doe"),
            entry("picture", "http://example.com/picture")
    );

    private OAuth2User oAuth2UserMock;
    private FacebookReactiveOAuth2UserLoader loader;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        oAuth2UserMock = mock(OAuth2User.class);
        doReturn(Set.of(new SimpleGrantedAuthority("user"))).when(oAuth2UserMock).getAuthorities();

        ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> userServiceMock = mock(ReactiveOAuth2UserService.class);
        when(userServiceMock.loadUser(any())).thenReturn(Mono.just(oAuth2UserMock));

        loader = new FacebookReactiveOAuth2UserLoader(userServiceMock);
    }

    @Test
    void shouldSupportOnlyFacebookClientRegistration() {
        ClientRegistration facebookRegistration = TestClientRegistrations.clientRegistration()
                .registrationId("facebook")
                .build();
        assertTrue(loader.supports(facebookRegistration));

        ClientRegistration otherRegistration = TestClientRegistrations.clientRegistration().build();
        assertFalse(loader.supports(otherRegistration));
    }

    @Test
    void shouldReturnInstanceOfFacebookOAuth2User() {
        doReturn(ATTRIBUTES).when(oAuth2UserMock).getAttributes();

        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        OAuth2UserRequest requestMock = new MockOidcUserRequest(clientRegistration, ATTRIBUTES);
        OAuth2User user = loader.loadUser(requestMock).block();
        assertTrue(user instanceof FacebookOAuth2User);
        assertEquals(ATTRIBUTES.get("email"), ((FacebookOAuth2User) user).getEmail());
        assertEquals(ATTRIBUTES.get("name"), ((FacebookOAuth2User) user).getFullName());
        assertEquals(ATTRIBUTES.get("picture"), ((FacebookOAuth2User) user).getPicture());
    }

    @Test
    void shouldBuildPictureUriWhenLoadedUserAttributesDoNotIncludeIt() {
        Map<String, Object> attrsWithoutPicture = new HashMap<>(ATTRIBUTES);
        attrsWithoutPicture.remove("picture");
        doReturn(attrsWithoutPicture).when(oAuth2UserMock).getAttributes();

        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        OidcUserRequest requestMock = new MockOidcUserRequest(clientRegistration, attrsWithoutPicture);
        OAuth2User user = loader.loadUser(requestMock).block();
        assertNotNull(user);
        assertNotNull(((OAuth2UserAttributeAccessor) user).getPicture());
    }
}

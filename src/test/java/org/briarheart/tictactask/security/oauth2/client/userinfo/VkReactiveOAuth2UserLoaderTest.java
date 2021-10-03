package org.briarheart.tictactask.security.oauth2.client.userinfo;

import org.briarheart.tictactask.security.oauth2.client.oidc.userinfo.MockOidcUserRequest;
import org.briarheart.tictactask.security.oauth2.client.registration.TestClientRegistrations;
import org.briarheart.tictactask.security.oauth2.core.user.VkOAuth2User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
class VkReactiveOAuth2UserLoaderTest {
    private static final Map<String, Object> ATTRIBUTES = Map.ofEntries(
            entry("id", "1"),
            entry("email", "test@example.com"),
            entry("first_name", "John"),
            entry("last_name", "Doe"),
            entry("photo_100", "http://example.com/picture")
    );

    private OAuth2User oAuth2UserMock;
    private VkReactiveOAuth2UserLoader loader;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        oAuth2UserMock = mock(OAuth2User.class);
        doReturn(Set.of(new SimpleGrantedAuthority("user"))).when(oAuth2UserMock).getAuthorities();

        ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> userServiceMock;
        userServiceMock = mock(ReactiveOAuth2UserService.class);
        when(userServiceMock.loadUser(any())).thenReturn(Mono.just(oAuth2UserMock));

        loader = new VkReactiveOAuth2UserLoader(userServiceMock);
    }

    @Test
    void shouldSupportOnlyVkClientRegistration() {
        ClientRegistration vkRegistration = TestClientRegistrations.clientRegistration()
                .registrationId("vk")
                .build();
        assertTrue(loader.supports(vkRegistration));

        ClientRegistration otherRegistration = TestClientRegistrations.clientRegistration().build();
        assertFalse(loader.supports(otherRegistration));
    }

    @Test
    void shouldReturnInstanceOfVkOAuth2User() {
        doReturn(ATTRIBUTES).when(oAuth2UserMock).getAttributes();

        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        OAuth2UserRequest requestMock = new MockOidcUserRequest(clientRegistration, ATTRIBUTES);
        OAuth2User user = loader.loadUser(requestMock).block();
        assertTrue(user instanceof VkOAuth2User);
        assertEquals(ATTRIBUTES.get("email"), ((VkOAuth2User) user).getEmail());
        String expectedFullName = ATTRIBUTES.get("first_name") + " " + ATTRIBUTES.get("last_name");
        assertEquals(expectedFullName, ((VkOAuth2User) user).getFullName());
        assertEquals(ATTRIBUTES.get("photo_100"), ((VkOAuth2User) user).getPicture());
    }
}

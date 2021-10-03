package org.briarheart.tictactask.security.oauth2.client.userinfo;

import org.briarheart.tictactask.security.oauth2.client.oidc.userinfo.MockOidcUserRequest;
import org.briarheart.tictactask.security.oauth2.client.registration.TestClientRegistrations;
import org.briarheart.tictactask.security.oauth2.core.user.GithubOAuth2User;
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
class GithubReactiveOAuth2UserLoaderTest {
    private static final Map<String, Object> ATTRIBUTES = Map.ofEntries(
            entry("id", "1"),
            entry("email", "test@example.com"),
            entry("name", "John Doe"),
            entry("avatar_url", "http://example.com/picture")
    );

    private OAuth2User oAuth2UserMock;
    private GithubReactiveOAuth2UserLoader loader;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        oAuth2UserMock = mock(OAuth2User.class);
        doReturn(Set.of(new SimpleGrantedAuthority("user"))).when(oAuth2UserMock).getAuthorities();

        ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> userServiceMock = mock(ReactiveOAuth2UserService.class);
        when(userServiceMock.loadUser(any())).thenReturn(Mono.just(oAuth2UserMock));

        loader = new GithubReactiveOAuth2UserLoader(userServiceMock);
    }

    @Test
    void shouldSupportOnlyGithubClientRegistration() {
        ClientRegistration facebookRegistration = TestClientRegistrations.clientRegistration()
                .registrationId("github")
                .build();
        assertTrue(loader.supports(facebookRegistration));

        ClientRegistration otherRegistration = TestClientRegistrations.clientRegistration().build();
        assertFalse(loader.supports(otherRegistration));
    }

    @Test
    void shouldReturnInstanceOfGithubOAuth2User() {
        doReturn(ATTRIBUTES).when(oAuth2UserMock).getAttributes();

        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        OAuth2UserRequest requestMock = new MockOidcUserRequest(clientRegistration, ATTRIBUTES);
        OAuth2User user = loader.loadUser(requestMock).block();
        assertTrue(user instanceof GithubOAuth2User);
        assertEquals(ATTRIBUTES.get("email"), ((GithubOAuth2User) user).getEmail());
        assertEquals(ATTRIBUTES.get("name"), ((GithubOAuth2User) user).getFullName());
        assertEquals(ATTRIBUTES.get("avatar_url"), ((GithubOAuth2User) user).getPicture());
    }
}

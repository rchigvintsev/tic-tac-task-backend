package org.briarheart.tictactask.security.oauth2.client.userinfo;

import org.briarheart.tictactask.data.UserRepository;
import org.briarheart.tictactask.model.User;
import org.briarheart.tictactask.security.oauth2.core.user.OAuth2UserAttributeAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class ReactiveOAuth2UserLoaderManagerTest {
    private ReactiveOAuth2UserLoader<OAuth2UserRequest, OAuth2User> userLoader;
    private UserRepository userRepository;
    private ReactiveOAuth2UserLoaderManager<OAuth2UserRequest, OAuth2User> loaderManager;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        userLoader = mock(ReactiveOAuth2UserLoader.class);
        userRepository = mock(UserRepository.class);
        when(userRepository.save(any(User.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));
        loaderManager = new ReactiveOAuth2UserLoaderManager<>(List.of(userLoader), userRepository);
    }

    @Test
    void shouldThrowExceptionOnConstructWhenUserLoadersAreNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new ReactiveOAuth2UserLoaderManager<>(null, userRepository));
        assertEquals("User loaders must not be null or empty", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnConstructWhenUserLoadersAreEmpty() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new ReactiveOAuth2UserLoaderManager<>(List.of(), userRepository));
        assertEquals("User loaders must not be null or empty", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnConstructWhenUserRepositoryIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new ReactiveOAuth2UserLoaderManager<>(List.of(userLoader), null));
        assertEquals("User repository must not be null", e.getMessage());
    }

    @Test
    void shouldLoadUser() {
        ClientRegistration clientRegistration = mockClientRegistration();
        OAuth2UserRequest request = new OAuth2UserRequest(clientRegistration, mockAccessToken());

        String userEmail = "alice@mail.com";
        OAuth2User oAuth2User = mock(OAuth2User.class,
                withSettings().extraInterfaces(OAuth2UserAttributeAccessor.class));
        when(((OAuth2UserAttributeAccessor) oAuth2User).getEmail()).thenReturn(userEmail);

        when(userRepository.findByEmail(userEmail)).thenReturn(Mono.empty());

        when(userLoader.supports(clientRegistration)).thenReturn(true);
        when(userLoader.loadUser(request)).thenReturn(Mono.just(oAuth2User));

        OAuth2User result = loaderManager.loadUser(request).block();
        assertNotNull(result);
    }

    @Test
    void shouldSaveNewUserInDatabase() {
        ClientRegistration clientRegistration = mockClientRegistration();
        OAuth2UserRequest request = new OAuth2UserRequest(clientRegistration, mockAccessToken());

        String userEmail = "alice@mail.com";
        OAuth2User oAuth2User = mock(OAuth2User.class,
                withSettings().extraInterfaces(OAuth2UserAttributeAccessor.class));
        when(((OAuth2UserAttributeAccessor) oAuth2User).getEmail()).thenReturn(userEmail);

        when(userRepository.findByEmail(userEmail)).thenReturn(Mono.empty());

        when(userLoader.supports(clientRegistration)).thenReturn(true);
        when(userLoader.loadUser(request)).thenReturn(Mono.just(oAuth2User));

        loaderManager.loadUser(request).block();
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenLoadedUserAttributesDoNotIncludeEmail() {
        ClientRegistration clientRegistration = mockClientRegistration();
        OAuth2UserRequest request = new OAuth2UserRequest(clientRegistration, mockAccessToken());

        String username = "alice";
        OAuth2User oAuth2User = mock(OAuth2User.class,
                withSettings().extraInterfaces(OAuth2UserAttributeAccessor.class));
        when(oAuth2User.getName()).thenReturn(username);

        when(userLoader.supports(clientRegistration)).thenReturn(true);
        when(userLoader.loadUser(request)).thenReturn(Mono.just(oAuth2User));

        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class,
                () -> loaderManager.loadUser(request).block());
        assertEquals("OAuth2 user \"" + username + "\" does not have an email", e.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUserLoaderSupportingGivenClientRegistrationIsNotFound() {
        ClientRegistration clientRegistration = mockClientRegistration();
        OAuth2UserRequest request = new OAuth2UserRequest(clientRegistration, mockAccessToken());
        when(userLoader.supports(clientRegistration)).thenReturn(false);

        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class,
                () -> loaderManager.loadUser(request).block());
        assertEquals("OAuth2 user loader that supports client registration with id \""
                + clientRegistration.getRegistrationId() + "\" is not found", e.getMessage());
    }

    private ClientRegistration mockClientRegistration() {
        return ClientRegistration.withRegistrationId("test")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientId("wSN8ZMHhUB")
                .redirectUriTemplate("https://client.com/authorization/callback?code={code}")
                .authorizationUri("https://server.com/authorize")
                .tokenUri("https://server.com/token")
                .build();
    }

    private OAuth2AccessToken mockAccessToken() {
        return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "6336108148904e2885e2449080e9db4d",
                Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS));
    }
}

package org.briarheart.orchestra.security.oauth2.client.oidc.userinfo;

import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.security.oauth2.client.registration.TestClientRegistrations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DatabaseOidcReactiveOAuth2UserServiceTest {
    private static final Map<String, Object> CLAIMS = Map.ofEntries(
            entry("id", "1"),
            entry("sub", "test"),
            entry("email", "test@example.com"),
            entry("name", "John Doe"),
            entry("picture", "http://example.com/picture")
    );

    private UserRepository userRepositoryMock;
    private DatabaseOidcReactiveOAuth2UserService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        userRepositoryMock = mock(UserRepository.class);

        ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> userServiceMock = mock(ReactiveOAuth2UserService.class);
        Set<SimpleGrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("user"));
        OAuth2User oAuth2User = new DefaultOAuth2User(authorities, CLAIMS, "sub");
        when(userServiceMock.loadUser(any())).thenReturn(Mono.just(oAuth2User));

        service = new DatabaseOidcReactiveOAuth2UserService(userRepositoryMock);
        service.setOauth2UserService(userServiceMock);
    }

    @SuppressWarnings("UnassignedFluxMonoInstance")
    @Test
    void shouldSaveNewUserInDatabase() {
        when(userRepositoryMock.findById(anyString())).thenReturn(Mono.empty());
        when(userRepositoryMock.save(any())).then((Answer<Mono<User>>) invoc -> Mono.just(invoc.getArgument(0)));

        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        MockOidcUserRequest userRequestMock = new MockOidcUserRequest(clientRegistration, CLAIMS);

        service.loadUser(userRequestMock).block();
        verify(userRepositoryMock, times(1)).save(any());
    }

    @SuppressWarnings("UnassignedFluxMonoInstance")
    @Test
    void shouldUpdateExistingUserInDatabase() {
        User user = new User();
        when(userRepositoryMock.findById(anyString())).thenReturn(Mono.just(user));
        when(userRepositoryMock.save(any())).then((Answer<Mono<User>>) invoc -> Mono.just(invoc.getArgument(0)));

        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        MockOidcUserRequest userRequestMock = new MockOidcUserRequest(clientRegistration, CLAIMS);

        service.loadUser(userRequestMock).block();
        verify(userRepositoryMock, times(1)).save(any());
        assertEquals(1, user.getVersion());
        assertEquals(CLAIMS.get("name"), user.getFullName());
        assertEquals(CLAIMS.get("picture"), user.getImageUrl());
    }

    @SuppressWarnings("UnassignedFluxMonoInstance")
    @Test
    void shouldDoNothingWhenExistingUserDataIsActual() {
        User user = new User();
        user.setFullName(CLAIMS.get("name").toString());
        user.setImageUrl(CLAIMS.get("picture").toString());

        when(userRepositoryMock.findById(anyString())).thenReturn(Mono.just(user));
        when(userRepositoryMock.save(any())).then((Answer<Mono<User>>) invoc -> Mono.just(invoc.getArgument(0)));

        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        MockOidcUserRequest userRequestMock = new MockOidcUserRequest(clientRegistration, CLAIMS);

        service.loadUser(userRequestMock).block();
        verify(userRepositoryMock, times(0)).save(any());
    }

    @SuppressWarnings({"unchecked", "UnassignedFluxMonoInstance"})
    @Test
    void shouldDoNothingWhenUserInfoIsNotAvailable() {
        ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> userServiceMock = mock(ReactiveOAuth2UserService.class);
        when(userServiceMock.loadUser(any())).thenReturn(Mono.empty());
        service.setOauth2UserService(userServiceMock);

        ClientRegistration clientRegistration = TestClientRegistrations.clientRegistration().build();
        MockOidcUserRequest userRequestMock = new MockOidcUserRequest(clientRegistration, CLAIMS);

        service.loadUser(userRequestMock).block();
        verify(userRepositoryMock, times(0)).save(any());
    }
}

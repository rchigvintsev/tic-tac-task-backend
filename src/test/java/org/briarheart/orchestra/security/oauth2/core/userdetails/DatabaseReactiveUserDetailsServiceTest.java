package org.briarheart.orchestra.security.oauth2.core.userdetails;

import org.briarheart.orchestra.data.UserAuthorityRelationRepository;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.model.UserAuthorityRelation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Chigvintsev
 */
class DatabaseReactiveUserDetailsServiceTest {
    private DatabaseReactiveUserDetailsService service;
    private UserRepository userRepository;
    private UserAuthorityRelationRepository userAuthorityRelationRepository;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userAuthorityRelationRepository = mock(UserAuthorityRelationRepository.class);
        service = new DatabaseReactiveUserDetailsService(userRepository, userAuthorityRelationRepository);
    }

    @Test
    void shouldFindUserByUsername() {
        User user = User.builder().email("alice@mail.com").build();
        when(userRepository.findById(user.getEmail())).thenReturn(Mono.just(user));
        when(userAuthorityRelationRepository.findByEmail(anyString())).thenReturn(Flux.empty());
        UserDetails result = service.findByUsername(user.getUsername()).block();
        assertEquals(user, result);
    }

    @Test
    void shouldLoadUserAuthoritiesOnUserFind() {
        User user = User.builder().email("alice@mail.com").build();
        GrantedAuthority authority = new SimpleGrantedAuthority("user");

        when(userRepository.findById(user.getEmail())).thenReturn(Mono.just(user));
        when(userAuthorityRelationRepository.findByEmail(user.getEmail()))
                .thenReturn(Flux.just(new UserAuthorityRelation(user.getEmail(), authority.getAuthority())));
        UserDetails result = service.findByUsername(user.getUsername()).block();
        assertNotNull(result);
        assertEquals(List.of(authority), result.getAuthorities());
    }
}

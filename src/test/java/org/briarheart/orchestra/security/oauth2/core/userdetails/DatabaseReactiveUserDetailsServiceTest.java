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
import static org.mockito.ArgumentMatchers.anyLong;
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
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Mono.just(user));
        when(userAuthorityRelationRepository.findByUserId(anyLong())).thenReturn(Flux.empty());
        UserDetails result = service.findByUsername(user.getUsername()).block();
        assertEquals(user, result);
    }

    @Test
    void shouldLoadUserAuthoritiesOnUserFind() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        GrantedAuthority authority = new SimpleGrantedAuthority("user");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Mono.just(user));
        when(userAuthorityRelationRepository.findByUserId(user.getId()))
                .thenReturn(Flux.just(new UserAuthorityRelation(user.getId(), authority.getAuthority())));
        UserDetails result = service.findByUsername(user.getUsername()).block();
        assertNotNull(result);
        assertEquals(List.of(authority), result.getAuthorities());
    }
}

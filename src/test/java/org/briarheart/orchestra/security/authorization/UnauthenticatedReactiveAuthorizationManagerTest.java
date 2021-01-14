package org.briarheart.orchestra.security.authorization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Chigvintsev
 */
class UnauthenticatedReactiveAuthorizationManagerTest {
    private UnauthenticatedReactiveAuthorizationManager<Object> manager;

    @BeforeEach
    void setUp() {
        manager = UnauthenticatedReactiveAuthorizationManager.unauthenticated();
    }

    @Test
    void shouldGrantAccessWhenAuthenticationTokenIsNotPresent() {
        AuthorizationDecision decision = manager.check(Mono.empty(), null).block();
        assertNotNull(decision);
        assertTrue(decision.isGranted());
    }

    @Test
    void shouldGrantAccessWhenUserIsNotAuthenticated() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);
        AuthorizationDecision decision = manager.check(Mono.just(authentication), null).block();
        assertNotNull(decision);
        assertTrue(decision.isGranted());
    }

    @Test
    void shouldGrantAccessWhenUserIsAnonymous() {
        Set<? extends GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"));
        Authentication authentication = new AnonymousAuthenticationToken("anonymous", "anonymousUser", authorities);
        AuthorizationDecision decision = manager.check(Mono.just(authentication), null).block();
        assertNotNull(decision);
        assertTrue(decision.isGranted());
    }

    @Test
    void shouldDenyAccessWhenUserIsAuthenticated() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        AuthorizationDecision decision = manager.check(Mono.just(authentication), null).block();
        assertNotNull(decision);
        assertFalse(decision.isGranted());
    }
}

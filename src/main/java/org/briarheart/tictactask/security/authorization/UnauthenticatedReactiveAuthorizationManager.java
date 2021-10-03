package org.briarheart.tictactask.security.authorization;

import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link ReactiveAuthorizationManager} that determines if current user is unauthenticated.
 *
 * @author Roman Chigvintsev
 */
public class UnauthenticatedReactiveAuthorizationManager<T> implements ReactiveAuthorizationManager<T> {
    private static final AuthorizationDecision POSITIVE_DECISION = new AuthorizationDecision(true);
    private static final AuthorizationDecision NEGATIVE_DECISION = new AuthorizationDecision(false);

    private final AuthenticationTrustResolver authTrustResolver = new AuthenticationTrustResolverImpl();

    private UnauthenticatedReactiveAuthorizationManager() {
    }

    /**
     * Creates new instance of {@link UnauthenticatedReactiveAuthorizationManager}.
     *
     * @return new instance of {@link UnauthenticatedReactiveAuthorizationManager}
     */
    public static <T> UnauthenticatedReactiveAuthorizationManager<T> unauthenticated() {
        return new UnauthenticatedReactiveAuthorizationManager<>();
    }

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, T object) {
        return authentication
                .filter(this::isNotAnonymous)
                .map(a -> a.isAuthenticated() ? NEGATIVE_DECISION : POSITIVE_DECISION)
                .defaultIfEmpty(POSITIVE_DECISION);
    }

    /**
     * Verifies that the given authentication is not anonymous.
     *
     * @param authentication authentication to be checked
     * @return {@code true} if not anonymous, {@code false} otherwise
     */
    private boolean isNotAnonymous(Authentication authentication) {
        return !authTrustResolver.isAnonymous(authentication);
    }
}

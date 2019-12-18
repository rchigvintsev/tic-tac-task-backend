package org.briarheart.orchestra.security.web.server.authentication;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessToken;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessTokenService;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link ReactiveAuthenticationManager} that performs authentication based on the access token.
 * This authentication manager expects only instances of {@link AccessTokenAuthentication}.
 *
 * @author Roman Chigvintsev
 *
 * @see AccessToken
 * @see AccessTokenAuthentication
 */
@RequiredArgsConstructor
public class AccessTokenReactiveAuthenticationManager implements ReactiveAuthenticationManager {
    private final AccessTokenService accessTokenService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.fromCallable(() -> {
            String accessTokenValue = ((AccessTokenAuthentication) authentication).getTokenValue();
            AccessToken accessToken = accessTokenService.parseAccessToken(accessTokenValue);
            AuthenticatedPrincipal principal = new AccessTokenAuthenticatedPrincipal(accessToken);
            return new AccessTokenAuthentication(accessToken, principal);
        });
    }
}

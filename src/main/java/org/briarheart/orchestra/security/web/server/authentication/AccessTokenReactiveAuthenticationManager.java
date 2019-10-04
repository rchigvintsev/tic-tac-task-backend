package org.briarheart.orchestra.security.web.server.authentication;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
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
        String accessTokenValue = ((AccessTokenAuthentication) authentication).getTokenValue();
        try {
            AccessToken accessToken = accessTokenService.parseAccessToken(accessTokenValue);
            return Mono.just(new AccessTokenAuthentication(accessToken));
        } catch (InvalidAccessTokenException e) {
            return Mono.error(e);
        }
    }
}

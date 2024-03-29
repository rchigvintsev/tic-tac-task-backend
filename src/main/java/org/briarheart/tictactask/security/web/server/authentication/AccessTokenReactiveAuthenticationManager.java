package org.briarheart.tictactask.security.web.server.authentication;

import lombok.RequiredArgsConstructor;
import org.briarheart.tictactask.security.web.server.authentication.accesstoken.AccessToken;
import org.briarheart.tictactask.security.web.server.authentication.accesstoken.AccessTokenService;
import org.briarheart.tictactask.user.User;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link ReactiveAuthenticationManager} that performs authentication based on the access token.
 * This authentication manager expects only instances of {@link AccessTokenAuthentication}.
 *
 * @author Roman Chigvintsev
 * @see AccessToken
 * @see AccessTokenAuthentication
 */
@RequiredArgsConstructor
public class AccessTokenReactiveAuthenticationManager implements ReactiveAuthenticationManager {
    private final AccessTokenService accessTokenService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String accessTokenValue = ((AccessTokenAuthentication) authentication).getTokenValue();
        return accessTokenService.parseAccessToken(accessTokenValue)
                .map(accessToken -> {
                    User principal = User.builder()
                            .id(Long.parseLong(accessToken.getSubject()))
                            .email(accessToken.getEmail())
                            .emailConfirmed(true)
                            .enabled(true)
                            .fullName(accessToken.getFullName())
                            .profilePictureUrl(accessToken.getProfilePictureUrl())
                            .admin(accessToken.isAdmin())
                            .build();
                    return new AccessTokenAuthentication(accessToken, principal);
                });
    }
}

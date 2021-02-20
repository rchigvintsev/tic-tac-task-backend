package org.briarheart.orchestra.security.web.server.authentication;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessToken;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessTokenService;
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
                            .fullName(accessToken.getFullName())
                            .profilePictureUrl(accessToken.getProfilePictureUrl())
                            .build();
                    return new AccessTokenAuthentication(accessToken, principal);
                });
    }
}

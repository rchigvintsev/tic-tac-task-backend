package org.briarheart.orchestra.security.web.server.authentication;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessToken;
import org.springframework.security.core.AuthenticatedPrincipal;

/**
 * Representation of an authenticated principal for authentication based on the access token.
 *
 * @author Roman Chigvintsev
 *
 * @see AccessToken
 */
@RequiredArgsConstructor
public class AccessTokenAuthenticatedPrincipal implements AuthenticatedPrincipal {
    private final AccessToken accessToken;

    @Override
    public String getName() {
        return accessToken.getSubject();
    }
}

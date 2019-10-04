package org.briarheart.orchestra.security.web.server.authentication;

import org.briarheart.orchestra.model.User;

/**
 * Service for creating of access tokens.
 *
 * @author Roman Chigvintsev
 *
 * @see AccessToken
 */
public interface AccessTokenService {
    /**
     * Creates new access token based on the given user entity.
     *
     * @param user user for whom an access token should be created (must not be {@code null})
     *
     * @return new access token
     */
    AccessToken createAccessToken(User user);

    /**
     * Creates access token from the given token value.
     *
     * @param tokenValue access token value (must not be {@code null} or empty)
     *
     * @return parsed access token
     *
     * @throws InvalidAccessTokenException if error occurred while parsing access token value
     */
    AccessToken parseAccessToken(String tokenValue) throws InvalidAccessTokenException;
}

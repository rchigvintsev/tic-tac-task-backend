package org.briarheart.orchestra.security.web.server.authentication;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.security.core.Authentication} that uses an access token as client
 * credentials.
 *
 * @author Roman Chigvintsev
 *
 * @see AccessToken
 */
public final class AccessTokenAuthentication extends AbstractAuthenticationToken {
    @Getter
    private final AccessToken token;

    @Getter
    private final String tokenValue;

    /**
     * Creates new unauthenticated instance of this class with the given access token value. To create authenticated
     * token use {@link #AccessTokenAuthentication(AccessToken)} instead.
     *
     * @param tokenValue access token values (must not be {@code null} or empty)
     */
    public AccessTokenAuthentication(String tokenValue) {
        super(null);
        Assert.hasText(tokenValue, "Token value must not be null or empty");
        this.token = null;
        this.tokenValue = tokenValue;
    }

    /**
     * Creates new authenticated instance of this class with the given access token.
     *
     * @param token access token (must not be {@code null})
     */
    public AccessTokenAuthentication(AccessToken token) {
        super(null);
        Assert.notNull(token, "Token must not be null");
        this.token = token;
        this.tokenValue = token.getTokenValue();
        super.setAuthenticated(true);
    }

    /**
     * Returns access token value.
     *
     * @return access token value
     */
    @Override
    public String getCredentials() {
        return tokenValue;
    }

    /**
     * Always returns {@code null}.
     *
     * @return {@code null}
     */
    @Override
    public Object getPrincipal() {
        return null;
    }

    /**
     * Makes this token unauthenticated. This method forbids making token authenticated. Instead constructor
     * {@link #AccessTokenAuthentication(AccessToken)} should be used.
     *
     * @param isAuthenticated {@code false} if this token should be unauthenticated; {@code true} is forbidden
     *
     * @throws IllegalArgumentException if {@code true} is passed as method argument
     */
    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated) {
            throw new IllegalArgumentException("Cannot set this token to trusted - use constructor which takes " +
                    "an authentication token instead");
        }
        super.setAuthenticated(false);
    }
}

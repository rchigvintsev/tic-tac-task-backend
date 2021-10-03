package org.briarheart.tictactask.security.web.server.authentication.accesstoken;

import org.springframework.security.authentication.BadCredentialsException;

/**
 * Thrown if an authentication request is rejected because the access token is invalid.
 *
 * @author Roman Chigvintsev
 */
public class InvalidAccessTokenException extends BadCredentialsException {
    public InvalidAccessTokenException(String msg, Throwable t) {
        super(msg, t);
    }
}

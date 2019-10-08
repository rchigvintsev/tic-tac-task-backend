package org.briarheart.orchestra.security.oauth2.client.web.server;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Roman Chigvintsev
 */
public class ClientRedirectUriMissingException extends ResponseStatusException {
    public ClientRedirectUriMissingException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}

package org.briarheart.tictactask.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception being thrown in case some required form parameter is not provided.
 *
 * @author Roman Chigvintsev
 */
public class RequiredFormParameterMissingException extends ResponseStatusException {
    /**
     * Creates new instance of this class with the given error message.
     *
     * @param message error message
     */
    public RequiredFormParameterMissingException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}

package org.briarheart.orchestra.service;

/**
 * Exception indicating that token is expired.
 *
 * @author Roman Chigvintsev
 */
public class TokenExpiredException extends RuntimeException {
    /**
     * Creates new instance of this class with the given error message.
     *
     * @param message error message
     */
    public TokenExpiredException(String message) {
        super(message);
    }
}

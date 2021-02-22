package org.briarheart.orchestra.service;

/**
 * Exception indicating that email confirmation token is expired.
 *
 * @author Roman Chigvintsev
 * @see org.briarheart.orchestra.model.EmailConfirmationToken
 */
public class EmailConfirmationTokenExpiredException extends RuntimeException {
    /**
     * Creates new instance of this class with the given error message.
     *
     * @param message error message
     */
    public EmailConfirmationTokenExpiredException(String message) {
        super(message);
    }
}

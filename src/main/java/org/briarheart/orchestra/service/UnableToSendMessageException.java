package org.briarheart.orchestra.service;

/**
 * Exception indicating that some error occurred while trying to send a message.
 *
 * @author Roman Chigvintsev
 */
public class UnableToSendMessageException extends RuntimeException {
    /**
     * Creates new instance of this class with the given error message and cause.
     *
     * @param message error message
     * @param cause error cause
     */
    public UnableToSendMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}

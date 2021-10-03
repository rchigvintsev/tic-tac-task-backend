package org.briarheart.tictactask.service;

import org.briarheart.tictactask.LocalizedRuntimeException;

/**
 * Exception indicating that some error occurred while trying to send a message.
 *
 * @author Roman Chigvintsev
 */
public class UnableToSendMessageException extends LocalizedRuntimeException {
    /**
     * Creates new instance of this class with the given error message, localized error message and cause.
     *
     * @param message error message
     * @param localizedMessage localized error message
     * @param cause error cause
     */
    public UnableToSendMessageException(String message, String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }
}

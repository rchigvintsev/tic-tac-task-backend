package org.briarheart.tictactask.user;

/**
 * Exception indicating that uploaded file is too large.
 *
 * @author Roman Chigvintsev
 */
public class FileTooLargeException extends RuntimeException {
    /**
     * Creates new instance of this class with the given error message.
     *
     * @param message error message
     */
    public FileTooLargeException(String message) {
        super(message);
    }
}

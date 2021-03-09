package org.briarheart.orchestra;

/**
 * Base class for runtime exceptions providing additional localized error message that can be shown to user.
 *
 * @author Roman Chigvintsev
 */
public class LocalizedRuntimeException extends RuntimeException {
    private final String localizedMessage;

    /**
     * Creates new instance of this class with the given error message.
     *
     * @param message error message
     */
    public LocalizedRuntimeException(String message) {
        this(message, null);
    }

    /**
     * Creates new instance of this class with the given error message and localized error message.
     *
     * @param message error message
     * @param localizedMessage localized error message
     */
    public LocalizedRuntimeException(String message, String localizedMessage) {
        this(message, localizedMessage, null);
    }

    /**
     * Creates new instance of this class with the given error message, localized error message and cause.
     *
     * @param message error message
     * @param localizedMessage localized error message
     * @param cause error cause
     */
    public LocalizedRuntimeException(String message, String localizedMessage, Throwable cause) {
        super(message, cause);
        this.localizedMessage = localizedMessage;
    }

    @Override
    public String getLocalizedMessage() {
        return localizedMessage;
    }
}

package org.briarheart.orchestra.data;

import org.briarheart.orchestra.LocalizedRuntimeException;

/**
 * Exception being thrown in case an entity with some attributes already exists.
 *
 * @author Roman Chigvintsev
 */
public class EntityAlreadyExistsException extends LocalizedRuntimeException {
    /**
     * Creates new instance of this class with the given error message.
     *
     * @param message error message
     */
    public EntityAlreadyExistsException(String message) {
        super(message);
    }

    /**
     * Creates new instance of this class with the given error message and localized error message.
     *
     * @param message error message
     * @param localizedMessage localized error message
     */
    public EntityAlreadyExistsException(String message, String localizedMessage) {
        super(message, localizedMessage);
    }
}

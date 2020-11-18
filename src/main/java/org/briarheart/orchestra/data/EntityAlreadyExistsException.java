package org.briarheart.orchestra.data;

/**
 * Exception being thrown in case an entity with some attributes already exists.
 *
 * @author Roman Chigvintsev
 */
public class EntityAlreadyExistsException extends RuntimeException {
    /**
     * Creates new instance of this class with the given error message.
     *
     * @param message error message
     */
    public EntityAlreadyExistsException(String message) {
        super(message);
    }
}

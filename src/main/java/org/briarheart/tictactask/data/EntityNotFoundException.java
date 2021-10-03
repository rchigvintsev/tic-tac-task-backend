package org.briarheart.tictactask.data;

/**
 * Exception being thrown in case an entity is not found by some criteria.
 *
 * @author Roman Chigvintsev
 */
public class EntityNotFoundException extends RuntimeException {
    /**
     * Creates new instance of this class with the given error message.
     *
     * @param message error message
     */
    public EntityNotFoundException(String message) {
        super(message);
    }
}

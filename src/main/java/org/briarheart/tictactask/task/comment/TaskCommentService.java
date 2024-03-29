package org.briarheart.tictactask.task.comment;

import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.user.User;
import reactor.core.publisher.Mono;

/**
 * Service for task comment managing.
 *
 * @author Roman Chigvintsev
 * @see TaskComment
 */
public interface TaskCommentService {
    /**
     * Updates comment.
     *
     * @param comment comment to be updated (must not be {@code null})
     * @return updated comment
     * @throws EntityNotFoundException if comment is not found
     */
    Mono<TaskComment> updateComment(TaskComment comment) throws EntityNotFoundException;

    /**
     * Deletes comment with the given id and belonging to the given user.
     *
     * @param id   comment id
     * @param user comment author (must not be {@code null})
     */
    Mono<Void> deleteComment(Long id, User user) throws EntityNotFoundException;
}

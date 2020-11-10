package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.TaskComment;
import reactor.core.publisher.Mono;

/**
 * Service for task comment managing.
 *
 * @author Roman Chigvintsev
 * @see TaskComment
 */
public interface TaskCommentService {
    /**
     * Updates comment with the given id and belonging to the given author.
     *
     * @param comment comment to be updated (must not be {@code null})
     * @param id comment id
     * @param author comment author
     * @return updated comment
     * @throws EntityNotFoundException if comment is not found by id and author
     */
    Mono<TaskComment> updateComment(TaskComment comment, Long id, String author) throws EntityNotFoundException;

    /**
     * Deletes comment with the given id and belonging to the given author.
     *
     * @param id comment id
     * @param author comment author
     */
    Mono<Void> deleteComment(Long id, String author) throws EntityNotFoundException;
}

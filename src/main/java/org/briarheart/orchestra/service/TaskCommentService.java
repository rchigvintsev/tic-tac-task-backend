package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.TaskComment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for task comment managing.
 *
 * @author Roman Chigvintsev
 * @see TaskComment
 */
public interface TaskCommentService {
    /**
     * Returns all comments for task with the given id and belonging to the given author.
     *
     * @param taskId task id
     * @param taskAuthor task author
     * @return task comments or empty stream when there is no comment meeting the given criteria
     * @throws EntityNotFoundException if task is not found by id and author
     */
    Flux<TaskComment> getComments(Long taskId, String taskAuthor) throws EntityNotFoundException;

    /**
     * Creates new comment belonging to the given author and associates it with the given task.
     *
     * @param comment comment to be created (must not be {@code null})
     * @param commentAuthor comment author
     * @param taskId id of task with which the new comment should be associated
     * @return created comment
     * @throws EntityNotFoundException if task is not found by id
     */
    Mono<TaskComment> createComment(TaskComment comment, String commentAuthor, Long taskId)
            throws EntityNotFoundException;

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

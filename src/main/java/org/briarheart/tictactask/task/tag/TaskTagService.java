package org.briarheart.tictactask.task.tag;

import org.briarheart.tictactask.data.EntityAlreadyExistsException;
import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.task.Task;
import org.briarheart.tictactask.user.User;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for task tag managing.
 *
 * @author Roman Chigvintsev
 * @see TaskTag
 */
public interface TaskTagService {
    /**
     * Returns all tags belonging to the given user.
     *
     * @param user author of tags (must not be {@code null})
     * @return found tags or empty stream when there is no tag meeting the given criteria
     */
    Flux<TaskTag> getTags(User user);

    /**
     * Returns tag with the given id and belonging to the given user.
     *
     * @param id   tag id
     * @param user tag author (must not be {@code null})
     * @return requested tag
     * @throws EntityNotFoundException if tag is not found by id or does not belong to the given user
     */
    Mono<TaskTag> getTag(Long id, User user) throws EntityNotFoundException;

    /**
     * Creates new tag.
     *
     * @param tag tag to be created (must not be {@code null})
     * @return created tag
     * @throws EntityAlreadyExistsException if tag with the given name already exists
     */
    Mono<TaskTag> createTag(TaskTag tag) throws EntityAlreadyExistsException;

    /**
     * Updates tag.
     *
     * @param tag tag to be updated (must not be {@code null})
     * @return updated tag
     * @throws EntityNotFoundException      if tag is not found
     * @throws EntityAlreadyExistsException if tag with updated name already exists
     */
    Mono<TaskTag> updateTag(TaskTag tag) throws EntityNotFoundException, EntityAlreadyExistsException;

    /**
     * Deletes tag with the given id and belonging to the given user.
     *
     * @param id   tag id
     * @param user tag author (must not be {@code null})
     * @throws EntityNotFoundException if tag is not found by id or does not belong to the given user
     */
    Mono<Void> deleteTag(Long id, User user) throws EntityNotFoundException;

    /**
     * Returns uncompleted tasks (either unprocessed or processed) for tag with the given id and belonging to the given
     * user.
     *
     * @param tagId    tag id
     * @param user     tag author (must not be {@code null})
     * @param pageable paging restriction
     * @return uncompleted tasks or empty stream when there is no task meeting the given criteria
     * @throws EntityNotFoundException if tag is not found by id or does not belong to the given user
     */
    Flux<Task> getUncompletedTasks(Long tagId, User user, Pageable pageable);
}

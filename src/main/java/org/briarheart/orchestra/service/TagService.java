package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.Tag;
import org.briarheart.orchestra.model.Task;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for tag managing.
 *
 * @author Roman Chigvintsev
 * @see Tag
 */
public interface TagService {
    /**
     * Returns all tags belonging to the given author.
     *
     * @param author tag author
     * @return found tags or empty stream when there is no tag meeting the given criteria
     */
    Flux<Tag> getTags(String author);

    /**
     * Returns tag with the given id and belonging to the given author.
     *
     * @param id     tag id
     * @param author tag author
     * @return requested tag
     * @throws EntityNotFoundException if tag is not found by id and author
     */
    Mono<Tag> getTag(Long id, String author) throws EntityNotFoundException;

    /**
     * Creates new tag belonging to the given author.
     *
     * @param tag tag to be created (must not be {@code null})
     * @param author tag author (must not be {@code null} or empty)
     * @return created tag
     * @throws EntityAlreadyExistsException if tag with the given name already exists
     */
    Mono<Tag> createTag(Tag tag, String author) throws EntityAlreadyExistsException;

    /**
     * Updates tag with the given id and belonging to the given author.
     *
     * @param tag tag to be updated (must not be {@code null})
     * @param id tag id
     * @param author tag author
     * @return updated tag
     * @throws EntityNotFoundException if tag is not found by id and author
     * @throws EntityAlreadyExistsException if tag with updated name already exists
     */
    Mono<Tag> updateTag(Tag tag, Long id, String author) throws EntityNotFoundException, EntityAlreadyExistsException;

    /**
     * Deletes tag with the given id and belonging to the given author.
     *
     * @param id tag id
     * @param author tag author
     * @throws EntityNotFoundException if tag is not found by id and author
     */
    Mono<Void> deleteTag(Long id, String author) throws EntityNotFoundException;

    /**
     * Returns uncompleted tasks (either unprocessed or processed) for tag with the given id and belonging to the given
     * author.
     *
     * @param tagId tag id
     * @param tagAuthor tag author
     * @param pageable paging restriction
     * @return uncompleted tasks or empty stream when there is no task meeting the given criteria
     * @throws EntityNotFoundException if tag is not found by id and author
     */
    Flux<Task> getUncompletedTasks(Long tagId, String tagAuthor, Pageable pageable);
}

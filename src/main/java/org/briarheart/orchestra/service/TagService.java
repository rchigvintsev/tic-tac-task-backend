package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.Tag;
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
     * Updates tag with the given id and belonging to the given author.
     *
     * @param tag tag to be updated (must not be {@code null})
     * @param id tag id
     * @param author tag author
     * @return updated tag
     * @throws EntityNotFoundException if tag is not found by id and author
     */
    Mono<Tag> updateTag(Tag tag, Long id, String author) throws EntityNotFoundException;

    /**
     * Deletes tag with the given id and belonging to the given author.
     *
     * @param id tag id
     * @param author tag author
     */
    Mono<Void> deleteTag(Long id, String author);
}

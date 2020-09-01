package org.briarheart.orchestra.service;

import org.briarheart.orchestra.model.Tag;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

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
     * @param pageable paging restriction
     * @return found tags or empty stream when there is no tag meeting the given criteria
     */
    Flux<Tag> getTags(String author, Pageable pageable);
}

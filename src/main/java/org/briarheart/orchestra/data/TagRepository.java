package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.Tag;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * @author Roman Chigvintsev
 */
public interface TagRepository extends ReactiveCrudRepository<Tag, Long> {
    @Query("SELECT * FROM tag WHERE author = :author OFFSET :offset LIMIT :limit")
    Flux<Tag> findByAuthor(String author, long offset, Integer limit);
}

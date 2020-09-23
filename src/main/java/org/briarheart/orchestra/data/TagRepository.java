package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.Tag;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface TagRepository extends ReactiveCrudRepository<Tag, Long> {
    @Query("SELECT * FROM tag WHERE author = :author OFFSET :offset LIMIT :limit")
    Flux<Tag> findByAuthor(String author, long offset, Integer limit);

    @Query("SELECT * FROM tag WHERE id = :id AND author = :author")
    Mono<Tag> findByIdAndAuthor(Long id, String author);

    @Query("SELECT * FROM tag WHERE name = :name AND author = :author")
    Mono<Tag> findByNameAndAuthor(String name, String author);

    @Query("DELETE FROM tag WHERE id = :id AND author = :author")
    Mono<Void> deleteByIdAndAuthor(Long id, String author);
}

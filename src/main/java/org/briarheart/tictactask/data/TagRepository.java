package org.briarheart.tictactask.data;

import org.briarheart.tictactask.model.Tag;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * @author Roman Chigvintsev
 */
public interface TagRepository extends ReactiveCrudRepository<Tag, Long> {
    @Query("SELECT * FROM tag WHERE id IN (:ids)")
    Flux<Tag> findByIdIn(Set<Long> ids);

    @Query("SELECT * FROM tag WHERE user_id = :userId")
    Flux<Tag> findByUserId(Long userId);

    @Query("SELECT * FROM tag WHERE id = :id AND user_id = :userId")
    Mono<Tag> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT * FROM tag WHERE name = :name AND user_id = :userId")
    Mono<Tag> findByNameAndUserId(String name, Long userId);
}

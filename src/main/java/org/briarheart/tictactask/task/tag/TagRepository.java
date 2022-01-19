package org.briarheart.tictactask.task.tag;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * @author Roman Chigvintsev
 */
public interface TagRepository extends ReactiveCrudRepository<TaskTag, Long> {
    @Query("SELECT * FROM tag WHERE id IN (:ids)")
    Flux<TaskTag> findByIdIn(Set<Long> ids);

    @Query("SELECT * FROM tag WHERE user_id = :userId")
    Flux<TaskTag> findByUserId(Long userId);

    @Query("SELECT * FROM tag WHERE id = :id AND user_id = :userId")
    Mono<TaskTag> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT * FROM tag WHERE name = :name AND user_id = :userId")
    Mono<TaskTag> findByNameAndUserId(String name, Long userId);
}

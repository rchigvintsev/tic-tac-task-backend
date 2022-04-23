package org.briarheart.tictactask.task.tag;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface TaskTagRepository extends ReactiveCrudRepository<TaskTag, Long> {
    @Query("SELECT * FROM tag WHERE user_id = :userId ORDER BY created_at DESC")
    Flux<TaskTag> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT t.* " +
            "FROM tag t " +
            "INNER JOIN tasks_tags tt ON tt.tag_id = t.id " +
            "WHERE tt.task_id = :taskId AND t.user_id = :userId " +
            "ORDER BY tt.created_at")
    Flux<TaskTag> findByTaskIdAndUserIdOrderByCreatedAtDesc(Long taskId, Long userId);

    @Query("SELECT * FROM tag WHERE id = :id AND user_id = :userId")
    Mono<TaskTag> findByIdAndUserId(Long id, Long userId);
}

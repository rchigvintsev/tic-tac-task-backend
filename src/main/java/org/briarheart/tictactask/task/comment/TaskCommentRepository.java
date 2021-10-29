package org.briarheart.tictactask.task.comment;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface TaskCommentRepository extends ReactiveCrudRepository<TaskComment, Long> {
    @Query("SELECT * FROM task_comment WHERE task_id = :taskId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<TaskComment> findByTaskIdOrderByCreatedAtDesc(Long taskId, long offset, Integer limit);

    @Query("SELECT * FROM task_comment WHERE id = :id AND user_id = :userId")
    Mono<TaskComment> findByIdAndUserId(Long id, Long userId);

    @Query("DELETE FROM task_comment WHERE id = :id AND user_id = :userId")
    Mono<Void> deleteByIdAndUserId(Long id, Long userId);
}

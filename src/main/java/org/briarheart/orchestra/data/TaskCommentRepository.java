package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.TaskComment;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface TaskCommentRepository extends ReactiveCrudRepository<TaskComment, Long> {
    @Query("SELECT * FROM task_comment WHERE task_id = :taskId ORDER BY created_at DESC OFFSET :offset LIMIT :limit")
    Flux<TaskComment> findByTaskIdOrderByCreatedAtDesc(Long taskId, long offset, Integer limit);

    @Query("SELECT * FROM task_comment WHERE id = :id AND author = :author")
    Mono<TaskComment> findByIdAndAuthor(Long id, String author);

    @Query("DELETE FROM task_comment WHERE id = :id AND author = :author")
    Mono<Void> deleteByIdAndAuthor(Long id, String author);
}

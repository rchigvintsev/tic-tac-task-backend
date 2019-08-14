package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.TaskComment;
import org.springframework.data.r2dbc.repository.query.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * @author Roman Chigvintsev
 */
public interface TaskCommentRepository extends ReactiveCrudRepository<TaskComment, Long> {
    @Query("SELECT * FROM task_comment WHERE task_id = :taskId ORDER BY created_at DESC")
    Flux<TaskComment> findByTaskIdOrderByCreatedAtDesc(Long taskId);
}

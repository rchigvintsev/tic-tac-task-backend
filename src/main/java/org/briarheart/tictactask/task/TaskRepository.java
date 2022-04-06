package org.briarheart.tictactask.task;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface TaskRepository extends ReactiveCrudRepository<Task, Long>, CustomizedTaskRepository {
    @Query("SELECT * FROM task WHERE id = :id AND user_id = :userId")
    Mono<Task> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT * FROM task WHERE parent_id = :parent_id AND user_id = :userId")
    Flux<Task> findByParentIdAndUserId(Long parentId, Long userId);

    @Query("SELECT * FROM task WHERE status <> :status AND id IN (SELECT task_id FROM tasks_tags WHERE tag_id = :tagId)"
            + " ORDER BY created_at ASC LIMIT :limit OFFSET :offset")
    Flux<Task> findByStatusNotAndTagIdOrderByCreatedAtAsc(TaskStatus status, Long tagId, long offset, Integer limit);

    @Query("SELECT * FROM task WHERE task_list_id = :taskListId AND user_id = :userId ORDER BY created_at ASC "
            + "LIMIT :limit OFFSET :offset")
    Flux<Task> findByTaskListIdAndUserIdOrderByCreatedAtAsc(Long taskListId, Long userId, long offset, Integer limit);

    @Query("SELECT * FROM task WHERE task_list_id = :taskListId AND user_id = :userId AND status <> :status "
            + "ORDER BY created_at ASC LIMIT :limit OFFSET :offset")
    Flux<Task> findByTaskListIdAndUserIdAndStatusNotOrderByCreatedAtAsc(Long taskListId,
                                                                        Long userId,
                                                                        TaskStatus status,
                                                                        long offset,
                                                                        Integer limit);
}

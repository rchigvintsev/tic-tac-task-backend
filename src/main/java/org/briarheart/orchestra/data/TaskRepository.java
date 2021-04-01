package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * @author Roman Chigvintsev
 */
public interface TaskRepository extends ReactiveCrudRepository<Task, Long> {
    @Query("SELECT * FROM task WHERE id = :id AND user_id = :userId")
    Mono<Task> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT * FROM task WHERE status = :status AND user_id = :userId ORDER BY created_at ASC LIMIT :limit "
            + "OFFSET :offset")
    Flux<Task> findByStatusAndUserIdOrderByCreatedAtAsc(TaskStatus status, Long userId, long offset, Integer limit);

    @Query("SELECT * FROM task WHERE status <> :status AND user_id = :userId ORDER BY created_at ASC LIMIT :limit "
            + "OFFSET :offset")
    Flux<Task> findByStatusNotAndUserIdOrderByCreatedAtAsc(TaskStatus status, Long userId, long offset, Integer limit);

    @Query("SELECT * FROM task WHERE status <> :status AND id IN (SELECT task_id FROM tasks_tags WHERE tag_id = :tagId)"
            + " ORDER BY created_at ASC LIMIT :limit OFFSET :offset")
    Flux<Task> findByStatusNotAndTagIdOrderByCreatedAtAsc(TaskStatus status, Long tagId, long offset, Integer limit);

    @Query("SELECT * FROM task WHERE task_list_id = :taskListId AND user_id = :userId ORDER BY created_at ASC "
            + "LIMIT :limit OFFSET :offset")
    Flux<Task> findByTaskListIdAndUserIdOrderByCreatedAtAsc(Long taskListId, Long userId, long offset, Integer limit);

    @Query("SELECT * FROM task WHERE deadline IS NULL AND status = :status AND user_id = :userId "
            + "ORDER BY created_at ASC LIMIT :limit OFFSET :offset")
    Flux<Task> findByDeadlineIsNullAndStatusAndUserIdOrderByCreatedAtAsc(TaskStatus status,
                                                                         Long userId,
                                                                         long offset,
                                                                         Integer limit);

    @Query("SELECT * FROM task WHERE deadline <= :deadline AND status = :status AND user_id = :userId "
            + "ORDER BY created_at ASC LIMIT :limit OFFSET :offset")
    Flux<Task> findByDeadlineLessThanEqualAndStatusAndUserIdOrderByCreatedAtAsc(LocalDateTime deadline,
                                                                                TaskStatus status,
                                                                                Long userId,
                                                                                long offset,
                                                                                Integer limit);


    @Query("SELECT * FROM task WHERE deadline >= :deadline AND status = :status AND user_id = :userId "
            + "ORDER BY created_at ASC LIMIT :limit OFFSET :offset")
    Flux<Task> findByDeadlineGreaterThanEqualAndStatusAndUserIdOrderByCreatedAtAsc(LocalDateTime deadline,
                                                                                   TaskStatus status,
                                                                                   Long userId,
                                                                                   long offset,
                                                                                   Integer limit);

    @Query("SELECT * "
            + "FROM task "
            + "WHERE (deadline BETWEEN :deadlineFrom AND :deadlineTo) "
            + "AND status = :status "
            + "AND user_id = :userId "
            + "ORDER BY created_at ASC "
            + "LIMIT :limit OFFSET :offset")
    Flux<Task> findByDeadlineBetweenAndStatusAndUserIdOrderByCreatedAtAsc(LocalDateTime deadlineFrom,
                                                                          LocalDateTime deadlineTo,
                                                                          TaskStatus status,
                                                                          Long userId,
                                                                          long offset,
                                                                          Integer limit);

    @Query("SELECT COUNT(*) FROM task WHERE status = :status AND user_id = :userId")
    Mono<Long> countAllByStatusAndUserId(TaskStatus status, Long userId);

    @Query("SELECT COUNT(*) FROM task WHERE status <> :status AND user_id = :userId")
    Mono<Long> countAllByStatusNotAndUserId(TaskStatus status, Long userId);

    @Query("SELECT COUNT(*) FROM task WHERE deadline IS NULL AND status = :status AND user_id = :userId")
    Mono<Long> countAllByDeadlineIsNullAndStatusAndUserId(TaskStatus status, Long userId);

    @Query("SELECT COUNT(*) FROM task WHERE deadline <= :deadline AND status = :status AND user_id = :userId")
    Mono<Long> countAllByDeadlineLessThanEqualAndStatusAndUserId(LocalDateTime deadline,
                                                                 TaskStatus status,
                                                                 Long userId);

    @Query("SELECT COUNT(*) FROM task WHERE deadline >= :deadline AND status = :status AND user_id = :userId")
    Mono<Long> countAllByDeadlineGreaterThanEqualAndStatusAndUserId(LocalDateTime deadline,
                                                                    TaskStatus status,
                                                                    Long userId);

    @Query("SELECT COUNT(*) "
            + "FROM task "
            + "WHERE (deadline BETWEEN :deadlineFrom AND :deadlineTo) "
            + "AND status = :status "
            + "AND user_id = :userId")
    Mono<Long> countAllByDeadlineBetweenAndStatusAndUserId(LocalDateTime deadlineFrom,
                                                           LocalDateTime deadlineTo,
                                                           TaskStatus status,
                                                           Long userId);
}

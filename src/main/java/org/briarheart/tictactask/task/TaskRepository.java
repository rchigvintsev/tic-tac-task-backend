package org.briarheart.tictactask.task;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * @author Roman Chigvintsev
 */
public interface TaskRepository extends ReactiveCrudRepository<Task, Long> {
    @Query("SELECT * FROM task WHERE id = :id AND user_id = :userId")
    Mono<Task> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT * FROM task WHERE status IN (:status) AND user_id = :userId ORDER BY created_at ASC LIMIT :limit "
            + "OFFSET :offset")
    Flux<Task> findByStatusInAndUserIdOrderByCreatedAtAsc(Set<TaskStatus> status, Long userId, long offset,
                                                          Integer limit);

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

    @Query("SELECT * FROM task WHERE deadline IS NULL AND status IN (:status) AND user_id = :userId "
            + "ORDER BY created_at ASC LIMIT :limit OFFSET :offset")
    Flux<Task> findByDeadlineIsNullAndStatusInAndUserIdOrderByCreatedAtAsc(Set<TaskStatus> status,
                                                                           Long userId,
                                                                           long offset,
                                                                           Integer limit);

    @Query("SELECT * FROM task WHERE deadline <= :deadline AND status IN (:status) AND user_id = :userId "
            + "ORDER BY created_at ASC LIMIT :limit OFFSET :offset")
    Flux<Task> findByDeadlineLessThanEqualAndStatusInAndUserIdOrderByCreatedAtAsc(LocalDateTime deadline,
                                                                                  Set<TaskStatus> status,
                                                                                  Long userId,
                                                                                  long offset,
                                                                                  Integer limit);

    @Query("SELECT * FROM task WHERE deadline >= :deadline AND status IN (:status) AND user_id = :userId "
            + "ORDER BY created_at ASC LIMIT :limit OFFSET :offset")
    Flux<Task> findByDeadlineGreaterThanEqualAndStatusInAndUserIdOrderByCreatedAtAsc(LocalDateTime deadline,
                                                                                     Set<TaskStatus> status,
                                                                                     Long userId,
                                                                                     long offset,
                                                                                     Integer limit);

    @Query("SELECT * "
            + "FROM task "
            + "WHERE (deadline BETWEEN :deadlineFrom AND :deadlineTo) "
            + "AND status IN (:status) "
            + "AND user_id = :userId "
            + "ORDER BY created_at ASC "
            + "LIMIT :limit OFFSET :offset")
    Flux<Task> findByDeadlineBetweenAndStatusInAndUserIdOrderByCreatedAtAsc(LocalDateTime deadlineFrom,
                                                                            LocalDateTime deadlineTo,
                                                                            Set<TaskStatus> status,
                                                                            Long userId,
                                                                            long offset,
                                                                            Integer limit);

    @Query("SELECT COUNT(*) FROM task WHERE status IN (:statuses) AND user_id = :userId")
    Mono<Long> countAllByStatusInAndUserId(Set<TaskStatus> status, Long userId);

    @Query("SELECT COUNT(*) FROM task WHERE deadline IS NULL AND status IN (:statuses) AND user_id = :userId")
    Mono<Long> countAllByDeadlineIsNullAndStatusInAndUserId(Set<TaskStatus> statuses, Long userId);

    @Query("SELECT COUNT(*) FROM task WHERE deadline <= :deadline AND status IN (:status) AND user_id = :userId")
    Mono<Long> countAllByDeadlineLessThanEqualAndStatusInAndUserId(LocalDateTime deadline,
                                                                   Set<TaskStatus> status,
                                                                   Long userId);

    @Query("SELECT COUNT(*) FROM task WHERE deadline >= :deadline AND status IN (:status) AND user_id = :userId")
    Mono<Long> countAllByDeadlineGreaterThanEqualAndStatusInAndUserId(LocalDateTime deadline,
                                                                      Set<TaskStatus> status,
                                                                      Long userId);

    @Query("SELECT COUNT(*) "
            + "FROM task "
            + "WHERE (deadline BETWEEN :deadlineFrom AND :deadlineTo) "
            + "AND status IN (:status) "
            + "AND user_id = :userId")
    Mono<Long> countAllByDeadlineBetweenAndStatusInAndUserId(LocalDateTime deadlineFrom,
                                                             LocalDateTime deadlineTo,
                                                             Set<TaskStatus> status,
                                                             Long userId);
}

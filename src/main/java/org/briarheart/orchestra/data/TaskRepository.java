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
    @Query("SELECT * FROM task WHERE id = :id AND author = :author")
    Mono<Task> findByIdAndAuthor(Long id, String author);

    @Query("SELECT * FROM task WHERE status = :status AND author = :author OFFSET :offset LIMIT :limit")
    Flux<Task> findByStatusAndAuthor(TaskStatus status, String author, long offset, Integer limit);

    @Query("SELECT * FROM task WHERE status <> :status AND author = :author OFFSET :offset LIMIT :limit")
    Flux<Task> findByStatusNotAndAuthor(TaskStatus status, String author, long offset, Integer limit);

    @Query("SELECT * FROM task WHERE status <> :status AND id IN (SELECT task_id FROM tasks_tags WHERE tag_id = :tagId)"
            + " OFFSET :offset LIMIT :limit")
    Flux<Task> findByStatusNotAndTagId(TaskStatus status, Long tagId, long offset, Integer limit);

    @Query("SELECT * FROM task WHERE deadline IS NULL AND status = :status AND author = :author "
            + "OFFSET :offset LIMIT :limit")
    Flux<Task> findByDeadlineIsNullAndStatusAndAuthor(TaskStatus status, String author, long offset, Integer limit);

    @Query("SELECT * FROM task WHERE deadline <= :deadline AND status = :status AND author = :author "
            + "OFFSET :offset LIMIT :limit")
    Flux<Task> findByDeadlineLessThanEqualAndStatusAndAuthor(LocalDateTime deadline,
                                                             TaskStatus status,
                                                             String author,
                                                             long offset,
                                                             Integer limit);


    @Query("SELECT * FROM task WHERE deadline >= :deadline AND status = :status AND author = :author "
            + "OFFSET :offset LIMIT :limit")
    Flux<Task> findByDeadlineGreaterThanEqualAndStatusAndAuthor(LocalDateTime deadline,
                                                                TaskStatus status,
                                                                String author,
                                                                long offset,
                                                                Integer limit);

    @Query("SELECT * "
            + "FROM task "
            + "WHERE (deadline BETWEEN :deadlineFrom AND :deadlineTo) "
            + "AND status = :status "
            + "AND author = :author "
            + "OFFSET :offset LIMIT :limit")
    Flux<Task> findByDeadlineBetweenAndStatusAndAuthor(LocalDateTime deadlineFrom,
                                                       LocalDateTime deadlineTo,
                                                       TaskStatus status,
                                                       String author,
                                                       long offset,
                                                       Integer limit);

    @Query("SELECT COUNT(*) FROM task WHERE status = :status AND author = :author")
    Mono<Long> countAllByStatusAndAuthor(TaskStatus status, String author);

    @Query("SELECT COUNT(*) FROM task WHERE status <> :status AND author = :author")
    Mono<Long> countAllByStatusNotAndAuthor(TaskStatus status, String author);

    @Query("SELECT COUNT(*) FROM task WHERE deadline IS NULL AND status = :status AND author = :author")
    Mono<Long> countAllByDeadlineIsNullAndStatusAndAuthor(TaskStatus status, String author);

    @Query("SELECT COUNT(*) FROM task WHERE deadline <= :deadline AND status = :status AND author = :author")
    Mono<Long> countAllByDeadlineLessThanEqualAndStatusAndAuthor(LocalDateTime deadline,
                                                                 TaskStatus status,
                                                                 String author);

    @Query("SELECT COUNT(*) FROM task WHERE deadline >= :deadline AND status = :status AND author = :author")
    Mono<Long> countAllByDeadlineGreaterThanEqualAndStatusAndAuthor(LocalDateTime deadline,
                                                                    TaskStatus status,
                                                                    String author);

    @Query("SELECT COUNT(*) "
            + "FROM task "
            + "WHERE (deadline BETWEEN :deadlineFrom AND :deadlineTo) "
            + "AND status = :status "
            + "AND author = :author")
    Mono<Long> countAllByDeadlineBetweenAndStatusAndAuthor(LocalDateTime deadlineFrom,
                                                           LocalDateTime deadlineTo,
                                                           TaskStatus status,
                                                           String author);
}

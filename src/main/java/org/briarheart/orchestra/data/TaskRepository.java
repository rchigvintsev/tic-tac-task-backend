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
    @Query("SELECT * FROM task WHERE id= :id AND author = :author")
    Mono<Task> findByIdAndAuthor(Long id, String author);

    @Query("SELECT * FROM task WHERE status = :status AND author = :author")
    Flux<Task> findByStatusAndAuthor(TaskStatus status, String author);

    @Query("SELECT * FROM task WHERE status <> :status AND author = :author")
    Flux<Task> findByStatusNotAndAuthor(TaskStatus status, String author);

    @Query("SELECT * FROM task WHERE deadline IS NULL AND status = :status AND author = :author")
    Flux<Task> findByDeadlineIsNullAndStatusAndAuthor(TaskStatus status, String author);

    @Query("SELECT * FROM task WHERE deadline <= :deadline AND status = :status AND author = :author")
    Flux<Task> findByDeadlineLessThanEqualAndStatusAndAuthor(LocalDateTime deadlineTo,
                                                             TaskStatus status,
                                                             String author);


    @Query("SELECT * FROM task WHERE deadline >= :deadline AND status = :status AND author = :author")
    Flux<Task> findByDeadlineGreaterThanEqualAndStatusAndAuthor(LocalDateTime deadlineFrom,
                                                                TaskStatus status,
                                                                String author);

    @Query("SELECT * "
            + "FROM task "
            + "WHERE (deadline BETWEEN :deadlineFrom AND :deadlineTo) AND status = :status AND author = :author")
    Flux<Task> findByDeadlineBetweenAndStatusAndAuthor(LocalDateTime deadlineFrom,
                                                       LocalDateTime deadlineTo,
                                                       TaskStatus status,
                                                       String author);
}

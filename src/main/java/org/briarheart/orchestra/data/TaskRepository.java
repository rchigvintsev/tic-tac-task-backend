package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * @author Roman Chigvintsev
 */
public interface TaskRepository extends ReactiveCrudRepository<Task, Long> {
    @Query("SELECT * FROM task WHERE id= :id AND author = :author")
    Mono<Task> findByIdAndAuthor(Long id, String author);

    @Query("SELECT * FROM task WHERE status = :status AND author = :author OFFSET :offset LIMIT :limit")
    Flux<Task> findByStatusAndAuthor(TaskStatus status, String author, long offset, Integer limit);

    @Query("SELECT * FROM task WHERE status <> :status AND author = :author OFFSET :offset LIMIT :limit")
    Flux<Task> findByStatusNotAndAuthor(TaskStatus status, String author, long offset, Integer limit);

    @Query("SELECT * FROM task WHERE deadline_date IS NULL AND status = :status AND author = :author "
            + "OFFSET :offset LIMIT :limit")
    Flux<Task> findByDeadlineDateIsNullAndStatusAndAuthor(TaskStatus status, String author, long offset, Integer limit);

    @Query("SELECT * FROM task WHERE deadline_date <= :deadlineDate AND status = :status AND author = :author "
            + "OFFSET :offset LIMIT :limit")
    Flux<Task> findByDeadlineDateLessThanEqualAndStatusAndAuthor(LocalDate deadlineDate,
                                                                 TaskStatus status,
                                                                 String author,
                                                                 long offset,
                                                                 Integer limit);


    @Query("SELECT * FROM task WHERE deadline_date >= :deadlineDate AND status = :status AND author = :author "
            + "OFFSET :offset LIMIT :limit")
    Flux<Task> findByDeadlineDateGreaterThanEqualAndStatusAndAuthor(LocalDate deadlineDate,
                                                                    TaskStatus status,
                                                                    String author,
                                                                    long offset,
                                                                    Integer limit);

    @Query("SELECT * "
            + "FROM task "
            + "WHERE (deadline_date BETWEEN :deadlineDateFrom AND :deadlineDateTo) "
            + "AND status = :status "
            + "AND author = :author "
            + "OFFSET :offset LIMIT :limit")
    Flux<Task> findByDeadlineDateBetweenAndStatusAndAuthor(LocalDate deadlineDateFrom,
                                                           LocalDate deadlineDateTo,
                                                           TaskStatus status,
                                                           String author,
                                                           long offset,
                                                           Integer limit);
}

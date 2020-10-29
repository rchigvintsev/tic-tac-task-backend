package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.TaskList;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface TaskListRepository extends ReactiveCrudRepository<TaskList, Long> {
    @Query("SELECT * FROM task_list WHERE completed = :completed AND author = :author")
    Flux<TaskList> findByCompletedAndAuthor(boolean completed, String author);

    @Query("SELECT * FROM task_list WHERE id = :id AND author = :author")
    Mono<TaskList> findByIdAndAuthor(Long id, String author);

    @Query("DELETE FROM task_list WHERE id = :id AND author = :author")
    Mono<Void> deleteByIdAndAuthor(Long id, String author);
}

package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.Task;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Service for task managing.
 *
 * @author Roman Chigvintsev
 * @see Task
 */
public interface TaskService {
    /**
     * Returns all unprocessed tasks belonging to the given author.
     *
     * @param author task author
     * @param pageable paging restriction
     * @return unprocessed tasks or empty stream when there is no task meeting the given criteria
     */
    Flux<Task> getUnprocessedTasks(String author, Pageable pageable);

    /**
     * Returns all processed tasks belonging to the given author.
     *
     * @param author task author
     * @param pageable paging restriction
     * @return processed tasks or empty stream when there is no task meeting the given criteria
     */
    Flux<Task> getProcessedTasks(String author, Pageable pageable);

    /**
     * Returns processed tasks optionally falling within the given deadline bounds and belonging to the given author.
     * If deadline bounds are not specified this method returns processed tasks without deadline.
     *
     * @param deadlineDateFrom optional deadline from bound
     * @param deadlineDateTo optional deadline to bound
     * @param author task author
     * @param pageable paging restriction
     * @return processed tasks or empty stream when there is no task meeting the given criteria
     */
    Flux<Task> getProcessedTasks(
            LocalDate deadlineDateFrom,
            LocalDate deadlineDateTo,
            String author,
            Pageable pageable
    );

    /**
     * Returns all uncompleted tasks (either unprocessed or processed) belonging to the given author.
     *
     * @param author task author
     * @param pageable paging restriction
     * @return uncompleted tasks or empty stream when there is no task meeting the given criteria
     */
    Flux<Task> getUncompletedTasks(String author, Pageable pageable);

    /**
     * Returns task with the given id and belonging to the given author.
     *
     * @param id     task id
     * @param author task author
     * @return requested task
     * @throws EntityNotFoundException if task is not found by id and author
     */
    Mono<Task> getTask(Long id, String author) throws EntityNotFoundException;

    /**
     * Creates new task belonging to the given author.
     *
     * @param task task to be created (must not be {@code null})
     * @param author task author (must not be {@code null} or empty)
     * @return created task
     */
    Mono<Task> createTask(Task task, String author);

    /**
     * Updates task with the given id and belonging to the given author.
     *
     * @param task task to be updated (must not be {@code null})
     * @param id task id
     * @param author task author
     * @return updated task
     * @throws EntityNotFoundException if task is not found by id and author
     */
    Mono<Task> updateTask(Task task, Long id, String author) throws EntityNotFoundException;

    /**
     * Completes task with the given id and belonging to the given author.
     *
     * @param id task id
     * @param author task author
     * @throws EntityNotFoundException if task is not found by id and author
     */
    Mono<Void> completeTask(Long id, String author) throws EntityNotFoundException;
}

package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskList;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for task list managing.
 *
 * @author Roman Chigvintsev
 * @see TaskList
 */
public interface TaskListService {
    /**
     * Returns uncompleted task lists belonging to the given author.
     *
     * @param author task list author
     * @return found task lists or empty stream when there is no task list meeting the given criteria
     */
    Flux<TaskList> getUncompletedTaskLists(String author);

    /**
     * Returns task list with the given id and belonging to the given author.
     *
     * @param id     task list id
     * @param author task list author
     * @return requested task list
     * @throws EntityNotFoundException if task list is not found by id and author
     */
    Mono<TaskList> getTaskList(Long id, String author) throws EntityNotFoundException;

    /**
     * Creates new task list belonging to the given author.
     *
     * @param taskList task list to be created (must not be {@code null})
     * @param author task list author (must not be {@code null} or empty)
     * @return created task list
     */
    Mono<TaskList> createTaskList(TaskList taskList, String author);

    /**
     * Updates task list with the given id and belonging to the given author.
     *
     * @param taskList task list to be updated (must not be {@code null})
     * @param id task list id
     * @param author task list author
     * @return updated task list
     * @throws EntityNotFoundException if task list is not found by id and author
     */
    Mono<TaskList> updateTaskList(TaskList taskList, Long id, String author) throws EntityNotFoundException;

    /**
     * Completes task list with the given id and belonging to the given author along with all tasks included in it.
     *
     * @param id task list id
     * @param author task list author
     * @throws EntityNotFoundException if task list is not found by id and author
     */
    Mono<Void> completeTaskList(Long id, String author) throws EntityNotFoundException;

    /**
     * Deletes task list with the given id and belonging to the given author along with all tasks included in it.
     *
     * @param id task list id
     * @param author task list author
     * @throws EntityNotFoundException if task list is not found by id and author
     */
    Mono<Void> deleteTaskList(Long id, String author) throws EntityNotFoundException;

    /**
     * Returns tasks for task list with the given id and belonging to the given author.
     *
     * @param taskListId task list id
     * @param taskListAuthor task list author
     * @param pageable paging restriction
     * @return tasks from task list or empty stream when task list does not have any tasks
     * @throws EntityNotFoundException if task list is not found by id and author
     */
    Flux<Task> getTasks(Long taskListId, String taskListAuthor, Pageable pageable) throws EntityNotFoundException;
}

package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskList;
import org.briarheart.orchestra.model.User;
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
     * Returns uncompleted task lists belonging to the given user.
     *
     * @param user task list author (must not be {@code null})
     * @return found task lists or empty stream when there is no task list meeting the given criteria
     */
    Flux<TaskList> getUncompletedTaskLists(User user);

    /**
     * Returns task list with the given id and belonging to the given user.
     *
     * @param id   task list id
     * @param user task list author (must not be {@code null})
     * @return requested task list
     * @throws EntityNotFoundException if task list is not found by id or does not belong to the given user
     */
    Mono<TaskList> getTaskList(Long id, User user) throws EntityNotFoundException;

    /**
     * Creates new task list.
     *
     * @param taskList task list to be created (must not be {@code null})
     * @return created task list
     */
    Mono<TaskList> createTaskList(TaskList taskList);

    /**
     * Updates task list.
     *
     * @param taskList task list to be updated (must not be {@code null})
     * @return updated task list
     * @throws EntityNotFoundException if task list is not found
     */
    Mono<TaskList> updateTaskList(TaskList taskList) throws EntityNotFoundException;

    /**
     * Completes task list with the given id and belonging to the given user along with all tasks included in it.
     *
     * @param id   task list id
     * @param user task list author (must not be {@code null})
     * @throws EntityNotFoundException if task list is not found by id or does not belong to the given user
     */
    Mono<Void> completeTaskList(Long id, User user) throws EntityNotFoundException;

    /**
     * Deletes task list with the given id and belonging to the given user along with all tasks included in it.
     *
     * @param id   task list id
     * @param user task list author (must not be {@code null})
     * @throws EntityNotFoundException if task list is not found by id or does not belong to the given user
     */
    Mono<Void> deleteTaskList(Long id, User user) throws EntityNotFoundException;

    /**
     * Returns tasks for task list with the given id and belonging to the given user.
     *
     * @param taskListId task list id
     * @param user       task list author (must not be {@code null})
     * @param pageable   paging restriction
     * @return tasks from task list or empty stream when task list does not have any tasks
     * @throws EntityNotFoundException if task list is not found by id or does not belong to the given user
     */
    Flux<Task> getTasks(Long taskListId, User user, Pageable pageable) throws EntityNotFoundException;

    /**
     * Assigns task with the given id to task list with the given id.
     *
     * @param taskListId task list id
     * @param taskId     id of task to be assigned
     * @param user       task/task list author (must not be {@code null})
     * @throws EntityNotFoundException if task/task list is not found by id or does not belong to the given user
     */
    Mono<Void> addTask(Long taskListId, Long taskId, User user) throws EntityNotFoundException;

    /**
     * Removes task with the given id from task list with the given id.
     *
     * @param taskListId task list id
     * @param taskId     id of task to be removed
     * @param user       task/task list author (must not be {@code null})
     * @throws EntityNotFoundException if task/task list is not found by id or does not belong to the given user
     */
    Mono<Void> removeTask(Long taskListId, Long taskId, User user) throws EntityNotFoundException;
}

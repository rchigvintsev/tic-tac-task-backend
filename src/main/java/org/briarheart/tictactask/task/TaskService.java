package org.briarheart.tictactask.task;

import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.task.comment.TaskComment;
import org.briarheart.tictactask.task.tag.Tag;
import org.briarheart.tictactask.user.User;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Service for task managing.
 *
 * @author Roman Chigvintsev
 * @see Task
 */
public interface TaskService {
    /**
     * Returns number of all unprocessed tasks belonging to the given user.
     *
     * @param user task author (must not be {@code null})
     * @return number of unprocessed tasks or empty stream when there is no task meeting the given criteria
     */
    Mono<Long> getUnprocessedTaskCount(User user);

    /**
     * Returns all unprocessed tasks belonging to the given user.
     *
     * @param user     task author (must not be {@code null})
     * @param pageable paging restriction
     * @return unprocessed tasks or empty stream when there is no task meeting the given criteria
     */
    Flux<Task> getUnprocessedTasks(User user, Pageable pageable);

    /**
     * Returns number of all processed tasks belonging to the given user.
     *
     * @param user task author (must not be {@code null})
     * @return number of processed tasks or empty stream when there is no task meeting the given criteria
     */
    Mono<Long> getProcessedTaskCount(User user);

    /**
     * Returns all processed tasks belonging to the given user.
     *
     * @param user     task author (must not be {@code null})
     * @param pageable paging restriction
     * @return processed tasks or empty stream when there is no task meeting the given criteria
     */
    Flux<Task> getProcessedTasks(User user, Pageable pageable);

    /**
     * Returns number of processed tasks optionally falling within the given deadline bounds and belonging to the given
     * user. If deadline bounds are not specified this method returns number of processed tasks without deadline.
     *
     * @param deadlineFrom optional deadline from bound
     * @param deadlineTo   optional deadline to bound
     * @param user         task author (must not be {@code null})
     * @return number of processed tasks or empty stream when there is no task meeting the given criteria
     */
    Mono<Long> getProcessedTaskCount(LocalDateTime deadlineFrom, LocalDateTime deadlineTo, User user);

    /**
     * Returns processed tasks optionally falling within the given deadline bounds and belonging to the given user.
     * If deadline bounds are not specified this method returns processed tasks without deadline.
     *
     * @param deadlineFrom optional deadline from bound
     * @param deadlineTo   optional deadline to bound
     * @param user         task author (must not be {@code null})
     * @param pageable     paging restriction
     * @return processed tasks or empty stream when there is no task meeting the given criteria
     */
    Flux<Task> getProcessedTasks(LocalDateTime deadlineFrom,
                                 LocalDateTime deadlineTo,
                                 User user,
                                 Pageable pageable);

    /**
     * Returns number of all uncompleted tasks (either unprocessed or processed) belonging to the given user.
     *
     * @param user task author (must not be {@code null})
     * @return number of uncompleted tasks or empty stream when there is no task meeting the given criteria
     */
    Mono<Long> getUncompletedTaskCount(User user);

    /**
     * Returns uncompleted tasks (either unprocessed or processed) belonging to the given user.
     *
     * @param user     task author (must not be {@code null})
     * @param pageable paging restriction
     * @return uncompleted tasks or empty stream when there is no task meeting the given criteria
     */
    Flux<Task> getUncompletedTasks(User user, Pageable pageable);

    /**
     * Returns completed tasks belonging to the given user.
     *
     * @param user     task author (must not be {@code null})
     * @param pageable paging restriction
     * @return completed tasks or empty stream when there is no task meeting the given criteria
     */
    Flux<Task> getCompletedTasks(User user, Pageable pageable);

    /**
     * Returns task with the given id and belonging to the given user.
     *
     * @param id   task id
     * @param user task author (must not be {@code null})
     * @return requested task
     * @throws EntityNotFoundException if task is not found by id or does not belong to the given user
     */
    Mono<Task> getTask(Long id, User user) throws EntityNotFoundException;

    /**
     * Creates new task.
     *
     * @param task task to be created (must not be {@code null})
     * @return created task
     */
    Mono<Task> createTask(Task task);

    /**
     * Updates task.
     *
     * @param task task to be updated (must not be {@code null})
     * @return updated task
     * @throws EntityNotFoundException if task is not found
     */
    Mono<Task> updateTask(Task task) throws EntityNotFoundException;

    /**
     * Completes task with the given id and belonging to the given user.
     *
     * @param id   task id
     * @param user task author (must not be {@code null})
     * @throws EntityNotFoundException if task is not found by id or does not belong to the given user
     */
    Mono<Void> completeTask(Long id, User user) throws EntityNotFoundException;

    /**
     * Restores previously completed task with the given id and belonging to the given user.
     *
     * @param id task id
     * @param user task author (must not be {@code null})
     * @return restored task
     * @throws EntityNotFoundException if task is not found by id or does not belong to the given user
     */
    Mono<Task> restoreTask(Long id, User user) throws EntityNotFoundException;

    /**
     * Deletes task with the given id and belonging to the given user.
     *
     * @param id   task id
     * @param user task author (must not be {@code null})
     * @throws EntityNotFoundException if task is not found by id and author
     */
    Mono<Void> deleteTask(Long id, User user) throws EntityNotFoundException;

    /**
     * Returns tags for task with the given id and belonging to the given user.
     *
     * @param taskId task id
     * @param user   task author (must not be {@code null})
     * @return task tags or empty stream when task does not have any tag
     * @throws EntityNotFoundException if task is not found by id or does not belong to the given user
     */
    Flux<Tag> getTags(Long taskId, User user) throws EntityNotFoundException;

    /**
     * Assigns tag with the given id to task with the given id. Both task and tag must belong to the given user.
     *
     * @param taskId task id
     * @param tagId  id of tag to be assigned
     * @param user   task/tag author (must not be {@code null})
     * @throws EntityNotFoundException if task or tag is not found by id or does not belong to the given user
     */
    Mono<Void> assignTag(Long taskId, Long tagId, User user) throws EntityNotFoundException;

    /**
     * Removes tag with the given id from task with the given id and belonging to the given user.
     *
     * @param taskId task id
     * @param tagId  id of tag to be removed
     * @param user   task author (must not be {@code null})
     * @throws EntityNotFoundException if task is not found by id or does not belong to the given user
     */
    Mono<Void> removeTag(Long taskId, Long tagId, User user) throws EntityNotFoundException;

    /**
     * Returns comments for task with the given id and belonging to the given user.
     *
     * @param taskId   task id
     * @param user     task author (must not be {@code null})
     * @param pageable paging restriction
     * @return task comments or empty stream when task does not have any comment
     * @throws EntityNotFoundException if task is not found by id or does not belong to the given user
     */
    Flux<TaskComment> getComments(Long taskId, User user, Pageable pageable) throws EntityNotFoundException;

    /**
     * Adds new comment to task.
     *
     * @param comment new comment (must not be {@code null})
     * @return added comment
     * @throws EntityNotFoundException if task is not found
     */
    Mono<TaskComment> addComment(TaskComment comment) throws EntityNotFoundException;
}

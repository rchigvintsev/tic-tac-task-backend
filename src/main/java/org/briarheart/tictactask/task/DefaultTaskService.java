package org.briarheart.tictactask.task;

import lombok.extern.slf4j.Slf4j;
import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.task.comment.TaskComment;
import org.briarheart.tictactask.task.comment.TaskCommentRepository;
import org.briarheart.tictactask.task.list.TaskList;
import org.briarheart.tictactask.task.list.TaskListRepository;
import org.briarheart.tictactask.task.tag.TaskTag;
import org.briarheart.tictactask.task.tag.TaskTagRelation;
import org.briarheart.tictactask.task.tag.TaskTagRelationRepository;
import org.briarheart.tictactask.task.tag.TaskTagRepository;
import org.briarheart.tictactask.user.User;
import org.briarheart.tictactask.util.DateTimeUtils;
import org.briarheart.tictactask.util.Pageables;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link TaskService}.
 *
 * @author Roman Chigvintsev
 */
@Service
@Slf4j
public class DefaultTaskService implements TaskService {
    private final TaskRepository taskRepository;
    private final TaskTagRelationRepository taskTagRelationRepository;
    private final TaskTagRepository tagRepository;
    private final TaskListRepository taskListRepository;
    private final TaskCommentRepository taskCommentRepository;

    public DefaultTaskService(TaskRepository taskRepository,
                              TaskTagRelationRepository taskTagRelationRepository,
                              TaskTagRepository tagRepository,
                              TaskListRepository taskListRepository,
                              TaskCommentRepository taskCommentRepository) {
        Assert.notNull(taskRepository, "Task repository must not be null");
        Assert.notNull(taskTagRelationRepository, "Task-tag relation repository must not be null");
        Assert.notNull(tagRepository, "Tag repository must not be null");
        Assert.notNull(taskListRepository, "Task list repository must not be null");
        Assert.notNull(taskCommentRepository, "Task comment repository must not be null");

        this.taskRepository = taskRepository;
        this.taskTagRelationRepository = taskTagRelationRepository;
        this.tagRepository = tagRepository;
        this.taskListRepository = taskListRepository;
        this.taskCommentRepository = taskCommentRepository;
    }

    @Override
    public Mono<Long> getTaskCount(GetTasksRequest request, User user) {
        return taskRepository.count(request, user);
    }

    @Override
    public Flux<Task> getTasks(GetTasksRequest request, User user, Pageable pageable) {
        return taskRepository.find(request, user, pageable);
    }

    @Override
    public Mono<Task> getTask(Long id, User user) throws EntityNotFoundException {
        Assert.notNull(user, "User must not be null");
        return findTask(id, user.getId());
    }

    @Override
    public Mono<Task> createTask(Task task) {
        Assert.notNull(task, "Task must not be null");
        return Mono.defer(() -> taskRepository.save(copyTask(task))
                .doOnSuccess(t -> log.debug("Task with id {} is created", t.getId())));
    }

    @Transactional
    @Override
    public Mono<Task> updateTask(Task task) throws EntityNotFoundException {
        Assert.notNull(task, "Task must not be null");
        return findTask(task.getId(), task.getUserId()).flatMap(existingTask -> {
            Task updatedTask = new Task(task);
            updatedTask.setTaskListId(existingTask.getTaskListId());
            updatedTask.setPreviousStatus(existingTask.getPreviousStatus());
            updatedTask.setStatus(determineTaskStatus(task));
            if (updatedTask.getStatus() != existingTask.getStatus()) {
                updatedTask.setPreviousStatus(existingTask.getStatus());
            }
            updatedTask.setCreatedAt(existingTask.getCreatedAt());
            return taskRepository.save(updatedTask)
                    .doOnSuccess(t -> log.debug("Task with id {} is updated", t.getId()));
        });
    }

    @Transactional
    @Override
    public Mono<Task> completeTask(Long id, User user) throws EntityNotFoundException {
        return getTask(id, user)
                .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
                .flatMap(task -> {
                    task.setPreviousStatus(task.getStatus());
                    task.setStatus(TaskStatus.COMPLETED);
                    task.setCompletedAt(getCurrentTime());
                    return taskRepository.save(task)
                            .doOnSuccess(t -> log.debug("Task with id {} is completed", t.getId()));
                });
    }

    @Transactional
    @Override
    public Mono<Task> restoreTask(Long id, User user) throws EntityNotFoundException {
        return getTask(id, user)
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .flatMap(this::deleteChildTasks)
                .flatMap(this::restoreTaskList)
                .flatMap(task -> {
                    task.setStatus(task.getPreviousStatus());
                    task.setPreviousStatus(TaskStatus.COMPLETED);
                    return taskRepository.save(task)
                            .doOnSuccess(t -> log.debug("Task with id {} is restored", t.getId()));
                });
    }

    @Transactional
    @Override
    public Mono<Void> deleteTask(Long id, User user) throws EntityNotFoundException {
        return getTask(id, user)
                .flatMap(taskRepository::delete)
                .doOnSuccess(v -> log.debug("Task with id {} is deleted", id));
    }

    @Transactional
    @Override
    public Flux<TaskTag> getTags(Long taskId, User user) throws EntityNotFoundException {
        return getTask(taskId, user).flatMapMany(task -> {
            Mono<List<TaskTagRelation>> taskTagRelations = taskTagRelationRepository.findByTaskId(taskId).collectList();
            return taskTagRelations.flatMapMany(relationList -> {
                if (relationList.isEmpty()) {
                    return Flux.empty();
                }
                Set<Long> tagIds = relationList.stream()
                        .map(TaskTagRelation::getTagId)
                        .collect(Collectors.toSet());
                return tagRepository.findByIdIn(tagIds);
            });
        });
    }

    @Transactional
    @Override
    public Mono<Void> assignTag(Long taskId, Long tagId, User user) throws EntityNotFoundException {
        return getTask(taskId, user)
                .flatMap(task -> findTag(tagId, user.getId()))
                .flatMap(tag -> taskTagRelationRepository.findByTaskIdAndTagId(taskId, tagId))
                .switchIfEmpty(taskTagRelationRepository.create(taskId, tagId).doOnSuccess(relation
                        -> log.debug("Tag with id {} is assigned to task with id {}", tagId, taskId)))
                .then();
    }

    @Transactional
    @Override
    public Mono<Void> removeTag(Long taskId, Long tagId, User user) throws EntityNotFoundException {
        return getTask(taskId, user)
                .flatMap(task -> taskTagRelationRepository.deleteByTaskIdAndTagId(taskId, tagId))
                .doOnSuccess(v -> log.debug("Tag with id {} is removed from task with id {}", tagId, taskId));
    }

    @Transactional
    @Override
    public Flux<TaskComment> getComments(Long taskId, User user, Pageable pageable) {
        return getTask(taskId, user).flatMapMany(task -> {
            long offset = Pageables.getOffset(pageable);
            Integer limit = Pageables.getLimit(pageable);
            return taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(taskId, offset, limit);
        });
    }

    @Transactional
    @Override
    public Mono<TaskComment> addComment(TaskComment comment) throws EntityNotFoundException {
        Assert.notNull(comment, "Task comment must not be null");
        return findTask(comment.getTaskId(), comment.getUserId()).flatMap(task -> {
            TaskComment newComment = new TaskComment(comment);
            newComment.setId(null);
            newComment.setCreatedAt(getCurrentTime());
            newComment.setUpdatedAt(null);
            return taskCommentRepository.save(newComment).doOnSuccess(c
                    -> log.debug("Comment with id {} is added to task with id {}", c.getId(), c.getTaskId()));
        });
    }

    protected LocalDateTime getCurrentTime() {
        return DateTimeUtils.currentDateTimeUtc();
    }

    private Mono<Task> findTask(Long id, Long userId) {
        return taskRepository.findByIdAndUserId(id, userId)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Task with id " + id + " is not found")));
    }

    private Mono<TaskTag> findTag(Long id, Long userId) {
        return tagRepository.findByIdAndUserId(id, userId)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Tag with id " + id + " is not found")));
    }

    private Task copyTask(Task task) {
        Task copy = new Task(task);
        copy.setId(null);
        copy.setParentId(task.getParentId());
        copy.setPreviousStatus(null);
        copy.setStatus(determineTaskStatus(task));
        copy.setCreatedAt(getCurrentTime());
        copy.setCompletedAt(null);
        return copy;
    }

    private TaskStatus determineTaskStatus(Task task) {
        TaskStatus result = task.getStatus() == TaskStatus.PROCESSED ? TaskStatus.PROCESSED : TaskStatus.UNPROCESSED;
        if (result == TaskStatus.UNPROCESSED && (task.getDeadlineDate() != null || task.getDeadlineDateTime() != null)) {
            log.debug("Task with id {} is unprocessed and deadline is set. Changing task status to \"{}\".",
                    task.getId(), TaskStatus.PROCESSED);
            result = TaskStatus.PROCESSED;
        }
        return result;
    }

    private Mono<Task> restoreTaskList(Task task) {
        if (task.getTaskListId() != null) {
            return taskListRepository.findById(task.getTaskListId())
                    .filter(TaskList::isCompleted)
                    .flatMap(taskList -> {
                        taskList.setCompleted(false);
                        return taskListRepository.save(taskList);
                    })
                    .then(Mono.just(task));
        }
        return Mono.just(task);
    }

    private Mono<Task> deleteChildTasks(Task task) {
        return taskRepository.findByParentIdAndUserId(task.getId(), task.getUserId())
                .flatMap(taskRepository::delete)
                .then(Mono.just(task));
    }
}

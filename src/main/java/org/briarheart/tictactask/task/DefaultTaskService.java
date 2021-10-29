package org.briarheart.tictactask.task;

import lombok.extern.slf4j.Slf4j;
import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.task.comment.TaskComment;
import org.briarheart.tictactask.task.comment.TaskCommentRepository;
import org.briarheart.tictactask.task.list.TaskList;
import org.briarheart.tictactask.task.list.TaskListRepository;
import org.briarheart.tictactask.task.tag.Tag;
import org.briarheart.tictactask.task.tag.TagRepository;
import org.briarheart.tictactask.task.tag.TaskTagRelation;
import org.briarheart.tictactask.task.tag.TaskTagRelationRepository;
import org.briarheart.tictactask.user.User;
import org.briarheart.tictactask.util.Pageables;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private final TagRepository tagRepository;
    private final TaskListRepository taskListRepository;
    private final TaskCommentRepository taskCommentRepository;

    public DefaultTaskService(TaskRepository taskRepository,
                              TaskTagRelationRepository taskTagRelationRepository,
                              TagRepository tagRepository,
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
    public Mono<Long> getUnprocessedTaskCount(User user) {
        Assert.notNull(user, "User must not be null");
        return taskRepository.countAllByStatusAndUserId(TaskStatus.UNPROCESSED, user.getId());
    }

    @Override
    public Flux<Task> getUnprocessedTasks(User user, Pageable pageable) {
        Assert.notNull(user, "User must not be null");
        return taskRepository.findByStatusAndUserIdOrderByCreatedAtAsc(TaskStatus.UNPROCESSED, user.getId(),
                Pageables.getOffset(pageable), Pageables.getLimit(pageable));
    }

    @Override
    public Mono<Long> getProcessedTaskCount(User user) {
        Assert.notNull(user, "User must not be null");
        return taskRepository.countAllByStatusAndUserId(TaskStatus.PROCESSED, user.getId());
    }

    @Override
    public Flux<Task> getProcessedTasks(User user, Pageable pageable) {
        Assert.notNull(user, "User must not be null");
        return taskRepository.findByStatusAndUserIdOrderByCreatedAtAsc(TaskStatus.PROCESSED, user.getId(),
                Pageables.getOffset(pageable), Pageables.getLimit(pageable));
    }

    @Override
    public Mono<Long> getProcessedTaskCount(LocalDateTime deadlineFrom, LocalDateTime deadlineTo, User user) {
        Assert.notNull(user, "User must not be null");
        if (deadlineFrom == null && deadlineTo == null) {
            return taskRepository.countAllByDeadlineIsNullAndStatusAndUserId(TaskStatus.PROCESSED, user.getId());
        }
        if (deadlineFrom == null) {
            return taskRepository.countAllByDeadlineLessThanEqualAndStatusAndUserId(deadlineTo, TaskStatus.PROCESSED,
                    user.getId());
        }
        if (deadlineTo == null) {
            return taskRepository.countAllByDeadlineGreaterThanEqualAndStatusAndUserId(deadlineFrom,
                    TaskStatus.PROCESSED, user.getId());
        }
        return taskRepository.countAllByDeadlineBetweenAndStatusAndUserId(deadlineFrom, deadlineTo,
                TaskStatus.PROCESSED, user.getId());
    }

    @Override
    public Flux<Task> getProcessedTasks(
            LocalDateTime deadlineFrom,
            LocalDateTime deadlineTo,
            User user,
            Pageable pageable
    ) {
        Assert.notNull(user, "User must not be null");
        if (deadlineFrom == null && deadlineTo == null) {
            return taskRepository.findByDeadlineIsNullAndStatusAndUserIdOrderByCreatedAtAsc(TaskStatus.PROCESSED,
                    user.getId(), Pageables.getOffset(pageable), Pageables.getLimit(pageable));
        }
        if (deadlineFrom == null) {
            return taskRepository.findByDeadlineLessThanEqualAndStatusAndUserIdOrderByCreatedAtAsc(deadlineTo,
                    TaskStatus.PROCESSED, user.getId(), Pageables.getOffset(pageable), Pageables.getLimit(pageable));
        }
        if (deadlineTo == null) {
            return taskRepository.findByDeadlineGreaterThanEqualAndStatusAndUserIdOrderByCreatedAtAsc(deadlineFrom,
                    TaskStatus.PROCESSED, user.getId(), Pageables.getOffset(pageable), Pageables.getLimit(pageable));
        }
        return taskRepository.findByDeadlineBetweenAndStatusAndUserIdOrderByCreatedAtAsc(deadlineFrom, deadlineTo,
                TaskStatus.PROCESSED, user.getId(), Pageables.getOffset(pageable), Pageables.getLimit(pageable));
    }

    @Override
    public Mono<Long> getUncompletedTaskCount(User user) {
        Assert.notNull(user, "User must not be null");
        return taskRepository.countAllByStatusNotAndUserId(TaskStatus.COMPLETED, user.getId());
    }

    @Override
    public Flux<Task> getUncompletedTasks(User user, Pageable pageable) {
        Assert.notNull(user, "User must not be null");
        return taskRepository.findByStatusNotAndUserIdOrderByCreatedAtAsc(TaskStatus.COMPLETED, user.getId(),
                Pageables.getOffset(pageable), Pageables.getLimit(pageable));
    }

    @Override
    public Flux<Task> getCompletedTasks(User user, Pageable pageable) {
        Assert.notNull(user, "User must not be null");
        return taskRepository.findByStatusAndUserIdOrderByCreatedAtDesc(TaskStatus.COMPLETED, user.getId(),
                Pageables.getOffset(pageable), Pageables.getLimit(pageable));
    }

    @Override
    public Mono<Task> getTask(Long id, User user) throws EntityNotFoundException {
        Assert.notNull(user, "User must not be null");
        return findTask(id, user.getId());
    }

    @Override
    public Mono<Task> createTask(Task task) {
        Assert.notNull(task, "Task must not be null");
        return Mono.defer(() -> {
            Task newTask = new Task();
            newTask.setUserId(task.getUserId());
            newTask.setTitle(task.getTitle());
            newTask.setDescription(task.getDescription());
            newTask.setStatus(determineTaskStatus(task));
            newTask.setCreatedAt(getCurrentTime());
            newTask.setDeadline(task.getDeadline());
            newTask.setDeadlineTimeExplicitlySet(task.getDeadline() != null && task.isDeadlineTimeExplicitlySet());
            return taskRepository.save(newTask).doOnSuccess(t -> log.debug("Task with id {} is created", t.getId()));
        });
    }

    @Transactional
    @Override
    public Mono<Task> updateTask(Task task) throws EntityNotFoundException {
        Assert.notNull(task, "Task must not be null");
        return findTask(task.getId(), task.getUserId()).flatMap(existingTask -> {
            existingTask.setTitle(task.getTitle());
            existingTask.setDescription(task.getDescription());
            TaskStatus newStatus = determineTaskStatus(task);
            if (newStatus != existingTask.getStatus()) {
                existingTask.setPreviousStatus(existingTask.getStatus());
                existingTask.setStatus(newStatus);
            }
            existingTask.setDeadline(task.getDeadline());
            existingTask.setDeadlineTimeExplicitlySet(task.getDeadline() != null && task.isDeadlineTimeExplicitlySet());
            return taskRepository.save(existingTask)
                    .doOnSuccess(t -> log.debug("Task with id {} is updated", t.getId()));
        });
    }

    @Transactional
    @Override
    public Mono<Void> completeTask(Long id, User user) throws EntityNotFoundException {
        return getTask(id, user)
                .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
                .flatMap(task -> {
                    task.setPreviousStatus(task.getStatus());
                    task.setStatus(TaskStatus.COMPLETED);
                    return taskRepository.save(task)
                            .doOnSuccess(t -> log.debug("Task with id {} is completed", t.getId()));
                })
                .then();
    }

    @Transactional
    @Override
    public Mono<Task> restoreTask(Long id, User user) throws EntityNotFoundException {
        return getTask(id, user)
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
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
    public Flux<Tag> getTags(Long taskId, User user) throws EntityNotFoundException {
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
            TaskComment newComment = new TaskComment();
            newComment.setTaskId(comment.getTaskId());
            newComment.setUserId(comment.getUserId());
            newComment.setCommentText(comment.getCommentText());
            newComment.setCreatedAt(getCurrentTime());
            return taskCommentRepository.save(newComment).doOnSuccess(c
                    -> log.debug("Comment with id {} is added to task with id {}", c.getId(), c.getTaskId()));
        });
    }

    protected LocalDateTime getCurrentTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private Mono<Task> findTask(Long id, Long userId) {
        return taskRepository.findByIdAndUserId(id, userId)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Task with id " + id + " is not found")));
    }

    private Mono<Tag> findTag(Long id, Long userId) {
        return tagRepository.findByIdAndUserId(id, userId)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Tag with id " + id + " is not found")));
    }

    private TaskStatus determineTaskStatus(Task task) {
        TaskStatus result = task.getStatus() == TaskStatus.PROCESSED ? TaskStatus.PROCESSED : TaskStatus.UNPROCESSED;
        if (result == TaskStatus.UNPROCESSED && task.getDeadline() != null) {
            log.debug("Task with id {} is unprocessed and deadline is set. Changing task status to \"processed\".",
                    task.getId());
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
}

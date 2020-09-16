package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TagRepository;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.data.TaskTagRelationRepository;
import org.briarheart.orchestra.model.Tag;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskStatus;
import org.briarheart.orchestra.model.TaskTagRelation;
import org.briarheart.orchestra.util.Pageables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link TaskService}.
 *
 * @author Roman Chigvintsev
 */
@Service
public class DefaultTaskService implements TaskService {
    private static final Logger log = LoggerFactory.getLogger(DefaultTaskService.class);

    private final TaskRepository taskRepository;
    private final TagRepository tagRepository;
    private final TaskTagRelationRepository taskTagRelationRepository;

    @Autowired
    public DefaultTaskService(TaskRepository taskRepository,
                              TagRepository tagRepository,
                              TaskTagRelationRepository taskTagRelationRepository) {
        this.taskRepository = taskRepository;
        this.tagRepository = tagRepository;
        this.taskTagRelationRepository = taskTagRelationRepository;
    }

    @Override
    public Mono<Long> getUnprocessedTaskCount(String author) {
        return taskRepository.countAllByStatusAndAuthor(TaskStatus.UNPROCESSED, author);
    }

    @Override
    public Flux<Task> getUnprocessedTasks(String author, Pageable pageable) {
        return taskRepository.findByStatusAndAuthor(TaskStatus.UNPROCESSED, author, Pageables.getOffset(pageable),
                Pageables.getLimit(pageable));
    }

    @Override
    public Mono<Long> getProcessedTaskCount(String author) {
        return taskRepository.countAllByStatusAndAuthor(TaskStatus.PROCESSED, author);
    }

    @Override
    public Flux<Task> getProcessedTasks(String author, Pageable pageable) {
        return taskRepository.findByStatusAndAuthor(TaskStatus.PROCESSED, author, Pageables.getOffset(pageable),
                Pageables.getLimit(pageable));
    }

    @Override
    public Mono<Long> getProcessedTaskCount(LocalDate deadlineFrom, LocalDate deadlineTo, String author) {
        if (deadlineFrom == null && deadlineTo == null) {
            return taskRepository.countAllByDeadlineIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author);
        }
        if (deadlineFrom == null) {
            return taskRepository.countAllByDeadlineLessThanEqualAndStatusAndAuthor(deadlineTo, TaskStatus.PROCESSED,
                    author);
        }
        if (deadlineTo == null) {
            return taskRepository.countAllByDeadlineGreaterThanEqualAndStatusAndAuthor(deadlineFrom,
                    TaskStatus.PROCESSED, author);
        }
        return taskRepository.countAllByDeadlineBetweenAndStatusAndAuthor(deadlineFrom, deadlineTo,
                TaskStatus.PROCESSED, author);
    }

    @Override
    public Flux<Task> getProcessedTasks(
            LocalDate deadlineFrom,
            LocalDate deadlineTo,
            String author,
            Pageable pageable
    ) {
        if (deadlineFrom == null && deadlineTo == null) {
            return taskRepository.findByDeadlineIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author,
                    Pageables.getOffset(pageable), Pageables.getLimit(pageable));
        }
        if (deadlineFrom == null) {
            return taskRepository.findByDeadlineLessThanEqualAndStatusAndAuthor(deadlineTo, TaskStatus.PROCESSED,
                    author, Pageables.getOffset(pageable), Pageables.getLimit(pageable));
        }
        if (deadlineTo == null) {
            return taskRepository.findByDeadlineGreaterThanEqualAndStatusAndAuthor(deadlineFrom, TaskStatus.PROCESSED,
                    author, Pageables.getOffset(pageable), Pageables.getLimit(pageable));
        }
        return taskRepository.findByDeadlineBetweenAndStatusAndAuthor(deadlineFrom, deadlineTo, TaskStatus.PROCESSED,
                author, Pageables.getOffset(pageable), Pageables.getLimit(pageable));
    }

    @Override
    public Mono<Long> getUncompletedTaskCount(String author) {
        return taskRepository.countAllByStatusNotAndAuthor(TaskStatus.COMPLETED, author);
    }

    @Override
    public Flux<Task> getUncompletedTasks(String author, Pageable pageable) {
        return taskRepository.findByStatusNotAndAuthor(TaskStatus.COMPLETED, author, Pageables.getOffset(pageable),
                Pageables.getLimit(pageable));
    }

    @Override
    public Mono<Task> getTask(Long id, String author) throws EntityNotFoundException {
        return taskRepository.findByIdAndAuthor(id, author)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Task with id " + id + " is not found")))
                .flatMap(task -> getTags(task).map(tags -> {
                    task.setTags(tags);
                    return task;
                }));
    }

    @Override
    public Mono<Task> createTask(Task task, String author) {
        Assert.notNull(task, "Task must not be null");
        Assert.hasText(author, "Task author must not be null or empty");
        return Mono.defer(() -> {
            Task newTask = task.copy();
            newTask.setAuthor(author);
            if (newTask.getStatus() == null) {
                newTask.setStatus(TaskStatus.UNPROCESSED);
            }
            return taskRepository.save(newTask);
        });
    }

    @Override
    public Mono<Task> updateTask(Task task, Long id, String author) throws EntityNotFoundException {
        Assert.notNull(task, "Task must not be null");
        return getTask(id, author).flatMap(savedTask -> {
            List<Tag> tagsToRemove = findTagsToRemove(savedTask, task);
            return Flux.fromIterable(tagsToRemove)
                    .flatMap(tag -> removeTag(tag, savedTask))
                    .then(Flux.fromIterable(task.getTags())
                            .flatMap(tag -> assignTag(tag, savedTask).onErrorResume(e -> {
                                log.error("Failed to assign tag", e);
                                return Mono.empty();
                            }))
                            .then(Mono.defer(() -> updateTask(savedTask, task))));
        });
    }

    @Override
    public Mono<Void> completeTask(Long id, String author) throws EntityNotFoundException {
        return getTask(id, author).flatMap(task -> {
            task.setStatus(TaskStatus.COMPLETED);
            return taskRepository.save(task);
        }).then();
    }

    private Mono<List<Tag>> getTags(Task task) {
        return taskTagRelationRepository.findByTaskId(task.getId())
                .flatMap(taskTagRelation -> tagRepository.findById(taskTagRelation.getTagId()))
                .collectList();
    }

    private List<Tag> findTagsToRemove(Task oldTask, Task newTask) {
        return oldTask.getTags().stream()
                .filter(oldTag -> newTask.getTags().stream()
                        .noneMatch(newTag -> oldTag.getId().equals(newTag.getId())))
                .collect(Collectors.toList());
    }

    private Mono<Void> removeTag(Tag tag, Task task) {
        return taskTagRelationRepository.delete(task.getId(), tag.getId());
    }

    private Mono<TaskTagRelation> assignTag(Tag tag, Task task) {
        tag.setAuthor(task.getAuthor());
        Mono<Tag> tagMono;
        if (tag.getId() != null) {
            String errorMessage = "Tag with id " + tag.getId() + " is not found";
            tagMono = tagRepository.findByIdAndAuthor(tag.getId(), tag.getAuthor())
                    .switchIfEmpty(Mono.error(new EntityNotFoundException(errorMessage)));
        } else {
            tagMono = tagRepository.findByNameAndAuthor(tag.getName(), tag.getAuthor())
                    .switchIfEmpty(Mono.defer(() -> tagRepository.save(tag)));
        }
        return tagMono
                .flatMap(savedTag -> taskTagRelationRepository.findByTaskIdAndTagId(task.getId(), savedTag.getId()))
                .switchIfEmpty(Mono.defer(() -> taskTagRelationRepository.create(task.getId(), tag.getId())
                        .then(Mono.empty())));
    }

    private Mono<Task> updateTask(Task oldTask, Task newTask) {
        newTask.setId(oldTask.getId());
        newTask.setAuthor(oldTask.getAuthor());
        if (newTask.getStatus() == null) {
            newTask.setStatus(oldTask.getStatus());
        }
        if (newTask.getStatus() == TaskStatus.UNPROCESSED && newTask.getDeadline() != null) {
            newTask.setStatus(TaskStatus.PROCESSED);
        }
        return taskRepository.save(newTask);
    }
}

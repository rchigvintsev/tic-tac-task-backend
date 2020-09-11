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
    public Mono<Long> getProcessedTaskCount(LocalDate deadlineDateFrom, LocalDate deadlineDateTo, String author) {
        if (deadlineDateFrom == null && deadlineDateTo == null) {
            return taskRepository.countAllByDeadlineDateIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author);
        }
        if (deadlineDateFrom == null) {
            return taskRepository.countAllByDeadlineDateLessThanEqualAndStatusAndAuthor(deadlineDateTo,
                    TaskStatus.PROCESSED, author);
        }
        if (deadlineDateTo == null) {
            return taskRepository.countAllByDeadlineDateGreaterThanEqualAndStatusAndAuthor(deadlineDateFrom,
                    TaskStatus.PROCESSED, author);
        }
        return taskRepository.countAllByDeadlineDateBetweenAndStatusAndAuthor(deadlineDateFrom, deadlineDateTo,
                TaskStatus.PROCESSED, author);
    }

    @Override
    public Flux<Task> getProcessedTasks(
            LocalDate deadlineDateFrom,
            LocalDate deadlineDateTo,
            String author,
            Pageable pageable
    ) {
        if (deadlineDateFrom == null && deadlineDateTo == null) {
            return taskRepository.findByDeadlineDateIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author,
                    Pageables.getOffset(pageable), Pageables.getLimit(pageable));
        }
        if (deadlineDateFrom == null) {
            return taskRepository.findByDeadlineDateLessThanEqualAndStatusAndAuthor(deadlineDateTo,
                    TaskStatus.PROCESSED, author, Pageables.getOffset(pageable), Pageables.getLimit(pageable));
        }
        if (deadlineDateTo == null) {
            return taskRepository.findByDeadlineDateGreaterThanEqualAndStatusAndAuthor(deadlineDateFrom,
                    TaskStatus.PROCESSED, author, Pageables.getOffset(pageable), Pageables.getLimit(pageable));
        }
        return taskRepository.findByDeadlineDateBetweenAndStatusAndAuthor(deadlineDateFrom, deadlineDateTo,
                TaskStatus.PROCESSED, author, Pageables.getOffset(pageable), Pageables.getLimit(pageable));
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
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Task with id " + id + " is not found")));
    }

    @Override
    public Flux<Tag> getTaskTags(Long taskId, String author) throws EntityNotFoundException {
        return getTask(taskId, author).flatMapMany(t -> tagRepository.findForTaskId(taskId));
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
        return getTask(id, author).flatMap(t -> Flux.fromIterable(task.getTags())
                .flatMap(tag -> assignTag(tag, t))
                .onErrorContinue((throwable, value) -> log.error("Failed to assign tag", throwable))
                .then(Mono.defer(() -> {
                    task.setId(id);
                    task.setAuthor(author);
                    if (task.getStatus() == null) {
                        task.setStatus(t.getStatus());
                    }
                    if (task.getStatus() == TaskStatus.UNPROCESSED && task.getDeadlineDate() != null) {
                        task.setStatus(TaskStatus.PROCESSED);
                    }
                    return taskRepository.save(task);
                })));
    }

    @Override
    public Mono<Void> completeTask(Long id, String author) throws EntityNotFoundException {
        return getTask(id, author).flatMap(task -> {
            task.setStatus(TaskStatus.COMPLETED);
            return taskRepository.save(task);
        }).then();
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
}

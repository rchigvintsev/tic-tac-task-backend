package org.briarheart.tictactask.task.tag;

import lombok.extern.slf4j.Slf4j;
import org.briarheart.tictactask.data.EntityAlreadyExistsException;
import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.task.Task;
import org.briarheart.tictactask.task.TaskRepository;
import org.briarheart.tictactask.task.TaskStatus;
import org.briarheart.tictactask.user.User;
import org.briarheart.tictactask.util.Pageables;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link TaskTagService}.
 *
 * @author Roman Chigvintsev
 */
@Service
@Slf4j
public class DefaultTaskTagService implements TaskTagService {
    private final TagRepository tagRepository;
    private final TaskRepository taskRepository;

    public DefaultTaskTagService(TagRepository tagRepository, TaskRepository taskRepository) {
        Assert.notNull(tagRepository, "Tag repository must not be null");
        Assert.notNull(taskRepository, "Task repository must not be null");

        this.tagRepository = tagRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public Flux<TaskTag> getTags(User user) {
        Assert.notNull(user, "User must not be null");
        return tagRepository.findByUserId(user.getId());
    }

    @Override
    public Mono<TaskTag> getTag(Long id, User user) throws EntityNotFoundException {
        Assert.notNull(user, "User must not be null");
        return findTag(id, user.getId());
    }

    @Transactional
    @Override
    public Mono<TaskTag> createTag(TaskTag tag) throws EntityAlreadyExistsException {
        Assert.notNull(tag, "Tag must not be null");
        return tagRepository.findByNameAndUserId(tag.getName(), tag.getUserId())
                .flatMap(t -> {
                    String message = "Tag with name \"" + tag.getName() + "\" already exists";
                    return Mono.<TaskTag>error(new EntityAlreadyExistsException(message));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    TaskTag newTag = new TaskTag(tag);
                    newTag.setId(null);
                    return tagRepository.save(newTag)
                            .doOnSuccess(t -> log.debug("Tag with id {} is created", t.getId()));
                }));
    }

    @Transactional
    @Override
    public Mono<TaskTag> updateTag(TaskTag tag) throws EntityNotFoundException, EntityAlreadyExistsException {
        Assert.notNull(tag, "Tag must not be null");
        return findTag(tag.getId(), tag.getUserId())
                .flatMap(t -> {
                    if (!t.getName().equals(tag.getName())) {
                        // If tag name is changed try to find other tag with the same name in order
                        // to ensure that tag name is unique in particular user's space.
                        return tagRepository.findByNameAndUserId(tag.getName(), tag.getUserId());
                    }
                    return Mono.empty();
                }).flatMap(t -> {
                    String message = "Tag with name \"" + tag.getName() + "\" already exists";
                    return Mono.<TaskTag>error(new EntityAlreadyExistsException(message));
                }).switchIfEmpty(tagRepository.save(tag)
                        .doOnSuccess(t -> log.debug("Tag with id {} is updated", t.getId())));
    }

    @Transactional
    @Override
    public Mono<Void> deleteTag(Long id, User user) throws EntityNotFoundException {
        return getTag(id, user)
                .flatMap(tagRepository::delete)
                .doOnSuccess(v -> log.debug("Tag with id {} is deleted", id));
    }

    @Transactional
    @Override
    public Flux<Task> getUncompletedTasks(Long tagId, User user, Pageable pageable) {
        return getTag(tagId, user).flatMapMany(tag -> {
            long offset = Pageables.getOffset(pageable);
            Integer limit = Pageables.getLimit(pageable);
            return taskRepository.findByStatusNotAndTagIdOrderByCreatedAtAsc(TaskStatus.COMPLETED, tagId, offset, limit);
        });
    }

    private Mono<TaskTag> findTag(Long tagId, Long userId) throws EntityNotFoundException {
        return tagRepository.findByIdAndUserId(tagId, userId)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Tag with id " + tagId + " is not found")));
    }
}

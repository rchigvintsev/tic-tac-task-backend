package org.briarheart.orchestra.service;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TagRepository;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.model.Tag;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskStatus;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.util.Pageables;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link TagService}.
 *
 * @author Roman Chigvintsev
 */
@Service
@RequiredArgsConstructor
public class DefaultTagService implements TagService {
    private final TagRepository tagRepository;
    private final TaskRepository taskRepository;

    @Override
    public Flux<Tag> getTags(User user) {
        Assert.notNull(user, "User must not be null");
        return tagRepository.findByUserId(user.getId());
    }

    @Override
    public Mono<Tag> getTag(Long id, User user) throws EntityNotFoundException {
        Assert.notNull(user, "User must not be null");
        return findTag(id, user.getId());
    }

    @Override
    public Mono<Tag> createTag(Tag tag) throws EntityAlreadyExistsException {
        Assert.notNull(tag, "Tag must not be null");
        return tagRepository.findByNameAndUserId(tag.getName(), tag.getUserId())
                .flatMap(t -> {
                    String message = "Tag with name \"" + tag.getName() + "\" already exists";
                    return Mono.<Tag>error(new EntityAlreadyExistsException(message));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    Tag newTag = new Tag(tag);
                    newTag.setId(null);
                    return tagRepository.save(newTag);
                }));
    }

    @Override
    public Mono<Tag> updateTag(Tag tag) throws EntityNotFoundException, EntityAlreadyExistsException {
        Assert.notNull(tag, "Tag must not be null");
        return findTag(tag.getId(), tag.getUserId())
                .flatMap(t -> {
                    if (!t.getName().equals(tag.getName())) {
                        return tagRepository.findByNameAndUserId(tag.getName(), tag.getUserId());
                    }
                    return Mono.empty();
                }).flatMap(t -> {
                    String message = "Tag with name \"" + tag.getName() + "\" already exists";
                    return Mono.<Tag>error(new EntityAlreadyExistsException(message));
                }).switchIfEmpty(Mono.defer(() -> tagRepository.save(tag)));
    }

    @Override
    public Mono<Void> deleteTag(Long id, User user) throws EntityNotFoundException {
        return getTag(id, user).flatMap(tagRepository::delete);
    }

    @Override
    public Flux<Task> getUncompletedTasks(Long tagId, User user, Pageable pageable) {
        return getTag(tagId, user).flatMapMany(tag -> {
            long offset = Pageables.getOffset(pageable);
            Integer limit = Pageables.getLimit(pageable);
            return taskRepository.findByStatusNotAndTagId(TaskStatus.COMPLETED, tagId, offset, limit);
        });
    }

    private Mono<Tag> findTag(Long tagId, Long userId) throws EntityNotFoundException {
        return tagRepository.findByIdAndUserId(tagId, userId)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Tag with id " + tagId + " is not found")));
    }
}

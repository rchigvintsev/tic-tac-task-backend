package org.briarheart.orchestra.service;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TagRepository;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.model.Tag;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskStatus;
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
    public Flux<Tag> getTags(String author) {
        return tagRepository.findByAuthor(author);
    }

    @Override
    public Mono<Tag> getTag(Long id, String author) throws EntityNotFoundException {
        return tagRepository.findByIdAndAuthor(id, author)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Tag with id " + id + " is not found")));
    }

    @Override
    public Mono<Tag> createTag(Tag tag, String author) throws EntityAlreadyExistsException {
        Assert.notNull(tag, "Tag must not be null");
        Assert.hasText(author, "Tag author must not be null or empty");
        return tagRepository.findByNameAndAuthor(tag.getName(), author)
                .flatMap(t -> {
                    String message = "Tag with name \"" + tag.getName() + "\" already exists";
                    return Mono.<Tag>error(new EntityAlreadyExistsException(message));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    Tag newTag = tag.copy();
                    newTag.setAuthor(author);
                    return tagRepository.save(newTag);
                }));
    }

    @Override
    public Mono<Tag> updateTag(Tag tag, Long id, String author)
            throws EntityNotFoundException, EntityAlreadyExistsException {
        Assert.notNull(tag, "Tag must not be null");
        return getTag(id, author)
                .flatMap(t -> {
                    if (!t.getName().equals(tag.getName())) {
                        return tagRepository.findByNameAndAuthor(tag.getName(), author);
                    }
                    return Mono.empty();
                }).flatMap(t -> {
                    String message = "Tag with name \"" + tag.getName() + "\" already exists";
                    return Mono.<Tag>error(new EntityAlreadyExistsException(message));
                }).switchIfEmpty(Mono.defer(() -> {
                    tag.setId(id);
                    tag.setAuthor(author);
                    return tagRepository.save(tag);
                }));
    }

    @Override
    public Mono<Void> deleteTag(Long id, String author) throws EntityNotFoundException {
        return getTag(id, author).flatMap(tagRepository::delete);
    }

    @Override
    public Flux<Task> getUncompletedTasks(Long tagId, String tagAuthor, Pageable pageable) {
        return getTag(tagId, tagAuthor).flatMapMany(tag -> {
            long offset = Pageables.getOffset(pageable);
            Integer limit = Pageables.getLimit(pageable);
            return taskRepository.findByStatusNotAndTagId(TaskStatus.COMPLETED, tagId, offset, limit);
        });
    }
}

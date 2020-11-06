package org.briarheart.orchestra.service;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TagRepository;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.data.TaskTagRelationRepository;
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
    private final TaskTagRelationRepository taskTagRelationRepository;
    private final TaskRepository taskRepository;

    @Override
    public Flux<Tag> getTags(String author) {
        return tagRepository.findByAuthor(author);
    }

    @Override
    public Mono<Tag> getTag(Long id, String author) {
        return tagRepository.findByIdAndAuthor(id, author)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Tag with id " + id + " is not found")));
    }

    @Override
    public Flux<Task> getUncompletedTasks(Long tagId, String tagAuthor, Pageable pageable) {
        return getTag(tagId, tagAuthor).flatMapMany(tag -> {
            long offset = Pageables.getOffset(pageable);
            Integer limit = Pageables.getLimit(pageable);
            return taskRepository.findByStatusNotAndTagId(TaskStatus.COMPLETED, tagId, offset, limit);
        });
    }

    @Override
    public Mono<Tag> updateTag(Tag tag, Long id, String author) throws EntityNotFoundException {
        Assert.notNull(tag, "Tag must not be null");
        return getTag(id, author).flatMap(t -> {
            tag.setId(t.getId());
            tag.setAuthor(author);
            return tagRepository.save(tag);
        });
    }

    @Override
    public Mono<Void> deleteTag(Long id, String author) {
        return getTag(id, author).flatMap(tag -> taskTagRelationRepository.deleteByTagId(tag.getId())
                .then(tagRepository.deleteByIdAndAuthor(id, author)));
    }
}

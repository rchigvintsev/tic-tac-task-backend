package org.briarheart.orchestra.service;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TagRepository;
import org.briarheart.orchestra.model.Tag;
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

    @Override
    public Flux<Tag> getTags(String author) {
        return tagRepository.findByAuthor(author);
    }

    @Override
    public Mono<Tag> updateTag(Tag tag, Long id, String author) throws EntityNotFoundException {
        Assert.notNull(tag, "Tag must not be null");
        return tagRepository.findByIdAndAuthor(id, author)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Tag with id " + id + " is not found")))
                .flatMap(t -> {
                    tag.setId(t.getId());
                    tag.setAuthor(author);
                    return tagRepository.save(tag);
                });
    }

    @Override
    public Mono<Void> deleteTag(Long id, String author) {
        return tagRepository.deleteByIdAndAuthor(id, author);
    }
}

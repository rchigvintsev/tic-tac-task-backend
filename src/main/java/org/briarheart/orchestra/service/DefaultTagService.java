package org.briarheart.orchestra.service;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.TagRepository;
import org.briarheart.orchestra.model.Tag;
import org.briarheart.orchestra.util.Pageables;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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
    public Flux<Tag> getTags(String author, Pageable pageable) {
        return tagRepository.findByAuthor(author, Pageables.getOffset(pageable), Pageables.getLimit(pageable));
    }
}

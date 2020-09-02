package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultTagServiceTest {
    private TagRepository tagRepository;
    private DefaultTagService tagService;

    @BeforeEach
    void setUp() {
        tagRepository = mock(TagRepository.class);
        tagService = new DefaultTagService(tagRepository);
    }

    @Test
    void shouldReturnAllTags() {
        String author = "alice";
        when(tagRepository.findByAuthor(author, 0, null)).thenReturn(Flux.empty());
        tagService.getTags(author, null).blockFirst();
        verify(tagRepository, times(1)).findByAuthor(author, 0, null);
    }
}

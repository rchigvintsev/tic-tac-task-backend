package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TagRepository;
import org.briarheart.orchestra.model.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
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
        when(tagRepository.findByAuthor(author)).thenReturn(Flux.empty());
        tagService.getTags(author).blockFirst();
        verify(tagRepository, times(1)).findByAuthor(author);
    }

    @Test
    void shouldUpdateTag() {
        Tag tag = Tag.builder().id(1L).author("alice").name("Test tag").build();
        when(tagRepository.findByIdAndAuthor(tag.getId(), tag.getAuthor())).thenReturn(Mono.just(tag));
        when(tagRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Tag.class)));

        Tag updatedTag = Tag.builder().name("Updated test tag").build();
        Tag result = tagService.updateTag(updatedTag, tag.getId(), tag.getAuthor()).block();
        assertNotNull(result);
        assertEquals(updatedTag.getName(), result.getName());
    }

    @Test
    void shouldSetIdFieldOnTagUpdate() {
        Tag tag = Tag.builder().id(1L).author("alice").name("Test tag").build();
        when(tagRepository.findByIdAndAuthor(tag.getId(), tag.getAuthor())).thenReturn(Mono.just(tag));
        when(tagRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Tag.class)));

        Tag updatedTag = Tag.builder().name("Updated test tag").build();
        Tag result = tagService.updateTag(updatedTag, tag.getId(), tag.getAuthor()).block();
        assertNotNull(result);
        assertEquals(tag.getId(), result.getId());
    }

    @Test
    void shouldSetAuthorFieldOnTagUpdate() {
        Tag tag = Tag.builder().id(1L).author("alice").name("Test tag").build();
        when(tagRepository.findByIdAndAuthor(tag.getId(), tag.getAuthor())).thenReturn(Mono.just(tag));
        when(tagRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Tag.class)));

        Tag updatedTag = Tag.builder().name("Updated test tag").build();
        Tag result = tagService.updateTag(updatedTag, tag.getId(), tag.getAuthor()).block();
        assertNotNull(result);
        assertEquals(tag.getAuthor(), result.getAuthor());
    }

    @Test
    void shouldThrowExceptionOnTagUpdateWhenTagIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tagService.updateTag(null, null, null));
        assertEquals("Tag must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTagUpdateWhenTagIsNotFound() {
        when(tagRepository.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        long tagId = 1L;
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> tagService.updateTag(new Tag(), tagId, "alice").block());
        assertEquals("Tag with id " + tagId + " is not found", exception.getMessage());
    }

    @Test
    void shouldDeleteTag() {
        Long tagId = 1L;
        String tagAuthor = "alice";
        when(tagRepository.deleteByIdAndAuthor(tagId, tagAuthor)).thenReturn(Mono.empty());
        tagService.deleteTag(tagId, tagAuthor).block();
        verify(tagRepository, times(1)).deleteByIdAndAuthor(tagId, tagAuthor);
    }
}

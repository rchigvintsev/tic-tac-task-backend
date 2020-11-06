package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TagRepository;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.data.TaskTagRelationRepository;
import org.briarheart.orchestra.model.Tag;
import org.briarheart.orchestra.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultTagServiceTest {
    private TagRepository tagRepository;
    private TaskTagRelationRepository taskTagRelationRepository;
    private TaskRepository taskRepository;
    private DefaultTagService tagService;

    @BeforeEach
    void setUp() {
        tagRepository = mock(TagRepository.class);
        taskTagRelationRepository = mock(TaskTagRelationRepository.class);
        taskRepository = mock(TaskRepository.class);
        tagService = new DefaultTagService(tagRepository, taskTagRelationRepository, taskRepository);
    }

    @Test
    void shouldReturnAllTags() {
        String author = "alice";
        when(tagRepository.findByAuthor(author)).thenReturn(Flux.empty());
        tagService.getTags(author).blockFirst();
        verify(tagRepository, times(1)).findByAuthor(author);
    }

    @Test
    void shouldReturnTagById() {
        Tag tag = Tag.builder().id(1L).name("Test tag").author("alice").build();
        when(tagRepository.findByIdAndAuthor(tag.getId(), tag.getAuthor())).thenReturn(Mono.just(tag));

        Tag result = tagService.getTag(tag.getId(), tag.getAuthor()).block();
        assertNotNull(result);
        assertEquals(tag, result);
    }

    @Test
    void shouldThrowExceptionOnTagGetWhenTagIsNotFound() {
        when(tagRepository.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class, () -> tagService.getTag(1L, "alice").block());
    }

    @Test
    void shouldReturnAllUncompletedTasksForTag() {
        Tag tag = Tag.builder().id(1L).author("alice").name("Test tag").build();
        when(tagRepository.findByIdAndAuthor(tag.getId(), tag.getAuthor())).thenReturn(Mono.just(tag));
        when(taskRepository.findByStatusNotAndTagId(TaskStatus.COMPLETED, tag.getId(), 0, null))
                .thenReturn(Flux.empty());

        tagService.getUncompletedTasks(tag.getId(), tag.getAuthor(), Pageable.unpaged()).blockFirst();
        verify(taskRepository, times(1)).findByStatusNotAndTagId(TaskStatus.COMPLETED, tag.getId(), 0, null);
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
        Tag tag = Tag.builder().id(1L).name("Test tag").author("alice").build();

        when(tagRepository.findByIdAndAuthor(tag.getId(), tag.getAuthor())).thenReturn(Mono.just(tag));
        when(taskTagRelationRepository.deleteByTagId(tag.getId())).thenReturn(Mono.empty());
        when(tagRepository.deleteByIdAndAuthor(tag.getId(), tag.getAuthor())).thenReturn(Mono.empty());

        tagService.deleteTag(tag.getId(), tag.getAuthor()).block();
        verify(tagRepository, times(1)).deleteByIdAndAuthor(tag.getId(), tag.getAuthor());
    }

    @Test
    void shouldThrowExceptionOnTagDeleteWhenTagIsNotFound() {
        when(tagRepository.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        long tagId = 1L;
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> tagService.deleteTag(tagId, "alice").block());
        assertEquals("Tag with id " + tagId + " is not found", exception.getMessage());
    }
}

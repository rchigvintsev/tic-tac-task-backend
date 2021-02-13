package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TagRepository;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.model.Tag;
import org.briarheart.orchestra.model.TaskStatus;
import org.briarheart.orchestra.model.User;
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
    private TaskRepository taskRepository;
    private DefaultTagService tagService;

    @BeforeEach
    void setUp() {
        tagRepository = mock(TagRepository.class);
        when(tagRepository.findByNameAndUserId(anyString(), anyLong())).thenReturn(Mono.empty());
        when(tagRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Tag.class)));

        taskRepository = mock(TaskRepository.class);
        tagService = new DefaultTagService(tagRepository, taskRepository);
    }

    @Test
    void shouldReturnAllTags() {
        User user = User.builder().id(1L).build();
        when(tagRepository.findByUserId(user.getId())).thenReturn(Flux.empty());
        tagService.getTags(user).blockFirst();
        verify(tagRepository, times(1)).findByUserId(user.getId());
    }

    @Test
    void shouldReturnTagById() {
        User user = User.builder().id(1L).build();
        Tag tag = Tag.builder().id(2L).name("Test tag").userId(user.getId()).build();
        when(tagRepository.findByIdAndUserId(tag.getId(), tag.getUserId())).thenReturn(Mono.just(tag));

        Tag result = tagService.getTag(tag.getId(), user).block();
        assertNotNull(result);
        assertEquals(tag, result);
    }

    @Test
    void shouldThrowExceptionOnTagGetWhenTagIsNotFound() {
        when(tagRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        User user = User.builder().id(1L).build();
        assertThrows(EntityNotFoundException.class, () -> tagService.getTag(1L, user).block());
    }

    @Test
    void shouldCreateTag() {
        Tag tag = Tag.builder().name("New tag").build();
        Tag result = tagService.createTag(tag).block();
        assertNotNull(result);
        assertEquals(tag.getName(), result.getName());
        verify(tagRepository, times(1)).save(any());
    }

    @Test
    void shouldThrowExceptionOnTagCreateWhenTagIsNull() {
        assertThrows(IllegalArgumentException.class, () -> tagService.createTag(null), "Tag must not be null");
    }

    @Test
    void shouldThrowExceptionOnTagCreateWhenTagAlreadyExists() {
        Tag tag = Tag.builder().name("New tag").userId(1L).build();
        when(tagRepository.findByNameAndUserId(tag.getName(), tag.getUserId())).thenReturn(Mono.just(tag));
        assertThrows(EntityAlreadyExistsException.class, () -> tagService.createTag(tag).block(),
                "Tag with name \"" + tag.getName() + "\" already exists");
    }

    @Test
    void shouldUpdateTag() {
        Tag tag = Tag.builder().id(1L).userId(2L).name("Test tag").build();
        when(tagRepository.findByIdAndUserId(tag.getId(), tag.getUserId())).thenReturn(Mono.just(tag));

        Tag updatedTag = tag.copy();
        updatedTag.setId(tag.getId());
        updatedTag.setColor(16777215);

        Tag result = tagService.updateTag(updatedTag).block();
        assertNotNull(result);
        assertEquals(updatedTag.getColor(), result.getColor());
    }

    @Test
    void shouldThrowExceptionOnTagUpdateWhenTagIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tagService.updateTag(null));
        assertEquals("Tag must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTagUpdateWhenTagIsNotFound() {
        when(tagRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        Tag tag = Tag.builder().id(1L).build();
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> tagService.updateTag(tag).block());
        assertEquals("Tag with id " + tag.getId() + " is not found", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTagUpdateWhenTagAlreadyExists() {
        Tag tag = Tag.builder().id(1L).userId(2L).name("Test tag").build();
        Tag updatedTag = tag.copy();
        updatedTag.setId(tag.getId());
        updatedTag.setName("Updated test tag");

        when(tagRepository.findByIdAndUserId(tag.getId(), tag.getUserId())).thenReturn(Mono.just(tag));
        when(tagRepository.findByNameAndUserId(updatedTag.getName(), tag.getUserId()))
                .thenReturn(Mono.just(updatedTag));
        assertThrows(EntityAlreadyExistsException.class, () -> tagService.updateTag(updatedTag).block(),
                "Tag with name \"" + updatedTag.getName() + "\" already exists");
    }

    @Test
    void shouldDeleteTag() {
        User user = User.builder().id(1L).build();
        Tag tag = Tag.builder().id(2L).name("Test tag").userId(user.getId()).build();

        when(tagRepository.findByIdAndUserId(tag.getId(), user.getId())).thenReturn(Mono.just(tag));
        when(tagRepository.delete(tag)).thenReturn(Mono.empty());

        tagService.deleteTag(tag.getId(), user).block();
        verify(tagRepository, times(1)).delete(tag);
    }

    @Test
    void shouldThrowExceptionOnTagDeleteWhenTagIsNotFound() {
        when(tagRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        long tagId = 1L;
        User user = User.builder().id(1L).build();
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> tagService.deleteTag(tagId, user).block());
        assertEquals("Tag with id " + tagId + " is not found", exception.getMessage());
    }

    @Test
    void shouldReturnAllUncompletedTasksForTag() {
        User user = User.builder().id(1L).build();
        Tag tag = Tag.builder().id(2L).userId(user.getId()).name("Test tag").build();
        when(tagRepository.findByIdAndUserId(tag.getId(), user.getId())).thenReturn(Mono.just(tag));
        when(taskRepository.findByStatusNotAndTagId(TaskStatus.COMPLETED, tag.getId(), 0, null))
                .thenReturn(Flux.empty());

        tagService.getUncompletedTasks(tag.getId(), user, Pageable.unpaged()).blockFirst();
        verify(taskRepository, times(1)).findByStatusNotAndTagId(TaskStatus.COMPLETED, tag.getId(), 0, null);
    }
}

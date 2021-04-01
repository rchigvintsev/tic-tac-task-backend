package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TagRepository;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.model.Tag;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskStatus;
import org.briarheart.orchestra.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        taskRepository = mock(TaskRepository.class);
        tagService = new DefaultTagService(tagRepository, taskRepository);
    }

    @Test
    void shouldReturnAllTags() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Tag tag = Tag.builder().id(2L).userId(user.getId()).name("Test tag").build();
        when(tagRepository.findByUserId(user.getId())).thenReturn(Flux.just(tag));

        Tag result = tagService.getTags(user).blockFirst();
        assertEquals(tag, result);
    }

    @Test
    void shouldThrowExceptionOnTagsGetWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> tagService.getTags(null).blockFirst());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldReturnTagById() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Tag tag = Tag.builder().id(2L).userId(user.getId()).name("Test tag").build();
        when(tagRepository.findByIdAndUserId(tag.getId(), user.getId())).thenReturn(Mono.just(tag));

        Tag result = tagService.getTag(tag.getId(), user).block();
        assertEquals(tag, result);
    }

    @Test
    void shouldThrowExceptionOnTagGetWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> tagService.getTag(1L, null).block());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTagGetWhenTagIsNotFound() {
        when(tagRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        long tagId = 2L;
        User user = User.builder().id(1L).email("alice@mail.com").build();
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> tagService.getTag(tagId, user).block());
        assertEquals("Tag with id " + tagId + " is not found", e.getMessage());
    }

    @Test
    void shouldCreateTag() {
        long tagId = 2L;
        when(tagRepository.save(any())).thenAnswer(args -> {
            Tag t = args.getArgument(0);
            if (t.getId() == null) {
                t.setId(tagId);
            }
            return Mono.just(t);
        });

        Tag tag = Tag.builder().id(-1L).userId(1L).name("New tag").build();
        when(tagRepository.findByNameAndUserId(tag.getName(), tag.getUserId())).thenReturn(Mono.empty());

        Tag expectedResult = new Tag(tag);
        expectedResult.setId(tagId);

        Tag result = tagService.createTag(tag).block();
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldThrowExceptionOnTagCreateWhenTagIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> tagService.createTag(null));
        assertEquals("Tag must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTagCreateWhenTagAlreadyExists() {
        Tag tag = Tag.builder().name("New tag").userId(1L).build();
        when(tagRepository.findByNameAndUserId(tag.getName(), tag.getUserId())).thenReturn(Mono.just(tag));
        EntityAlreadyExistsException e = assertThrows(EntityAlreadyExistsException.class,
                () -> tagService.createTag(tag).block());
        assertEquals("Tag with name \"" + tag.getName() + "\" already exists", e.getMessage());
    }

    @Test
    void shouldUpdateTag() {
        Tag tag = Tag.builder().id(1L).userId(2L).name("Test tag").build();

        when(tagRepository.findByIdAndUserId(tag.getId(), tag.getUserId())).thenReturn(Mono.just(tag));
        when(tagRepository.findByNameAndUserId(tag.getName(), tag.getUserId())).thenReturn(Mono.empty());
        when(tagRepository.save(any())).thenAnswer(args -> Mono.just(args.getArgument(0)));

        Tag updatedTag = new Tag(tag);
        updatedTag.setColor(16777215);

        Tag result = tagService.updateTag(updatedTag).block();
        assertEquals(updatedTag, result);
    }

    @Test
    void shouldThrowExceptionOnTagUpdateWhenTagIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> tagService.updateTag(null));
        assertEquals("Tag must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTagUpdateWhenTagIsNotFound() {
        when(tagRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        Tag tag = Tag.builder().id(2L).userId(1L).name("Test tag").build();
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> tagService.updateTag(tag).block());
        assertEquals("Tag with id " + tag.getId() + " is not found", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTagUpdateWhenTagAlreadyExists() {
        Tag tag = Tag.builder().id(1L).userId(2L).name("Test tag").build();

        Tag updatedTag = new Tag(tag);
        updatedTag.setId(tag.getId());
        updatedTag.setName("Updated test tag");

        when(tagRepository.findByIdAndUserId(tag.getId(), tag.getUserId())).thenReturn(Mono.just(tag));
        when(tagRepository.findByNameAndUserId(updatedTag.getName(), updatedTag.getUserId()))
                .thenReturn(Mono.just(updatedTag));
        EntityAlreadyExistsException e = assertThrows(EntityAlreadyExistsException.class,
                () -> tagService.updateTag(updatedTag).block());
        assertEquals("Tag with name \"" + updatedTag.getName() + "\" already exists", e.getMessage());
    }

    @Test
    void shouldDeleteTag() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Tag tag = Tag.builder().id(2L).userId(user.getId()).name("Test tag").build();

        when(tagRepository.findByIdAndUserId(tag.getId(), user.getId())).thenReturn(Mono.just(tag));
        when(tagRepository.delete(tag)).thenReturn(Mono.just(true).then());

        tagService.deleteTag(tag.getId(), user).block();
        verify(tagRepository, times(1)).delete(tag);
    }

    @Test
    void shouldThrowExceptionOnTagDeleteWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> tagService.deleteTag(1L, null).block());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTagDeleteWhenTagIsNotFound() {
        when(tagRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());

        long tagId = 1L;
        User user = User.builder().id(1L).email("alice@mail.com").build();

        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> tagService.deleteTag(tagId, user).block());
        assertEquals("Tag with id " + tagId + " is not found", e.getMessage());
    }

    @Test
    void shouldReturnAllUncompletedTasksForTag() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Tag tag = Tag.builder().id(2L).userId(user.getId()).name("Test tag").build();
        Task task = Task.builder().id(3L).userId(user.getId()).title("Test task").status(TaskStatus.PROCESSED).build();

        when(tagRepository.findByIdAndUserId(tag.getId(), user.getId())).thenReturn(Mono.just(tag));
        when(taskRepository.findByStatusNotAndTagIdOrderByCreatedAtAsc(TaskStatus.COMPLETED, tag.getId(), 0, null))
                .thenReturn(Flux.just(task));

        Task result = tagService.getUncompletedTasks(tag.getId(), user, Pageable.unpaged()).blockFirst();
        assertEquals(task, result);
    }
}

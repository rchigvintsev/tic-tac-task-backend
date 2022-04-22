package org.briarheart.tictactask.task.tag;

import org.briarheart.tictactask.data.EntityAlreadyExistsException;
import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.task.Task;
import org.briarheart.tictactask.task.TaskRepository;
import org.briarheart.tictactask.task.TaskStatus;
import org.briarheart.tictactask.user.User;
import org.briarheart.tictactask.util.DateTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultTaskTagServiceTest {
    private TaskTagRepository tagRepository;
    private TaskRepository taskRepository;
    private DefaultTaskTagService tagService;

    @BeforeEach
    void setUp() {
        tagRepository = mock(TaskTagRepository.class);
        taskRepository = mock(TaskRepository.class);
        tagService = new DefaultTaskTagService(tagRepository, taskRepository);
    }

    @Test
    void shouldReturnAllTags() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        TaskTag tag = TaskTag.builder().id(2L).userId(user.getId()).name("Test tag").build();
        when(tagRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(Flux.just(tag));

        TaskTag result = tagService.getTags(user).blockFirst();
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
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        TaskTag tag = TaskTag.builder().id(2L).userId(user.getId()).name("Test tag").build();
        when(tagRepository.findByIdAndUserId(tag.getId(), user.getId())).thenReturn(Mono.just(tag));

        TaskTag result = tagService.getTag(tag.getId(), user).block();
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
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> tagService.getTag(tagId, user).block());
        assertEquals("Tag with id " + tagId + " is not found", e.getMessage());
    }

    @Test
    void shouldCreateTag() {
        long tagId = 2L;
        LocalDateTime now = DateTimeUtils.currentDateTimeUtc();
        when(tagRepository.save(any())).thenAnswer(args -> {
            TaskTag t = new TaskTag(args.getArgument(0));
            t.setId(tagId);
            t.setCreatedAt(now);
            return Mono.just(t);
        });

        TaskTag newTag = TaskTag.builder().userId(1L).name("New tag").build();

        TaskTag expectedResult = new TaskTag(newTag);
        expectedResult.setId(tagId);
        expectedResult.setCreatedAt(now);

        TaskTag result = tagService.createTag(newTag).block();
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldThrowExceptionOnTagCreateWhenTagIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> tagService.createTag(null));
        assertEquals("Tag must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTagCreateWhenTagAlreadyExists() {
        TaskTag tag = TaskTag.builder().name("New tag").userId(1L).build();
        when(tagRepository.save(any(TaskTag.class)))
                .thenReturn(Mono.error(new DataIntegrityViolationException("No way!")));
        EntityAlreadyExistsException e = assertThrows(EntityAlreadyExistsException.class,
                () -> tagService.createTag(tag).block());
        assertEquals("Tag with name \"" + tag.getName() + "\" already exists", e.getMessage());
    }

    @Test
    void shouldUpdateTag() {
        TaskTag tag = TaskTag.builder().id(1L).userId(2L).name("Test tag").build();

        when(tagRepository.findByIdAndUserId(tag.getId(), tag.getUserId())).thenReturn(Mono.just(tag));
        when(tagRepository.save(any())).thenAnswer(args -> Mono.just(new TaskTag(args.getArgument(0))));

        TaskTag updatedTag = new TaskTag(tag);
        updatedTag.setColor(16777215);

        TaskTag result = tagService.updateTag(updatedTag).block();
        assertEquals(updatedTag, result);
    }

    @Test
    void shouldThrowExceptionOnTagUpdateWhenTagIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> tagService.updateTag(null));
        assertEquals("Tag must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTagUpdateWhenTagIsNotFound() {
        when(tagRepository.save(any())).thenAnswer(args -> Mono.just(new TaskTag(args.getArgument(0))));
        when(tagRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());

        TaskTag tag = TaskTag.builder().id(2L).userId(1L).name("Test tag").build();
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> tagService.updateTag(tag).block());
        assertEquals("Tag with id " + tag.getId() + " is not found", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTagUpdateWhenTagAlreadyExists() {
        TaskTag tag = TaskTag.builder().id(1L).userId(2L).name("Test tag").build();

        TaskTag updatedTag = new TaskTag(tag);
        updatedTag.setId(tag.getId());
        updatedTag.setName("Updated test tag");

        when(tagRepository.save(any())).thenReturn(Mono.error(new DataIntegrityViolationException("No way!")));
        when(tagRepository.findByIdAndUserId(tag.getId(), tag.getUserId())).thenReturn(Mono.just(tag));

        EntityAlreadyExistsException e = assertThrows(EntityAlreadyExistsException.class,
                () -> tagService.updateTag(updatedTag).block());
        assertEquals("Tag with name \"" + updatedTag.getName() + "\" already exists", e.getMessage());
    }

    @Test
    void shouldDeleteTag() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        TaskTag tag = TaskTag.builder().id(2L).userId(user.getId()).name("Test tag").build();

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
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();

        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> tagService.deleteTag(tagId, user).block());
        assertEquals("Tag with id " + tagId + " is not found", e.getMessage());
    }

    @Test
    void shouldReturnAllUncompletedTasksForTag() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        TaskTag tag = TaskTag.builder().id(2L).userId(user.getId()).name("Test tag").build();
        Task task = Task.builder().id(3L).userId(user.getId()).title("Test task").status(TaskStatus.PROCESSED).build();

        when(tagRepository.findByIdAndUserId(tag.getId(), user.getId())).thenReturn(Mono.just(tag));
        when(taskRepository.findByStatusNotAndTagIdOrderByCreatedAtAsc(TaskStatus.COMPLETED, tag.getId(), 0, null))
                .thenReturn(Flux.just(task));

        Task result = tagService.getUncompletedTasks(tag.getId(), user, Pageable.unpaged()).blockFirst();
        assertEquals(task, result);
    }
}

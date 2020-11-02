package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TagRepository;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.data.TaskTagRelationRepository;
import org.briarheart.orchestra.model.Tag;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskStatus;
import org.briarheart.orchestra.model.TaskTagRelation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultTaskServiceTest {
    private TaskRepository taskRepositoryMock;
    private TagRepository tagRepositoryMock;
    private TaskTagRelationRepository taskTagRelationRepository;

    private DefaultTaskService taskService;

    @BeforeEach
    void setUp() {
        taskRepositoryMock = mock(TaskRepository.class);
        when(taskRepositoryMock.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Task.class)));

        tagRepositoryMock = mock(TagRepository.class);

        taskTagRelationRepository = mock(TaskTagRelationRepository.class);
        when(taskTagRelationRepository.create(any(), any())).thenReturn(Mono.empty().then());
        when(taskTagRelationRepository.findByTaskId(any())).thenReturn(Flux.empty());

        taskService = new DefaultTaskService(taskRepositoryMock, tagRepositoryMock, taskTagRelationRepository);
    }

    @Test
    void shouldReturnNumberOfAllUnprocessedTasks() {
        String author = "alice";
        when(taskRepositoryMock.countAllByStatusAndAuthor(TaskStatus.UNPROCESSED, author)).thenReturn(Mono.empty());
        taskService.getUnprocessedTaskCount(author).block();
        verify(taskRepositoryMock, times(1)).countAllByStatusAndAuthor(TaskStatus.UNPROCESSED, author);
    }

    @Test
    void shouldReturnAllUnprocessedTasks() {
        String author = "alice";
        when(taskRepositoryMock.findByStatusAndAuthor(TaskStatus.UNPROCESSED, author, 0, null))
                .thenReturn(Flux.empty());

        taskService.getUnprocessedTasks(author, Pageable.unpaged()).blockFirst();
        verify(taskRepositoryMock, times(1)).findByStatusAndAuthor(TaskStatus.UNPROCESSED, author, 0, null);
    }

    @Test
    void shouldReturnAllUnprocessedTasksWithPagingRestriction() {
        String author = "alice";
        PageRequest pageRequest = PageRequest.of(3, 50);

        when(taskRepositoryMock.findByStatusAndAuthor(TaskStatus.UNPROCESSED, author, pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.empty());

        taskService.getUnprocessedTasks(author, pageRequest).blockFirst();
        verify(taskRepositoryMock, times(1)).findByStatusAndAuthor(TaskStatus.UNPROCESSED, author,
                pageRequest.getOffset(), pageRequest.getPageSize());
    }

    @Test
    void shouldReturnNumberOfAllProcessedTasks() {
        String author = "alice";
        when(taskRepositoryMock.countAllByStatusAndAuthor(TaskStatus.PROCESSED, author)).thenReturn(Mono.empty());
        taskService.getProcessedTaskCount(author).block();
        verify(taskRepositoryMock, times(1)).countAllByStatusAndAuthor(TaskStatus.PROCESSED, author);
    }

    @Test
    void shouldReturnAllProcessedTasks() {
        String author = "alice";
        when(taskRepositoryMock.findByStatusAndAuthor(TaskStatus.PROCESSED, author, 0, null)).thenReturn(Flux.empty());

        taskService.getProcessedTasks(author, Pageable.unpaged()).blockFirst();
        verify(taskRepositoryMock, times(1)).findByStatusAndAuthor(TaskStatus.PROCESSED, author, 0, null);
    }

    @Test
    void shouldReturnAllProcessedTasksWithPagingRestriction() {
        String author = "alice";
        PageRequest pageRequest = PageRequest.of(3, 50);

        when(taskRepositoryMock.findByStatusAndAuthor(TaskStatus.PROCESSED, author, pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.empty());

        taskService.getProcessedTasks(author, pageRequest).blockFirst();
        verify(taskRepositoryMock, times(1)).findByStatusAndAuthor(TaskStatus.PROCESSED, author,
                pageRequest.getOffset(), pageRequest.getPageSize());
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateBetween() {
        LocalDateTime deadlineFrom = LocalDateTime.now();
        LocalDateTime deadlineTo = deadlineFrom.plus(1, ChronoUnit.DAYS);
        String author = "alice";

        when(taskRepositoryMock.countAllByDeadlineBetweenAndStatusAndAuthor(
                deadlineFrom,
                deadlineTo,
                TaskStatus.PROCESSED,
                author
        )).thenReturn(Mono.empty());

        taskService.getProcessedTaskCount(deadlineFrom, deadlineTo, author).block();
        verify(taskRepositoryMock, times(1)).countAllByDeadlineBetweenAndStatusAndAuthor(deadlineFrom,
                deadlineTo, TaskStatus.PROCESSED, author);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateBetween() {
        LocalDateTime deadlineFrom = LocalDateTime.now();
        LocalDateTime deadlineTo = deadlineFrom.plus(1, ChronoUnit.DAYS);
        String author = "alice";

        when(taskRepositoryMock.findByDeadlineBetweenAndStatusAndAuthor(
                deadlineFrom,
                deadlineTo,
                TaskStatus.PROCESSED,
                author,
                0,
                null
        )).thenReturn(Flux.empty());

        taskService.getProcessedTasks(deadlineFrom, deadlineTo, author, null).blockFirst();
        verify(taskRepositoryMock, times(1)).findByDeadlineBetweenAndStatusAndAuthor(deadlineFrom, deadlineTo,
                TaskStatus.PROCESSED, author, 0, null);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateLessThanOrEqual() {
        LocalDateTime deadlineTo = LocalDateTime.now().plus(1, ChronoUnit.DAYS);
        String author = "alice";

        when(taskRepositoryMock.countAllByDeadlineLessThanEqualAndStatusAndAuthor(
                deadlineTo,
                TaskStatus.PROCESSED,
                author
        )).thenReturn(Mono.empty());

        taskService.getProcessedTaskCount(null, deadlineTo, author).block();
        verify(taskRepositoryMock, times(1)).countAllByDeadlineLessThanEqualAndStatusAndAuthor(deadlineTo,
                TaskStatus.PROCESSED, author);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateLessThanOrEqual() {
        LocalDateTime deadlineTo = LocalDateTime.now().plus(1, ChronoUnit.DAYS);
        String author = "alice";

        when(taskRepositoryMock.findByDeadlineLessThanEqualAndStatusAndAuthor(
                deadlineTo,
                TaskStatus.PROCESSED,
                author,
                0,
                null
        )).thenReturn(Flux.empty());

        taskService.getProcessedTasks(null, deadlineTo, author, null).blockFirst();
        verify(taskRepositoryMock, times(1)).findByDeadlineLessThanEqualAndStatusAndAuthor(deadlineTo,
                TaskStatus.PROCESSED, author, 0, null);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateGreaterThanOrEqual() {
        LocalDateTime deadlineFrom = LocalDateTime.now();
        String author = "alice";

        when(taskRepositoryMock.countAllByDeadlineGreaterThanEqualAndStatusAndAuthor(
                deadlineFrom,
                TaskStatus.PROCESSED,
                author
        )).thenReturn(Mono.empty());

        taskService.getProcessedTaskCount(deadlineFrom, null, author).block();
        verify(taskRepositoryMock, times(1)).countAllByDeadlineGreaterThanEqualAndStatusAndAuthor(deadlineFrom,
                TaskStatus.PROCESSED, author);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateGreaterThanOrEqual() {
        LocalDateTime deadlineFrom = LocalDateTime.now();
        String author = "alice";

        when(taskRepositoryMock.findByDeadlineGreaterThanEqualAndStatusAndAuthor(
                deadlineFrom,
                TaskStatus.PROCESSED,
                author,
                0,
                null
        )).thenReturn(Flux.empty());

        taskService.getProcessedTasks(deadlineFrom, null, author, null).blockFirst();
        verify(taskRepositoryMock, times(1)).findByDeadlineGreaterThanEqualAndStatusAndAuthor(deadlineFrom,
                TaskStatus.PROCESSED, author, 0, null);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithoutDeadlineDate() {
        String author = "alice";
        when(taskRepositoryMock.countAllByDeadlineIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author))
                .thenReturn(Mono.empty());

        taskService.getProcessedTaskCount(null, null, author).block();
        verify(taskRepositoryMock, times(1)).countAllByDeadlineIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author);
    }

    @Test
    void shouldReturnProcessedTasksWithoutDeadlineDate() {
        String author = "alice";
        when(taskRepositoryMock.findByDeadlineIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author, 0, null))
                .thenReturn(Flux.empty());

        taskService.getProcessedTasks(null, null, author, null).blockFirst();
        verify(taskRepositoryMock, times(1)).findByDeadlineIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author,
                0, null);
    }

    @Test
    void shouldReturnNumberOfAllUncompletedTasks() {
        String author = "alice";
        when(taskRepositoryMock.countAllByStatusNotAndAuthor(TaskStatus.COMPLETED, author)).thenReturn(Mono.empty());

        taskService.getUncompletedTaskCount(author).block();
        verify(taskRepositoryMock, times(1)).countAllByStatusNotAndAuthor(TaskStatus.COMPLETED, author);
    }

    @Test
    void shouldReturnAllUncompletedTasks() {
        String author = "alice";
        when(taskRepositoryMock.findByStatusNotAndAuthor(TaskStatus.COMPLETED, author, 0, null))
                .thenReturn(Flux.empty());

        taskService.getUncompletedTasks(null, author, Pageable.unpaged()).blockFirst();
        verify(taskRepositoryMock, times(1)).findByStatusNotAndAuthor(TaskStatus.COMPLETED, author, 0, null);
    }

    @Test
    void shouldReturnAllUncompletedTasksWithPagingRestriction() {
        String author = "alice";
        PageRequest pageRequest = PageRequest.of(3, 50);

        when(taskRepositoryMock.findByStatusNotAndAuthor(TaskStatus.COMPLETED, author, pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.empty());

        taskService.getUncompletedTasks(null, author, pageRequest).blockFirst();
        verify(taskRepositoryMock, times(1)).findByStatusNotAndAuthor(TaskStatus.COMPLETED, author,
                pageRequest.getOffset(), pageRequest.getPageSize());
    }

    @Test
    void shouldReturnUncompletedTasksFilteredByTagId() {
        String author = "alice";
        Long tagId = 1L;

        when(taskRepositoryMock.findByStatusNotAndAuthorAndTagId(TaskStatus.COMPLETED, author, tagId, 0, null))
                .thenReturn(Flux.empty());

        taskService.getUncompletedTasks(tagId, author, Pageable.unpaged()).blockFirst();
        verify(taskRepositoryMock, times(1))
                .findByStatusNotAndAuthorAndTagId(TaskStatus.COMPLETED, author, tagId, 0, null);
    }

    @Test
    void shouldReturnTaskById() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        Task result = taskService.getTask(task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertEquals(task, result);
    }

    @Test
    void shouldReturnTaskTagsOnTaskGetById() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        Tag tag = Tag.builder().id(2L).name("Test tag").author(task.getAuthor()).build();

        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        when(taskTagRelationRepository.findByTaskId(task.getId()))
                .thenReturn(Flux.just(new TaskTagRelation(task.getId(), tag.getId())));
        when(tagRepositoryMock.findByIdIn(Set.of(tag.getId()))).thenReturn(Flux.just(tag));

        Task result = taskService.getTask(task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertEquals(List.of(tag), result.getTags());
    }

    @Test
    void shouldThrowExceptionOnTaskGetWhenTaskIsNotFound() {
        when(taskRepositoryMock.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class, () -> taskService.getTask(1L, "alice").block());
    }

    @Test
    void shouldCreateTask() {
        Task task = Task.builder().title("New task").build();
        Task result = taskService.createTask(task, "alice").block();
        assertNotNull(result);
        assertEquals(task.getTitle(), result.getTitle());
        verify(taskRepositoryMock, times(1)).save(any());
    }

    @Test
    void shouldSetAuthorFieldOnTaskCreate() {
        Task task = Task.builder().title("New task").build();
        String author = "alice";
        Task result = taskService.createTask(task, author).block();
        assertNotNull(result);
        assertEquals(author, result.getAuthor());
    }

    @Test
    void shouldSetTaskStatusToUnprocessedOnTaskCreate() {
        Task task = Task.builder().title("New task").status(null).build();
        Task result = taskService.createTask(task, "alice").block();
        assertNotNull(result);
        assertSame(TaskStatus.UNPROCESSED, result.getStatus());
    }

    @Test
    void shouldThrowExceptionOnTaskCreateWhenTaskIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(null, null));
        assertEquals("Task must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTaskCreateWhenAuthorIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(Task.builder().title("New task").build(), null));
        assertEquals("Task author must not be null or empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTaskCreateWhenAuthorIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(Task.builder().title("New task").build(), ""));
        assertEquals("Task author must not be null or empty", exception.getMessage());
    }

    @Test
    void shouldUpdateTask() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        Task updatedTask = Task.builder().title("Updated test task").build();
        Task result = taskService.updateTask(updatedTask, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertEquals(updatedTask.getTitle(), result.getTitle());
    }

    @Test
    void shouldSetIdFieldOnTaskUpdate() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        Task updatedTask = Task.builder().title("Updated test task").build();
        Task result = taskService.updateTask(updatedTask, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertEquals(task.getId(), result.getId());
    }

    @Test
    void shouldSetAuthorFieldOnTaskUpdate() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        Task updatedTask = Task.builder().title("Updated test task").build();
        Task result = taskService.updateTask(updatedTask, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertEquals(task.getAuthor(), result.getAuthor());
    }

    @Test
    void shouldSetStatusFieldOnTaskUpdate() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").status(TaskStatus.UNPROCESSED).build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        Task updatedTask = Task.builder().title("Updated test task").status(null).build();
        Task result = taskService.updateTask(updatedTask, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertSame(task.getStatus(), result.getStatus());
    }

    @Test
    void shouldMakeTaskProcessedOnTaskUpdateWhenDeadlineDateIsNotNull() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").status(TaskStatus.UNPROCESSED).build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        Task updatedTask = Task.builder()
                .title("Updated test task")
                .deadline(LocalDateTime.now().plus(3, ChronoUnit.DAYS))
                .build();
        Task result = taskService.updateTask(updatedTask, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertSame(TaskStatus.PROCESSED, result.getStatus());
    }

    @Test
    void shouldThrowExceptionOnTaskUpdateWhenTaskIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskService.updateTask(null, null, null));
        assertEquals("Task must not be null", exception.getMessage());
    }

    @Test
    void shouldAssignNewTagOnTaskUpdate() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        Task savedTask = task.copy();
        savedTask.setId(1L);
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(savedTask));

        Tag newTag = Tag.builder().name("Test tag").build();
        task.setTags(List.of(newTag));

        final long TAG_ID = 2L;
        when(tagRepositoryMock.findByNameAndAuthor(newTag.getName(), task.getAuthor())).thenReturn(Mono.empty());
        when(tagRepositoryMock.save(newTag)).thenAnswer(invocation -> {
            Tag tagToSave = invocation.getArgument(0);
            tagToSave.setId(TAG_ID);
            return Mono.just(tagToSave);
        });

        when(taskTagRelationRepository.findByTaskIdAndTagId(task.getId(), TAG_ID)).thenReturn(Mono.empty());
        when(taskTagRelationRepository.create(task.getId(), TAG_ID)).thenReturn(Mono.empty().then());

        Task result = taskService.updateTask(task, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        verify(taskTagRelationRepository, times(1)).create(task.getId(), TAG_ID);
    }

    @Test
    void shouldIgnoreAlreadyAssignedTagsOnTaskUpdate() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        Task savedTask = task.copy();
        savedTask.setId(1L);
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(savedTask));

        Tag tag = Tag.builder().id(2L).name("Test tag").build();
        task.setTags(List.of(tag));

        when(tagRepositoryMock.findByIdAndAuthor(tag.getId(), task.getAuthor())).thenReturn(Mono.just(tag));
        when(taskTagRelationRepository.findByTaskIdAndTagId(task.getId(), tag.getId()))
                .thenReturn(Mono.just(new TaskTagRelation(task.getId(), tag.getId())));

        Task result = taskService.updateTask(task, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        verify(taskTagRelationRepository, never()).create(task.getId(), tag.getId());
    }

    @Test
    void shouldIgnoreNotFoundTagsOnTaskUpdate() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        Task savedTask = task.copy();
        savedTask.setId(1L);
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(savedTask));

        Tag tag = Tag.builder().id(2L).name("Test tag").build();
        task.setTags(List.of(tag));
        when(tagRepositoryMock.findByIdAndAuthor(tag.getId(), task.getAuthor())).thenReturn(Mono.empty());

        Task result = taskService.updateTask(task, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        verify(taskTagRelationRepository, never()).create(task.getId(), tag.getId());
    }

    @Test
    void shouldRemoveTagsOnTaskUpdate() {
        Tag redTag = Tag.builder().id(2L).name("Red").build();
        Tag greenTag = Tag.builder().id(3L).name("Green").build();

        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        Task savedTask = task.copy();
        savedTask.setId(1L);
        task.setTags(List.of(greenTag));

        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(savedTask));
        when(tagRepositoryMock.findByIdIn(Set.of(redTag.getId(), greenTag.getId())))
                .thenReturn(Flux.just(redTag, greenTag));
        when(tagRepositoryMock.findByIdAndAuthor(greenTag.getId(), task.getAuthor())).thenReturn(Mono.just(greenTag));
        when(taskTagRelationRepository.findByTaskId(task.getId())).thenReturn(Flux.just(
                new TaskTagRelation(task.getId(), redTag.getId()),
                new TaskTagRelation(task.getId(), greenTag.getId())
        ));
        when(taskTagRelationRepository.deleteByTaskIdAndTagId(task.getId(), redTag.getId())).thenReturn(Mono.empty().then());

        Task result = taskService.updateTask(task, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        verify(taskTagRelationRepository, times(1)).deleteByTaskIdAndTagId(task.getId(), redTag.getId());
    }

    @Test
    void shouldCompleteTask() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        taskService.completeTask(task.getId(), task.getAuthor()).block();
        assertSame(TaskStatus.COMPLETED, task.getStatus());
    }
}

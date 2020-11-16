package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.*;
import org.briarheart.orchestra.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultTaskServiceTest {
    private TaskRepository taskRepository;
    private TaskTagRelationRepository taskTagRelationRepository;
    private TagRepository tagRepository;
    private TaskCommentRepository taskCommentRepository;

    private DefaultTaskService taskService;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        when(taskRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Task.class)));

        taskTagRelationRepository = mock(TaskTagRelationRepository.class);
        when(taskTagRelationRepository.findByTaskId(any())).thenReturn(Flux.empty());
        when(taskTagRelationRepository.create(anyLong(), anyLong())).thenAnswer(args -> {
            Long taskId = args.getArgument(0);
            Long tagId = args.getArgument(1);
            return Mono.just(new TaskTagRelation(taskId, tagId));
        });

        tagRepository = mock(TagRepository.class);

        taskCommentRepository = mock(TaskCommentRepository.class);

        taskService = new DefaultTaskService(taskRepository, taskTagRelationRepository, tagRepository,
                taskCommentRepository);
    }

    @Test
    void shouldReturnNumberOfAllUnprocessedTasks() {
        String author = "alice";
        when(taskRepository.countAllByStatusAndAuthor(TaskStatus.UNPROCESSED, author)).thenReturn(Mono.empty());
        taskService.getUnprocessedTaskCount(author).block();
        verify(taskRepository, times(1)).countAllByStatusAndAuthor(TaskStatus.UNPROCESSED, author);
    }

    @Test
    void shouldReturnAllUnprocessedTasks() {
        String author = "alice";
        when(taskRepository.findByStatusAndAuthor(TaskStatus.UNPROCESSED, author, 0, null))
                .thenReturn(Flux.empty());

        taskService.getUnprocessedTasks(author, Pageable.unpaged()).blockFirst();
        verify(taskRepository, times(1)).findByStatusAndAuthor(TaskStatus.UNPROCESSED, author, 0, null);
    }

    @Test
    void shouldReturnAllUnprocessedTasksWithPagingRestriction() {
        String author = "alice";
        PageRequest pageRequest = PageRequest.of(3, 50);

        when(taskRepository.findByStatusAndAuthor(TaskStatus.UNPROCESSED, author, pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.empty());

        taskService.getUnprocessedTasks(author, pageRequest).blockFirst();
        verify(taskRepository, times(1)).findByStatusAndAuthor(TaskStatus.UNPROCESSED, author,
                pageRequest.getOffset(), pageRequest.getPageSize());
    }

    @Test
    void shouldReturnNumberOfAllProcessedTasks() {
        String author = "alice";
        when(taskRepository.countAllByStatusAndAuthor(TaskStatus.PROCESSED, author)).thenReturn(Mono.empty());
        taskService.getProcessedTaskCount(author).block();
        verify(taskRepository, times(1)).countAllByStatusAndAuthor(TaskStatus.PROCESSED, author);
    }

    @Test
    void shouldReturnAllProcessedTasks() {
        String author = "alice";
        when(taskRepository.findByStatusAndAuthor(TaskStatus.PROCESSED, author, 0, null)).thenReturn(Flux.empty());

        taskService.getProcessedTasks(author, Pageable.unpaged()).blockFirst();
        verify(taskRepository, times(1)).findByStatusAndAuthor(TaskStatus.PROCESSED, author, 0, null);
    }

    @Test
    void shouldReturnAllProcessedTasksWithPagingRestriction() {
        String author = "alice";
        PageRequest pageRequest = PageRequest.of(3, 50);

        when(taskRepository.findByStatusAndAuthor(TaskStatus.PROCESSED, author, pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.empty());

        taskService.getProcessedTasks(author, pageRequest).blockFirst();
        verify(taskRepository, times(1)).findByStatusAndAuthor(TaskStatus.PROCESSED, author,
                pageRequest.getOffset(), pageRequest.getPageSize());
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateBetween() {
        LocalDateTime deadlineFrom = LocalDateTime.now();
        LocalDateTime deadlineTo = deadlineFrom.plus(1, ChronoUnit.DAYS);
        String author = "alice";

        when(taskRepository.countAllByDeadlineBetweenAndStatusAndAuthor(
                deadlineFrom,
                deadlineTo,
                TaskStatus.PROCESSED,
                author
        )).thenReturn(Mono.empty());

        taskService.getProcessedTaskCount(deadlineFrom, deadlineTo, author).block();
        verify(taskRepository, times(1)).countAllByDeadlineBetweenAndStatusAndAuthor(deadlineFrom,
                deadlineTo, TaskStatus.PROCESSED, author);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateBetween() {
        LocalDateTime deadlineFrom = LocalDateTime.now();
        LocalDateTime deadlineTo = deadlineFrom.plus(1, ChronoUnit.DAYS);
        String author = "alice";

        when(taskRepository.findByDeadlineBetweenAndStatusAndAuthor(
                deadlineFrom,
                deadlineTo,
                TaskStatus.PROCESSED,
                author,
                0,
                null
        )).thenReturn(Flux.empty());

        taskService.getProcessedTasks(deadlineFrom, deadlineTo, author, null).blockFirst();
        verify(taskRepository, times(1)).findByDeadlineBetweenAndStatusAndAuthor(deadlineFrom, deadlineTo,
                TaskStatus.PROCESSED, author, 0, null);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateLessThanOrEqual() {
        LocalDateTime deadlineTo = LocalDateTime.now().plus(1, ChronoUnit.DAYS);
        String author = "alice";

        when(taskRepository.countAllByDeadlineLessThanEqualAndStatusAndAuthor(
                deadlineTo,
                TaskStatus.PROCESSED,
                author
        )).thenReturn(Mono.empty());

        taskService.getProcessedTaskCount(null, deadlineTo, author).block();
        verify(taskRepository, times(1)).countAllByDeadlineLessThanEqualAndStatusAndAuthor(deadlineTo,
                TaskStatus.PROCESSED, author);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateLessThanOrEqual() {
        LocalDateTime deadlineTo = LocalDateTime.now().plus(1, ChronoUnit.DAYS);
        String author = "alice";

        when(taskRepository.findByDeadlineLessThanEqualAndStatusAndAuthor(
                deadlineTo,
                TaskStatus.PROCESSED,
                author,
                0,
                null
        )).thenReturn(Flux.empty());

        taskService.getProcessedTasks(null, deadlineTo, author, null).blockFirst();
        verify(taskRepository, times(1)).findByDeadlineLessThanEqualAndStatusAndAuthor(deadlineTo,
                TaskStatus.PROCESSED, author, 0, null);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateGreaterThanOrEqual() {
        LocalDateTime deadlineFrom = LocalDateTime.now();
        String author = "alice";

        when(taskRepository.countAllByDeadlineGreaterThanEqualAndStatusAndAuthor(
                deadlineFrom,
                TaskStatus.PROCESSED,
                author
        )).thenReturn(Mono.empty());

        taskService.getProcessedTaskCount(deadlineFrom, null, author).block();
        verify(taskRepository, times(1)).countAllByDeadlineGreaterThanEqualAndStatusAndAuthor(deadlineFrom,
                TaskStatus.PROCESSED, author);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateGreaterThanOrEqual() {
        LocalDateTime deadlineFrom = LocalDateTime.now();
        String author = "alice";

        when(taskRepository.findByDeadlineGreaterThanEqualAndStatusAndAuthor(
                deadlineFrom,
                TaskStatus.PROCESSED,
                author,
                0,
                null
        )).thenReturn(Flux.empty());

        taskService.getProcessedTasks(deadlineFrom, null, author, null).blockFirst();
        verify(taskRepository, times(1)).findByDeadlineGreaterThanEqualAndStatusAndAuthor(deadlineFrom,
                TaskStatus.PROCESSED, author, 0, null);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithoutDeadlineDate() {
        String author = "alice";
        when(taskRepository.countAllByDeadlineIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author))
                .thenReturn(Mono.empty());

        taskService.getProcessedTaskCount(null, null, author).block();
        verify(taskRepository, times(1)).countAllByDeadlineIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author);
    }

    @Test
    void shouldReturnProcessedTasksWithoutDeadlineDate() {
        String author = "alice";
        when(taskRepository.findByDeadlineIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author, 0, null))
                .thenReturn(Flux.empty());

        taskService.getProcessedTasks(null, null, author, null).blockFirst();
        verify(taskRepository, times(1)).findByDeadlineIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author,
                0, null);
    }

    @Test
    void shouldReturnNumberOfAllUncompletedTasks() {
        String author = "alice";
        when(taskRepository.countAllByStatusNotAndAuthor(TaskStatus.COMPLETED, author)).thenReturn(Mono.empty());

        taskService.getUncompletedTaskCount(author).block();
        verify(taskRepository, times(1)).countAllByStatusNotAndAuthor(TaskStatus.COMPLETED, author);
    }

    @Test
    void shouldReturnAllUncompletedTasks() {
        String author = "alice";
        when(taskRepository.findByStatusNotAndAuthor(TaskStatus.COMPLETED, author, 0, null))
                .thenReturn(Flux.empty());

        taskService.getUncompletedTasks(author, Pageable.unpaged()).blockFirst();
        verify(taskRepository, times(1)).findByStatusNotAndAuthor(TaskStatus.COMPLETED, author, 0, null);
    }

    @Test
    void shouldReturnUncompletedTasksWithPagingRestriction() {
        String author = "alice";
        PageRequest pageRequest = PageRequest.of(3, 50);

        when(taskRepository.findByStatusNotAndAuthor(TaskStatus.COMPLETED, author, pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.empty());

        taskService.getUncompletedTasks(author, pageRequest).blockFirst();
        verify(taskRepository, times(1)).findByStatusNotAndAuthor(TaskStatus.COMPLETED, author,
                pageRequest.getOffset(), pageRequest.getPageSize());
    }

    @Test
    void shouldReturnTaskById() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        when(taskRepository.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        Task result = taskService.getTask(task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertEquals(task, result);
    }

    @Test
    void shouldThrowExceptionOnTaskGetWhenTaskIsNotFound() {
        when(taskRepository.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class, () -> taskService.getTask(1L, "alice").block());
    }

    @Test
    void shouldCreateTask() {
        Task task = Task.builder().title("New task").build();
        Task result = taskService.createTask(task, "alice").block();
        assertNotNull(result);
        assertEquals(task.getTitle(), result.getTitle());
        verify(taskRepository, times(1)).save(any());
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
        when(taskRepository.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        Task updatedTask = Task.builder().title("Updated test task").build();
        Task result = taskService.updateTask(updatedTask, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertEquals(updatedTask.getTitle(), result.getTitle());
    }

    @Test
    void shouldSetIdFieldOnTaskUpdate() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        when(taskRepository.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        Task updatedTask = Task.builder().title("Updated test task").build();
        Task result = taskService.updateTask(updatedTask, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertEquals(task.getId(), result.getId());
    }

    @Test
    void shouldSetAuthorFieldOnTaskUpdate() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        when(taskRepository.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        Task updatedTask = Task.builder().title("Updated test task").build();
        Task result = taskService.updateTask(updatedTask, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertEquals(task.getAuthor(), result.getAuthor());
    }

    @Test
    void shouldSetStatusFieldOnTaskUpdate() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").status(TaskStatus.UNPROCESSED).build();
        when(taskRepository.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        Task updatedTask = Task.builder().title("Updated test task").status(null).build();
        Task result = taskService.updateTask(updatedTask, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertSame(task.getStatus(), result.getStatus());
    }

    @Test
    void shouldMarkTaskAsProcessedOnTaskUpdateWhenDeadlineDateIsNotNull() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").status(TaskStatus.UNPROCESSED).build();
        when(taskRepository.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

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
    void shouldCompleteTask() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        when(taskRepository.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        taskService.completeTask(task.getId(), task.getAuthor()).block();
        assertSame(TaskStatus.COMPLETED, task.getStatus());
    }

    @Test
    void shouldReturnAllTagsForTask() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        Tag tag = Tag.builder().id(2L).name("Test tag").author(task.getAuthor()).build();

        when(taskRepository.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        when(taskTagRelationRepository.findByTaskId(task.getId()))
                .thenReturn(Flux.just(new TaskTagRelation(task.getId(), tag.getId())));
        when(tagRepository.findByIdIn(Set.of(tag.getId()))).thenReturn(Flux.just(tag));

        taskService.getTags(task.getId(), task.getAuthor()).blockFirst();
        verify(tagRepository, times(1)).findByIdIn(Set.of(tag.getId()));
    }

    @Test
    void shouldThrowExceptionOnTagsGetWhenTaskIsNotFound() {
        when(taskRepository.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class, () -> taskService.getTags(1L, "alice").blockFirst());
    }

    @Test
    void shouldAssignTagToTask() {
        String author = "alice";
        Task task = Task.builder().id(1L).title("Test task").author(author).build();
        Tag tag = Tag.builder().id(2L).name("Test tag").author(author).build();

        when(taskRepository.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        when(tagRepository.findByIdAndAuthor(tag.getId(), tag.getAuthor())).thenReturn(Mono.just(tag));
        when(taskTagRelationRepository.findByTaskIdAndTagId(task.getId(), tag.getId())).thenReturn(Mono.empty());

        taskService.assignTag(task.getId(), tag.getId(), author).block();
        verify(taskTagRelationRepository, times(1)).create(task.getId(), tag.getId());
    }

    @Test
    void shouldThrowExceptionOnTagAssignWhenTaskIsNotFound() {
        Long taskId = 1L;
        String author = "alice";
        Tag tag = Tag.builder().id(2L).name("Test tag").author(author).build();

        when(taskRepository.findByIdAndAuthor(taskId, author)).thenReturn(Mono.empty());
        when(tagRepository.findByIdAndAuthor(tag.getId(), tag.getAuthor())).thenReturn(Mono.just(tag));

        assertThrows(EntityNotFoundException.class,
                () -> taskService.assignTag(taskId, tag.getId(), author).block(),
                "Task with id " + taskId + "is not found");
    }

    @Test
    void shouldThrowExceptionOnTagAssignWhenTagIsNotFound() {
        Long tagId = 2L;
        String author = "alice";
        Task task = Task.builder().id(1L).title("Test task").author(author).build();

        when(taskRepository.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        when(tagRepository.findByIdAndAuthor(tagId, author)).thenReturn(Mono.empty());

        assertThrows(EntityNotFoundException.class,
                () -> taskService.assignTag(task.getId(), tagId, author).block(),
                "Tag with id " + tagId + " is not found");
    }

    @Test
    void shouldRemoveTagFromTask() {
        String author = "alice";
        Task task = Task.builder().id(1L).title("Test task").author(author).build();
        Tag tag = Tag.builder().id(2L).name("Test tag").author(author).build();

        when(taskRepository.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        when(taskTagRelationRepository.deleteByTaskIdAndTagId(task.getId(), tag.getId()))
                .thenReturn(Mono.empty().then());

        taskService.removeTag(task.getId(), author, tag.getId()).block();
        verify(taskTagRelationRepository, times(1)).deleteByTaskIdAndTagId(task.getId(), tag.getId());
    }

    @Test
    void shouldThrowExceptionOnTagRemoveWhenTaskIsNotFound() {
        Long taskId = 1L;
        Long tagId = 2L;
        String author = "alice";
        when(taskRepository.findByIdAndAuthor(taskId, author)).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class, () -> taskService.removeTag(taskId, author, tagId).block(),
                "Task with id " + taskId + "is not found");
    }

    @Test
    void shouldReturnAllCommentsForTask() {
        Task task = Task.builder().id(1L).author("alice").title("Test task").build();
        when(taskRepository.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        when(taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(task.getId(), 0, null))
                .thenReturn(Flux.empty());

        taskService.getComments(task.getId(), task.getAuthor(), Pageable.unpaged()).blockFirst();
        verify(taskCommentRepository, times(1)).findByTaskIdOrderByCreatedAtDesc(task.getId(), 0, null);
    }

    @Test
    void shouldAddCommentToTask() {
        Task task = Task.builder().id(1L).author("alice").title("Test task").build();
        when(taskRepository.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        when(taskCommentRepository.save(any())).thenAnswer(invocation -> {
            TaskComment comment = invocation.getArgument(0, TaskComment.class);
            comment.setId(2L);
            return Mono.just(comment);
        });
        TaskComment newComment = TaskComment.builder().commentText("New comment").build();

        taskService.addComment(task.getId(), task.getAuthor(), newComment).block();
        verify(taskCommentRepository, times(1)).save(any());
    }

    @Test
    void shouldSetTaskIdFieldOnCommentAdd() {
        Task task = Task.builder().id(1L).author("alice").title("Test task").build();
        when(taskRepository.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        when(taskCommentRepository.save(any())).thenAnswer(invocation -> {
            TaskComment comment = invocation.getArgument(0, TaskComment.class);
            comment.setId(2L);
            return Mono.just(comment);
        });
        TaskComment newComment = TaskComment.builder().commentText("New comment").build();

        TaskComment result = taskService.addComment(task.getId(), task.getAuthor(), newComment).block();
        assertNotNull(result);
        assertEquals(task.getId(), result.getTaskId());
    }

    @Test
    void shouldSetAuthorFieldOnCommentAdd() {
        Task task = Task.builder().id(1L).author("alice").title("Test task").build();
        when(taskRepository.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        when(taskCommentRepository.save(any())).thenAnswer(invocation -> {
            TaskComment comment = invocation.getArgument(0, TaskComment.class);
            comment.setId(2L);
            return Mono.just(comment);
        });
        TaskComment newComment = TaskComment.builder().commentText("New comment").build();

        TaskComment result = taskService.addComment(task.getId(), task.getAuthor(), newComment).block();
        assertNotNull(result);
        assertEquals(task.getAuthor(), result.getAuthor());
    }

    @Test
    void shouldSetCreatedAtFieldOnCommentAdd() {
        Task task = Task.builder().id(1L).author("alice").title("Test task").build();
        when(taskRepository.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        when(taskCommentRepository.save(any())).thenAnswer(invocation -> {
            TaskComment comment = invocation.getArgument(0, TaskComment.class);
            comment.setId(2L);
            return Mono.just(comment);
        });
        TaskComment newComment = TaskComment.builder().commentText("New comment").build();

        TaskComment result = taskService.addComment(task.getId(), task.getAuthor(), newComment).block();
        assertNotNull(result);
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void shouldThrowExceptionOnCommentAddWhenCommentIsNull() {
        assertThrows(IllegalArgumentException.class, () -> taskService.addComment(1L, "alice", null),
                "Task comment must not be null");
    }

    @Test
    void shouldThrowExceptionOnCommentAddWhenTaskIsNotFound() {
        Long taskId = 1L;
        String author = "alice";
        TaskComment comment = TaskComment.builder().commentText("New comment").build();
        when(taskRepository.findByIdAndAuthor(taskId, author)).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class, () -> taskService.addComment(taskId, author, comment).block(),
                "Task with id " + taskId + "is not found");
    }

    @Test
    void shouldReturnCommentsForTaskWithPagingRestriction() {
        PageRequest pageRequest = PageRequest.of(3, 50);

        Task task = Task.builder().id(1L).author("alice").title("Test task").build();
        when(taskRepository.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        when(taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(task.getId(), pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.empty());

        taskService.getComments(task.getId(), task.getAuthor(), pageRequest).blockFirst();
        verify(taskCommentRepository, times(1)).findByTaskIdOrderByCreatedAtDesc(task.getId(),
                pageRequest.getOffset(), pageRequest.getPageSize());
    }

    @Test
    void shouldThrowExceptionOnCommentsGetWhenTaskIsNotFound() {
        when(taskRepository.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class,
                () -> taskService.getComments(1L, "alice", Pageable.unpaged()).blockFirst());
    }
}

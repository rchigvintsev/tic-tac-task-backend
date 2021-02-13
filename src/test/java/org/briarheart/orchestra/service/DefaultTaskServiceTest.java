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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
        User user = User.builder().id(1L).build();
        when(taskRepository.countAllByStatusAndUserId(TaskStatus.UNPROCESSED, user.getId())).thenReturn(Mono.empty());
        taskService.getUnprocessedTaskCount(user).block();
        verify(taskRepository, times(1)).countAllByStatusAndUserId(TaskStatus.UNPROCESSED, user.getId());
    }

    @Test
    void shouldReturnAllUnprocessedTasks() {
        User user = User.builder().id(1L).build();
        when(taskRepository.findByStatusAndUserId(TaskStatus.UNPROCESSED, user.getId(), 0, null))
                .thenReturn(Flux.empty());

        taskService.getUnprocessedTasks(user, Pageable.unpaged()).blockFirst();
        verify(taskRepository, times(1)).findByStatusAndUserId(TaskStatus.UNPROCESSED, user.getId(), 0, null);
    }

    @Test
    void shouldReturnAllUnprocessedTasksWithPagingRestriction() {
        User user = User.builder().id(1L).build();
        PageRequest pageRequest = PageRequest.of(3, 50);

        when(taskRepository.findByStatusAndUserId(TaskStatus.UNPROCESSED, user.getId(), pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.empty());

        taskService.getUnprocessedTasks(user, pageRequest).blockFirst();
        verify(taskRepository, times(1)).findByStatusAndUserId(TaskStatus.UNPROCESSED, user.getId(),
                pageRequest.getOffset(), pageRequest.getPageSize());
    }

    @Test
    void shouldReturnNumberOfAllProcessedTasks() {
        User user = User.builder().id(1L).build();
        when(taskRepository.countAllByStatusAndUserId(TaskStatus.PROCESSED, user.getId())).thenReturn(Mono.empty());
        taskService.getProcessedTaskCount(user).block();
        verify(taskRepository, times(1)).countAllByStatusAndUserId(TaskStatus.PROCESSED, user.getId());
    }

    @Test
    void shouldReturnAllProcessedTasks() {
        User user = User.builder().id(1L).build();
        when(taskRepository.findByStatusAndUserId(TaskStatus.PROCESSED, user.getId(), 0, null))
                .thenReturn(Flux.empty());

        taskService.getProcessedTasks(user, Pageable.unpaged()).blockFirst();
        verify(taskRepository, times(1)).findByStatusAndUserId(TaskStatus.PROCESSED, user.getId(), 0, null);
    }

    @Test
    void shouldReturnAllProcessedTasksWithPagingRestriction() {
        User user = User.builder().id(1L).build();
        PageRequest pageRequest = PageRequest.of(3, 50);

        when(taskRepository.findByStatusAndUserId(TaskStatus.PROCESSED, user.getId(), pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.empty());

        taskService.getProcessedTasks(user, pageRequest).blockFirst();
        verify(taskRepository, times(1)).findByStatusAndUserId(TaskStatus.PROCESSED, user.getId(),
                pageRequest.getOffset(), pageRequest.getPageSize());
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateBetween() {
        LocalDateTime deadlineFrom = LocalDateTime.now();
        LocalDateTime deadlineTo = deadlineFrom.plus(1, ChronoUnit.DAYS);
        User user = User.builder().id(1L).build();

        when(taskRepository.countAllByDeadlineBetweenAndStatusAndUserId(
                deadlineFrom,
                deadlineTo,
                TaskStatus.PROCESSED,
                user.getId()
        )).thenReturn(Mono.empty());

        taskService.getProcessedTaskCount(deadlineFrom, deadlineTo, user).block();
        verify(taskRepository, times(1)).countAllByDeadlineBetweenAndStatusAndUserId(deadlineFrom,
                deadlineTo, TaskStatus.PROCESSED, user.getId());
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateBetween() {
        LocalDateTime deadlineFrom = LocalDateTime.now();
        LocalDateTime deadlineTo = deadlineFrom.plus(1, ChronoUnit.DAYS);
        User user = User.builder().id(1L).build();

        when(taskRepository.findByDeadlineBetweenAndStatusAndUserId(
                deadlineFrom,
                deadlineTo,
                TaskStatus.PROCESSED,
                user.getId(),
                0,
                null
        )).thenReturn(Flux.empty());

        taskService.getProcessedTasks(deadlineFrom, deadlineTo, user, null).blockFirst();
        verify(taskRepository, times(1)).findByDeadlineBetweenAndStatusAndUserId(deadlineFrom, deadlineTo,
                TaskStatus.PROCESSED, user.getId(), 0, null);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateLessThanOrEqual() {
        LocalDateTime deadlineTo = LocalDateTime.now().plus(1, ChronoUnit.DAYS);
        User user = User.builder().id(1L).build();

        when(taskRepository.countAllByDeadlineLessThanEqualAndStatusAndUserId(
                deadlineTo,
                TaskStatus.PROCESSED,
                user.getId()
        )).thenReturn(Mono.empty());

        taskService.getProcessedTaskCount(null, deadlineTo, user).block();
        verify(taskRepository, times(1)).countAllByDeadlineLessThanEqualAndStatusAndUserId(deadlineTo,
                TaskStatus.PROCESSED, user.getId());
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateLessThanOrEqual() {
        LocalDateTime deadlineTo = LocalDateTime.now().plus(1, ChronoUnit.DAYS);
        User user = User.builder().id(1L).build();

        when(taskRepository.findByDeadlineLessThanEqualAndStatusAndUserId(
                deadlineTo,
                TaskStatus.PROCESSED,
                user.getId(),
                0,
                null
        )).thenReturn(Flux.empty());

        taskService.getProcessedTasks(null, deadlineTo, user, null).blockFirst();
        verify(taskRepository, times(1)).findByDeadlineLessThanEqualAndStatusAndUserId(deadlineTo,
                TaskStatus.PROCESSED, user.getId(), 0, null);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateGreaterThanOrEqual() {
        LocalDateTime deadlineFrom = LocalDateTime.now();
        User user = User.builder().id(1L).build();

        when(taskRepository.countAllByDeadlineGreaterThanEqualAndStatusAndUserId(
                deadlineFrom,
                TaskStatus.PROCESSED,
                user.getId()
        )).thenReturn(Mono.empty());

        taskService.getProcessedTaskCount(deadlineFrom, null, user).block();
        verify(taskRepository, times(1)).countAllByDeadlineGreaterThanEqualAndStatusAndUserId(deadlineFrom,
                TaskStatus.PROCESSED, user.getId());
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateGreaterThanOrEqual() {
        LocalDateTime deadlineFrom = LocalDateTime.now();
        User user = User.builder().id(1L).build();

        when(taskRepository.findByDeadlineGreaterThanEqualAndStatusAndUserId(
                deadlineFrom,
                TaskStatus.PROCESSED,
                user.getId(),
                0,
                null
        )).thenReturn(Flux.empty());

        taskService.getProcessedTasks(deadlineFrom, null, user, null).blockFirst();
        verify(taskRepository, times(1)).findByDeadlineGreaterThanEqualAndStatusAndUserId(deadlineFrom,
                TaskStatus.PROCESSED, user.getId(), 0, null);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithoutDeadlineDate() {
        User user = User.builder().id(1L).build();
        when(taskRepository.countAllByDeadlineIsNullAndStatusAndUserId(TaskStatus.PROCESSED, user.getId()))
                .thenReturn(Mono.empty());

        taskService.getProcessedTaskCount(null, null, user).block();
        verify(taskRepository, times(1)).countAllByDeadlineIsNullAndStatusAndUserId(TaskStatus.PROCESSED, user.getId());
    }

    @Test
    void shouldReturnProcessedTasksWithoutDeadlineDate() {
        User user = User.builder().id(1L).build();
        when(taskRepository.findByDeadlineIsNullAndStatusAndUserId(TaskStatus.PROCESSED, user.getId(), 0, null))
                .thenReturn(Flux.empty());

        taskService.getProcessedTasks(null, null, user, null).blockFirst();
        verify(taskRepository, times(1)).findByDeadlineIsNullAndStatusAndUserId(TaskStatus.PROCESSED, user.getId(),
                0, null);
    }

    @Test
    void shouldReturnNumberOfAllUncompletedTasks() {
        User user = User.builder().id(1L).build();
        when(taskRepository.countAllByStatusNotAndUserId(TaskStatus.COMPLETED, user.getId())).thenReturn(Mono.empty());

        taskService.getUncompletedTaskCount(user).block();
        verify(taskRepository, times(1)).countAllByStatusNotAndUserId(TaskStatus.COMPLETED, user.getId());
    }

    @Test
    void shouldReturnAllUncompletedTasks() {
        User user = User.builder().id(1L).build();
        when(taskRepository.findByStatusNotAndUserId(TaskStatus.COMPLETED, user.getId(), 0, null))
                .thenReturn(Flux.empty());

        taskService.getUncompletedTasks(user, Pageable.unpaged()).blockFirst();
        verify(taskRepository, times(1)).findByStatusNotAndUserId(TaskStatus.COMPLETED, user.getId(), 0, null);
    }

    @Test
    void shouldReturnUncompletedTasksWithPagingRestriction() {
        User user = User.builder().id(1L).build();
        PageRequest pageRequest = PageRequest.of(3, 50);

        when(taskRepository.findByStatusNotAndUserId(TaskStatus.COMPLETED, user.getId(), pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.empty());

        taskService.getUncompletedTasks(user, pageRequest).blockFirst();
        verify(taskRepository, times(1)).findByStatusNotAndUserId(TaskStatus.COMPLETED, user.getId(),
                pageRequest.getOffset(), pageRequest.getPageSize());
    }

    @Test
    void shouldReturnTaskById() {
        User user = User.builder().id(1L).build();
        Task task = Task.builder().id(2L).title("Test task").userId(user.getId()).build();
        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));

        Task result = taskService.getTask(task.getId(), user).block();
        assertNotNull(result);
        assertEquals(task, result);
    }

    @Test
    void shouldThrowExceptionOnTaskGetWhenTaskIsNotFound() {
        when(taskRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        User user = User.builder().id(1L).build();
        assertThrows(EntityNotFoundException.class, () -> taskService.getTask(2L, user).block());
    }

    @Test
    void shouldCreateTask() {
        Task task = Task.builder().title("New task").userId(1L).build();
        Task result = taskService.createTask(task).block();
        assertNotNull(result);
        assertEquals(task.getTitle(), result.getTitle());
        verify(taskRepository, times(1)).save(any());
    }

    @Test
    void shouldSetTaskStatusToUnprocessedOnTaskCreate() {
        Task task = Task.builder().title("New task").userId(1L).status(null).build();
        Task result = taskService.createTask(task).block();
        assertNotNull(result);
        assertSame(TaskStatus.UNPROCESSED, result.getStatus());
    }

    @Test
    void shouldThrowExceptionOnTaskCreateWhenTaskIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(null));
        assertEquals("Task must not be null", exception.getMessage());
    }

    @Test
    void shouldUpdateTask() {
        Task existingTask = Task.builder().id(1L).title("Test task").userId(2L).build();

        when(taskRepository.findByIdAndUserId(existingTask.getId(), existingTask.getUserId()))
                .thenReturn(Mono.just(existingTask));

        Task updatedTask = existingTask.copy();
        updatedTask.setId(existingTask.getId());
        updatedTask.setTitle("Updated test task");

        Task result = taskService.updateTask(updatedTask).block();
        assertNotNull(result);
        assertEquals(updatedTask.getTitle(), result.getTitle());
    }

    @Test
    void shouldSetStatusFieldOnTaskUpdate() {
        Task existingTask = Task.builder().id(1L).title("Test task").userId(2L).status(TaskStatus.UNPROCESSED).build();
        when(taskRepository.findByIdAndUserId(existingTask.getId(), existingTask.getUserId()))
                .thenReturn(Mono.just(existingTask));

        Task updatedTask = existingTask.copy();
        updatedTask.setId(existingTask.getId());
        updatedTask.setTitle("Updated test task");
        updatedTask.setStatus(null);

        Task result = taskService.updateTask(updatedTask).block();
        assertNotNull(result);
        assertSame(existingTask.getStatus(), result.getStatus());
    }

    @Test
    void shouldMarkTaskAsProcessedOnTaskUpdateWhenDeadlineDateIsNotNull() {
        Task existingTask = Task.builder().id(1L).title("Test task").userId(2L).status(TaskStatus.UNPROCESSED).build();
        when(taskRepository.findByIdAndUserId(existingTask.getId(), existingTask.getUserId()))
                .thenReturn(Mono.just(existingTask));

        Task updatedTask = existingTask.copy();
        updatedTask.setId(existingTask.getId());
        updatedTask.setTitle("Updated test task");
        updatedTask.setDeadline(LocalDateTime.now().plus(3, ChronoUnit.DAYS));

        Task result = taskService.updateTask(updatedTask).block();
        assertNotNull(result);
        assertSame(TaskStatus.PROCESSED, result.getStatus());
    }

    @Test
    void shouldThrowExceptionOnTaskUpdateWhenTaskIsNull() {
        assertThrows(IllegalArgumentException.class, () -> taskService.updateTask(null), "Task must not be null");
    }

    @Test
    void shouldThrowExceptionOnTaskUpdateWhenTaskIsNotFound() {
        Task task = Task.builder().id(1L).title("Test task").userId(2L).build();
        when(taskRepository.findByIdAndUserId(task.getId(), task.getUserId())).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class, () -> taskService.updateTask(task).block(),
                "Task with id " + task.getId() + " is not found");
    }

    @Test
    void shouldCompleteTask() {
        User user = User.builder().id(1L).build();
        Task task = Task.builder().id(2L).title("Test task").userId(user.getId()).build();

        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));

        taskService.completeTask(task.getId(), user).block();
        assertSame(TaskStatus.COMPLETED, task.getStatus());
    }

    @Test
    void shouldThrowExceptionOnTaskCompleteWhenTaskIsNotFound() {
        User user = User.builder().id(1L).build();
        long taskId = 2L;

        when(taskRepository.findByIdAndUserId(taskId, user.getId())).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class, () -> taskService.completeTask(taskId, user).block(),
                "Task with id " + taskId + " is not found");
    }

    @Test
    void shouldDeleteTask() {
        User user = User.builder().id(1L).build();
        Task task = Task.builder().id(2L).title("Test task").userId(user.getId()).build();

        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(taskRepository.delete(task)).thenReturn(Mono.empty());

        taskService.deleteTask(task.getId(), user).block();
        verify(taskRepository, times(1)).delete(task);
    }

    @Test
    void shouldThrowExceptionOnTaskDeleteWhenTaskIsNotFound() {
        User user = User.builder().id(1L).build();
        long taskId = 2L;

        when(taskRepository.findByIdAndUserId(taskId, user.getId())).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class, () -> taskService.deleteTask(taskId, user).block(),
                "Task with id " + taskId + " is not found");
    }

    @Test
    void shouldReturnAllTagsForTask() {
        User user = User.builder().id(1L).build();
        Task task = Task.builder().id(2L).title("Test task").userId(user.getId()).build();
        Tag tag = Tag.builder().id(3L).name("Test tag").userId(user.getId()).build();

        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(taskTagRelationRepository.findByTaskId(task.getId()))
                .thenReturn(Flux.just(new TaskTagRelation(task.getId(), tag.getId())));
        when(tagRepository.findByIdIn(Set.of(tag.getId()))).thenReturn(Flux.just(tag));

        taskService.getTags(task.getId(), user).blockFirst();
        verify(tagRepository, times(1)).findByIdIn(Set.of(tag.getId()));
    }

    @Test
    void shouldThrowExceptionOnTagsGetWhenTaskIsNotFound() {
        when(taskRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        User user = User.builder().id(1L).build();
        assertThrows(EntityNotFoundException.class, () -> taskService.getTags(2L, user).blockFirst());
    }

    @Test
    void shouldAssignTagToTask() {
        User user = User.builder().id(1L).build();
        Task task = Task.builder().id(2L).title("Test task").userId(user.getId()).build();
        Tag tag = Tag.builder().id(3L).name("Test tag").userId(user.getId()).build();

        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(tagRepository.findByIdAndUserId(tag.getId(), user.getId())).thenReturn(Mono.just(tag));
        when(taskTagRelationRepository.findByTaskIdAndTagId(task.getId(), tag.getId())).thenReturn(Mono.empty());

        taskService.assignTag(task.getId(), tag.getId(), user).block();
        verify(taskTagRelationRepository, times(1)).create(task.getId(), tag.getId());
    }

    @Test
    void shouldThrowExceptionOnTagAssignWhenTaskIsNotFound() {
        User user = User.builder().id(1L).build();
        Tag tag = Tag.builder().id(2L).name("Test tag").userId(user.getId()).build();
        long taskId = 3L;

        when(taskRepository.findByIdAndUserId(taskId, user.getId())).thenReturn(Mono.empty());
        when(tagRepository.findByIdAndUserId(tag.getId(), user.getId())).thenReturn(Mono.just(tag));

        assertThrows(EntityNotFoundException.class, () -> taskService.assignTag(taskId, tag.getId(), user).block(),
                "Task with id " + taskId + "is not found");
    }

    @Test
    void shouldThrowExceptionOnTagAssignWhenTagIsNotFound() {
        User user = User.builder().id(1L).build();
        Task task = Task.builder().id(2L).title("Test task").userId(user.getId()).build();
        Long tagId = 3L;

        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(tagRepository.findByIdAndUserId(tagId, user.getId())).thenReturn(Mono.empty());

        assertThrows(EntityNotFoundException.class, () -> taskService.assignTag(task.getId(), tagId, user).block(),
                "Tag with id " + tagId + " is not found");
    }

    @Test
    void shouldRemoveTagFromTask() {
        User user = User.builder().id(1L).build();
        Task task = Task.builder().id(2L).title("Test task").userId(user.getId()).build();
        Tag tag = Tag.builder().id(3L).name("Test tag").userId(user.getId()).build();

        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(taskTagRelationRepository.deleteByTaskIdAndTagId(task.getId(), tag.getId()))
                .thenReturn(Mono.empty().then());

        taskService.removeTag(task.getId(), tag.getId(), user).block();
        verify(taskTagRelationRepository, times(1)).deleteByTaskIdAndTagId(task.getId(), tag.getId());
    }

    @Test
    void shouldThrowExceptionOnTagRemoveWhenTaskIsNotFound() {
        User user = User.builder().id(1L).build();
        Long taskId = 2L;
        Long tagId = 3L;

        when(taskRepository.findByIdAndUserId(taskId, user.getId())).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class, () -> taskService.removeTag(taskId, tagId, user).block(),
                "Task with id " + taskId + "is not found");
    }

    @Test
    void shouldReturnAllCommentsForTask() {
        User user = User.builder().id(1L).build();
        Task task = Task.builder().id(2L).userId(user.getId()).title("Test task").build();

        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(task.getId(), 0, null))
                .thenReturn(Flux.empty());

        taskService.getComments(task.getId(), user, Pageable.unpaged()).blockFirst();
        verify(taskCommentRepository, times(1)).findByTaskIdOrderByCreatedAtDesc(task.getId(), 0, null);
    }

    @Test
    void shouldAddCommentToTask() {
        long userId = 1L;
        Task task = Task.builder().id(2L).userId(userId).title("Test task").build();

        when(taskRepository.findByIdAndUserId(task.getId(), userId)).thenReturn(Mono.just(task));
        when(taskCommentRepository.save(any())).thenAnswer(invocation -> {
            TaskComment comment = invocation.getArgument(0, TaskComment.class);
            comment.setId(3L);
            return Mono.just(comment);
        });

        TaskComment newComment = TaskComment.builder()
                .commentText("New comment")
                .userId(userId)
                .taskId(task.getId())
                .build();

        taskService.addComment(newComment).block();
        verify(taskCommentRepository, times(1)).save(any());
    }

    @Test
    void shouldSetCreatedAtFieldOnCommentAdd() {
        long userId = 1L;
        Task task = Task.builder().id(2L).userId(userId).title("Test task").build();

        when(taskRepository.findByIdAndUserId(task.getId(), userId)).thenReturn(Mono.just(task));
        when(taskCommentRepository.save(any())).thenAnswer(invocation -> {
            TaskComment comment = invocation.getArgument(0, TaskComment.class);
            comment.setId(3L);
            return Mono.just(comment);
        });

        TaskComment newComment = TaskComment.builder()
                .commentText("New comment")
                .userId(userId)
                .taskId(task.getId())
                .build();

        TaskComment result = taskService.addComment(newComment).block();
        assertNotNull(result);
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void shouldThrowExceptionOnCommentAddWhenCommentIsNull() {
        assertThrows(IllegalArgumentException.class, () -> taskService.addComment(null),
                "Task comment must not be null");
    }

    @Test
    void shouldThrowExceptionOnCommentAddWhenTaskIsNotFound() {
        long userId = 1L;
        long taskId = 2L;
        TaskComment comment = TaskComment.builder()
                .commentText("New comment")
                .userId(userId)
                .taskId(taskId)
                .build();
        when(taskRepository.findByIdAndUserId(taskId, userId)).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class, () -> taskService.addComment(comment).block(),
                "Task with id " + taskId + "is not found");
    }

    @Test
    void shouldReturnCommentsForTaskWithPagingRestriction() {
        User user = User.builder().id(1L).build();
        PageRequest pageRequest = PageRequest.of(3, 50);

        Task task = Task.builder().id(1L).userId(user.getId()).title("Test task").build();
        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(task.getId(), pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.empty());

        taskService.getComments(task.getId(), user, pageRequest).blockFirst();
        verify(taskCommentRepository, times(1)).findByTaskIdOrderByCreatedAtDesc(task.getId(),
                pageRequest.getOffset(), pageRequest.getPageSize());
    }

    @Test
    void shouldThrowExceptionOnCommentsGetWhenTaskIsNotFound() {
        when(taskRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        User user = User.builder().id(1L).build();
        assertThrows(EntityNotFoundException.class,
                () -> taskService.getComments(1L, user, Pageable.unpaged()).blockFirst());
    }
}

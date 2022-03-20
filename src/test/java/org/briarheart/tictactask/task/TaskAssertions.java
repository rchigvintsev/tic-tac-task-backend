package org.briarheart.tictactask.task;

import org.briarheart.tictactask.task.TaskController.TaskResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.platform.commons.util.UnrecoverableExceptions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TaskAssertions {
    private TaskAssertions() {
        //no instance
    }

    public static void assertAllWithStatuses(List<Task> tasks, TaskStatus... statuses) {
        TaskResponse[] responses = tasks.stream().map(TaskResponse::new).toArray(TaskResponse[]::new);
        assertAllWithStatuses(responses, statuses);
    }

    public static void assertAllWithStatuses(TaskResponse[] tasks, TaskStatus... statuses) {
        Set<TaskStatus> statusSet = Arrays.stream(statuses).collect(Collectors.toSet());
        for (TaskResponse task : tasks) {
            assertTrue(statusSet.contains(task.getStatus()), "Unexpected task status: " + task.getStatus());
        }
    }

    public static void assertAllByStatus(List<Task> tasks, Map<TaskStatus, ThrowingConsumer<Task>> consumers) {
        for (Task task : tasks) {
            ThrowingConsumer<Task> consumer = consumers.get(task.getStatus());
            assertNotNull(consumer, "Unexpected task status: " + task.getStatus());
            try {
                consumer.accept(task);
            } catch (Throwable t) {
                UnrecoverableExceptions.rethrowIfUnrecoverable(t);
                Assertions.fail(t);
            }
        }
    }

    public static void assertAllByStatus(TaskResponse[] tasks,
                                         Map<TaskStatus, ThrowingConsumer<TaskResponse>> consumers) {
        for (TaskResponse task : tasks) {
            ThrowingConsumer<TaskResponse> consumer = consumers.get(task.getStatus());
            assertNotNull(consumer, "Unexpected task status: " + task.getStatus());
            try {
                consumer.accept(task);
            } catch (Throwable t) {
                UnrecoverableExceptions.rethrowIfUnrecoverable(t);
                Assertions.fail(t);
            }
        }
    }

    public static void assertWithDeadlineWithinRange(Task task, LocalDateTime from, LocalDateTime to) {
        assertWithDeadlineWithinRange(new TaskResponse(task), from, to);
    }

    public static void assertWithDeadlineWithinRange(TaskResponse task, LocalDateTime from, LocalDateTime to) {
        LocalDate deadlineDate = task.getDeadlineDate();
        LocalDateTime deadlineDateTime = task.getDeadlineDateTime();

        if (deadlineDate != null) {
            assertNull(deadlineDateTime);
            assertTrue(!deadlineDate.isBefore(from.toLocalDate()) && !deadlineDate.isAfter(to.toLocalDate()),
                    "Task deadline (" + deadlineDate + ") was expected to be within range from " + from + " to " + to);
        } else {
            assertNotNull(deadlineDateTime);
            assertTrue(!deadlineDateTime.isBefore(from) && !deadlineDateTime.isAfter(to), "Task deadline ("
                    + deadlineDateTime + ") was expected to be within range from " + from + " to " + to);
        }
    }

    public static void assertAllWithDeadlineWithinRange(List<Task> tasks, LocalDateTime from, LocalDateTime to) {
        TaskResponse[] responses = tasks.stream().map(TaskResponse::new).toArray(TaskResponse[]::new);
        assertAllWithDeadlineWithinRange(responses, from, to);
    }

    public static void assertAllWithDeadlineWithinRange(TaskResponse[] tasks, LocalDateTime from, LocalDateTime to) {
        for (TaskResponse task : tasks) {
            assertWithDeadlineWithinRange(task, from, to);
        }
    }

    public static void assertAllWithDeadlineLessThanOrEqual(List<Task> tasks, LocalDateTime deadline) {
        TaskResponse[] responses = tasks.stream().map(TaskResponse::new).toArray(TaskResponse[]::new);
        assertAllWithDeadlineLessThanOrEqual(responses, deadline);
    }

    public static void assertAllWithDeadlineLessThanOrEqual(TaskResponse[] tasks, LocalDateTime deadline) {
        for (TaskResponse task : tasks) {
            LocalDate deadlineDate = task.getDeadlineDate();
            LocalDateTime deadlineDateTime = task.getDeadlineDateTime();

            if (deadlineDate != null) {
                assertNull(deadlineDateTime);
                assertFalse(deadlineDate.isAfter(deadline.toLocalDate()), "Task deadline (" + deadlineDate
                        + ") was expected to be less than or equal to " + deadline);
            } else {
                assertNotNull(deadlineDateTime);
                assertFalse(deadlineDateTime.isAfter(deadline), "Task deadline (" + deadlineDateTime
                        + ") was expected to be less than or equal to " + deadline);
            }
        }
    }

    public static void assertAllWithDeadlineGreaterThanOrEqual(List<Task> tasks, LocalDateTime deadline) {
        TaskResponse[] responses = tasks.stream().map(TaskResponse::new).toArray(TaskResponse[]::new);
        assertAllWithDeadlineGreaterThanOrEqual(responses, deadline);
    }

    public static void assertAllWithDeadlineGreaterThanOrEqual(TaskResponse[] tasks, LocalDateTime deadline) {
        for (TaskResponse task : tasks) {
            LocalDate deadlineDate = task.getDeadlineDate();
            LocalDateTime deadlineDateTime = task.getDeadlineDateTime();

            if (deadlineDate != null) {
                assertNull(deadlineDateTime);
                assertFalse(deadlineDate.isBefore(deadline.toLocalDate()), "Task deadline (" + deadlineDate
                        + ") was expected to be greater than or equal to " + deadline);
            } else {
                assertNotNull(deadlineDateTime);
                assertFalse(deadlineDateTime.isBefore(deadline), "Task deadline (" + deadlineDateTime
                        + ") was expected to be greater than or equal to " + deadline);
            }
        }
    }

    public static void assertAllWithoutDeadline(List<Task> tasks) {
        TaskResponse[] responses = tasks.stream().map(TaskResponse::new).toArray(TaskResponse[]::new);
        assertAllWithoutDeadline(responses);
    }

    public static void assertAllWithoutDeadline(TaskResponse[] tasks) {
        for (TaskResponse task : tasks) {
            assertNull(task.getDeadlineDate());
            assertNull(task.getDeadlineDateTime());
        }
    }

    public static void assertWithCompletedAtWithinRange(Task task, LocalDateTime from, LocalDateTime to) {
        assertWithCompletedAtWithinRange(new TaskResponse(task), from, to);
    }

    public static void assertWithCompletedAtWithinRange(TaskResponse task, LocalDateTime from, LocalDateTime to) {
        LocalDateTime completedAt = task.getCompletedAt();
        assertNotNull(completedAt);
        assertTrue(!completedAt.isBefore(from) && !completedAt.isAfter(to),
                "Task was expected to be completed at time within range from " + from + " to " + to);
    }

    public static void assertAllWithCompletedAtLessThanOrEqual(List<Task> tasks, LocalDateTime completedAt) {
        TaskResponse[] responses = tasks.stream().map(TaskResponse::new).toArray(TaskResponse[]::new);
        assertAllWithCompletedAtLessThanOrEqual(responses, completedAt);
    }

    public static void assertAllWithCompletedAtLessThanOrEqual(TaskResponse[] tasks, LocalDateTime completedAt) {
        for (TaskResponse task : tasks) {
            LocalDateTime taskCompletedAt = task.getCompletedAt();
            assertNotNull(taskCompletedAt);
            assertFalse(taskCompletedAt.isAfter(completedAt),
                    "Task was expected to be completed at time less than or equal to " + completedAt);
        }
    }

    public static void assertAllWithCompletedAtGreaterThanOrEqual(List<Task> tasks, LocalDateTime completedAt) {
        TaskResponse[] responses = tasks.stream().map(TaskResponse::new).toArray(TaskResponse[]::new);
        assertAllWithCompletedAtGreaterThanOrEqual(responses, completedAt);
    }

    public static void assertAllWithCompletedAtGreaterThanOrEqual(TaskResponse[] tasks, LocalDateTime completedAt) {
        for (TaskResponse task : tasks) {
            LocalDateTime taskCompletedAt = task.getCompletedAt();
            assertNotNull(taskCompletedAt);
            assertFalse(taskCompletedAt.isBefore(completedAt),
                    "Task was expected to be completed at time greater than or equal to " + completedAt);
        }
    }
}

package org.briarheart.tictactask.task;

import org.briarheart.tictactask.controller.format.LocalDateTimeFormatter;
import org.briarheart.tictactask.task.TaskController.TaskResponse;
import org.springframework.format.Formatter;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TaskAssertions {
    private static final Formatter<LocalDateTime> LOCAL_DATE_TIME_FORMATTER = new LocalDateTimeFormatter();

    private TaskAssertions() {
        //no instance
    }

    public static void assertAllTasksWithStatuses(List<Task> tasks, TaskStatus... expectedStatuses) {
        TaskResponse[] responses = tasks.stream().map(TaskResponse::new).toArray(TaskResponse[]::new);
        assertAllTasksWithStatuses(responses, expectedStatuses);
    }

    public static void assertAllTasksWithStatuses(TaskResponse[] tasks, TaskStatus... expectedStatuses) {
        Set<TaskStatus> statusSet = Arrays.stream(expectedStatuses).collect(Collectors.toSet());
        for (TaskResponse task : tasks) {
            assertTrue(statusSet.contains(task.getStatus()), "Unexpected task status: " + task.getStatus());
        }
    }

    public static void assertAllTasksWithDeadlineWithinRange(List<Task> tasks, String deadlineFrom, String deadlineTo) {
        TaskResponse[] responses = tasks.stream().map(TaskResponse::new).toArray(TaskResponse[]::new);
        assertAllTasksWithDeadlineWithinRange(responses, deadlineFrom, deadlineTo);
    }

    public static void assertAllTasksWithDeadlineWithinRange(TaskResponse[] tasks,
                                                             String deadlineFrom,
                                                             String deadlineTo) {
        for (TaskResponse task : tasks) {
            LocalDateTime deadline = task.getDeadline();
            assertNotNull(deadline);
            assertTrue(!deadline.isBefore(parseLocalDateTime(deadlineFrom))
                    && !deadline.isAfter(parseLocalDateTime(deadlineTo)), "Task deadline (" + deadline
                    + ") was expected to be within range from " + deadlineFrom + " to " + deadlineTo);
        }
    }

    public static void assertAllTasksWithDeadlineLessThanOrEqual(List<Task> tasks, String deadlineTo) {
        TaskResponse[] responses = tasks.stream().map(TaskResponse::new).toArray(TaskResponse[]::new);
        assertAllTasksWithDeadlineLessThanOrEqual(responses, deadlineTo);
    }

    public static void assertAllTasksWithDeadlineLessThanOrEqual(TaskResponse[] tasks, String deadlineTo) {
        for (TaskResponse task : tasks) {
            LocalDateTime deadline = task.getDeadline();
            assertNotNull(deadline);
            assertFalse(deadline.isAfter(parseLocalDateTime(deadlineTo)), "Task deadline (" + deadline
                    + ") was expected to be less than or equal to " + deadlineTo);
        }
    }

    public static void assertAllTasksWithDeadlineGreaterThanOrEqual(List<Task> tasks, String deadlineFrom) {
        TaskResponse[] responses = tasks.stream().map(TaskResponse::new).toArray(TaskResponse[]::new);
        assertAllTasksWithDeadlineGreaterThanOrEqual(responses, deadlineFrom);
    }

    public static void assertAllTasksWithDeadlineGreaterThanOrEqual(TaskResponse[] tasks, String deadlineFrom) {
        for (TaskResponse task : tasks) {
            LocalDateTime deadline = task.getDeadline();
            assertNotNull(deadline);
            assertFalse(deadline.isBefore(parseLocalDateTime(deadlineFrom)), "Task deadline (" + deadline
                    + ") was expected to be greater than or equal to " + deadlineFrom);
        }
    }

    public static void assertAllTasksWithoutDeadline(List<Task> tasks) {
        TaskResponse[] responses = tasks.stream().map(TaskResponse::new).toArray(TaskResponse[]::new);
        assertAllTasksWithoutDeadline(responses);
    }

    public static void assertAllTasksWithoutDeadline(TaskResponse[] tasks) {
        for (TaskResponse task : tasks) {
            assertNull(task.getDeadline());
        }
    }

    private static LocalDateTime parseLocalDateTime(String dateTime) {
        try {
            return LOCAL_DATE_TIME_FORMATTER.parse(dateTime, Locale.getDefault());
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse string " + dateTime + " to produce instance of "
                    + LocalDateTime.class.getName(), e);
        }
    }
}

package org.briarheart.tictactask.task;

import org.briarheart.tictactask.config.TestR2dbcConnectionFactoryConfig;
import org.briarheart.tictactask.controller.format.LocalDateTimeFormatter;
import org.briarheart.tictactask.user.User;
import org.briarheart.tictactask.util.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.format.Formatter;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.briarheart.tictactask.task.TaskAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {
        TestR2dbcConnectionFactoryConfig.class,
        R2dbcAutoConfiguration.class,
        R2dbcDataAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
class CustomizedTaskRepositoryImplTest {
    private static final Formatter<LocalDateTime> LOCAL_DATE_TIME_FORMATTER = new LocalDateTimeFormatter();

    @Autowired
    private R2dbcEntityTemplate entityTemplate;
    private CustomizedTaskRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new CustomizedTaskRepositoryImpl(entityTemplate);
    }

    @Test
    void shouldThrowExceptionOnConstructWhenEntityTemplateIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new CustomizedTaskRepositoryImpl(null));
        assertEquals("Entity template must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnCountWhenGetTasksRequestIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> repository.count(null, TestUsers.JOHN_DOE).block());
        assertEquals("Request must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnCountWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> repository.count(new GetTasksRequest(), null).block());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnFindWhenGetTasksRequestIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> repository.find(null, User.builder().id(1L).build(), Pageable.unpaged()).blockFirst());
        assertEquals("Request must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnFindWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> repository.find(new GetTasksRequest(), null, Pageable.unpaged()).blockFirst());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldReturnNumberOfAllUnprocessedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.UNPROCESSED));
        assertEquals(3L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnAllUnprocessedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.UNPROCESSED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(3L, result.size());
        assertAllTasksWithStatuses(result, TaskStatus.UNPROCESSED);
    }

    @Test
    void shouldReturnUnprocessedTasksWithPagingRestriction() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.UNPROCESSED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.ofSize(2)).collectList().block();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertAllTasksWithStatuses(result, TaskStatus.UNPROCESSED);
    }

    @Test
    void shouldReturnNumberOfAllProcessedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        assertEquals(3L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnAllProcessedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(3L, result.size());
        assertAllTasksWithStatuses(result, TaskStatus.PROCESSED);
    }

    @Test
    void shouldReturnProcessedTasksWithPagingRestriction() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.ofSize(2)).collectList().block();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertAllTasksWithStatuses(result, TaskStatus.PROCESSED);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateBetween() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineFrom(parseLocalDateTime("2022-01-01T00:00"));
        request.setDeadlineTo(parseLocalDateTime("2022-01-02T23:59"));
        assertEquals(2L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateBetween() {
        String deadlineFrom = "2022-01-01T00:00";
        String deadlineTo = "2022-01-02T23:59";

        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineFrom(parseLocalDateTime(deadlineFrom));
        request.setDeadlineTo(parseLocalDateTime(deadlineTo));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertAllTasksWithStatuses(result, TaskStatus.PROCESSED);
        assertAllTasksWithDeadlineWithinRange(result, deadlineFrom, deadlineTo);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateLessThanOrEqual() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineTo(parseLocalDateTime("2022-01-01T00:00"));
        assertEquals(1L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateLessThanOrEqual() {
        String deadlineTo = "2022-01-01T00:00";

        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineTo(parseLocalDateTime(deadlineTo));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertAllTasksWithStatuses(result, TaskStatus.PROCESSED);
        assertAllTasksWithDeadlineLessThanOrEqual(result, deadlineTo);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateGreaterThanOrEqual() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineFrom(parseLocalDateTime("2022-01-02T23:59"));
        assertEquals(1L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateGreaterThanOrEqual() {
        String deadlineFrom = "2022-01-02T23:59";

        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineFrom(parseLocalDateTime(deadlineFrom));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertAllTasksWithStatuses(result, TaskStatus.PROCESSED);
        assertAllTasksWithDeadlineGreaterThanOrEqual(result, deadlineFrom);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithoutDeadlineDate() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineFrom(null);
        request.setDeadlineTo(null);
        assertEquals(1L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnProcessedTasksWithoutDeadlineDate() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineFrom(null);
        request.setDeadlineTo(null);

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertAllTasksWithStatuses(result, TaskStatus.PROCESSED);
        assertAllTasksWithoutDeadline(result);
    }

    @Test
    void shouldReturnNumberOfAllUncompletedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.UNPROCESSED, TaskStatus.PROCESSED));
        assertEquals(6L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnAllUncompletedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.UNPROCESSED, TaskStatus.PROCESSED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(6L, result.size());
        assertAllTasksWithStatuses(result, TaskStatus.UNPROCESSED, TaskStatus.PROCESSED);
    }

    @Test
    void shouldReturnUncompletedTasksWithPagingRestriction() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.UNPROCESSED, TaskStatus.PROCESSED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.ofSize(2)).collectList().block();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertAllTasksWithStatuses(result, TaskStatus.UNPROCESSED, TaskStatus.PROCESSED);
    }

    @Test
    void shouldReturnNumberOfAllCompletedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.COMPLETED));
        assertEquals(3L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnAllCompletedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.COMPLETED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(3L, result.size());
        assertAllTasksWithStatuses(result, TaskStatus.COMPLETED);
    }

    @Test
    void shouldReturnCompletedTasksWithPagingRestriction() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.COMPLETED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.ofSize(2)).collectList().block();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertAllTasksWithStatuses(result, TaskStatus.COMPLETED);
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
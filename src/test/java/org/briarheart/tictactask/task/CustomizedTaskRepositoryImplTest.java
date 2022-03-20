package org.briarheart.tictactask.task;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.briarheart.tictactask.config.TestR2dbcConnectionFactoryConfig;
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
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.briarheart.tictactask.task.TaskAssertions.*;
import static org.briarheart.tictactask.util.DateTimeUtils.parseIsoDate;
import static org.briarheart.tictactask.util.DateTimeUtils.parseIsoDateTime;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {
        TestR2dbcConnectionFactoryConfig.class,
        R2dbcAutoConfiguration.class,
        R2dbcDataAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase
class CustomizedTaskRepositoryImplTest {
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
        assertAllWithStatuses(result, TaskStatus.UNPROCESSED);
    }

    @Test
    void shouldReturnUnprocessedTasksWithPagingRestriction() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.UNPROCESSED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.ofSize(2)).collectList().block();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertAllWithStatuses(result, TaskStatus.UNPROCESSED);
    }

    @Test
    void shouldReturnNumberOfAllProcessedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        assertEquals(5L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnAllProcessedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(5L, result.size());
        assertAllWithStatuses(result, TaskStatus.PROCESSED);
    }

    @Test
    void shouldReturnProcessedTasksWithPagingRestriction() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.ofSize(2)).collectList().block();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertAllWithStatuses(result, TaskStatus.PROCESSED);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineBetween() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineDateFrom(parseIsoDate("2022-01-01"));
        request.setDeadlineDateTo(parseIsoDate("2022-01-02"));
        request.setDeadlineDateTimeFrom(parseIsoDateTime("2022-01-01T00:00"));
        request.setDeadlineDateTimeTo(parseIsoDateTime("2022-01-02T23:59"));

        assertEquals(4L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineBetween() {
        LocalDateTime deadlineDateTimeFrom = parseIsoDateTime("2022-01-01T00:00");
        LocalDateTime deadlineDateTimeTo = parseIsoDateTime("2022-01-02T23:59");

        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineDateFrom(parseIsoDate("2022-01-01"));
        request.setDeadlineDateTo(parseIsoDate("2022-01-02"));
        request.setDeadlineDateTimeFrom(deadlineDateTimeFrom);
        request.setDeadlineDateTimeTo(deadlineDateTimeTo);

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(4, result.size());
        assertAllWithStatuses(result, TaskStatus.PROCESSED);
        assertAllWithDeadlineWithinRange(result, deadlineDateTimeFrom, deadlineDateTimeTo);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineLessThanOrEqual() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineDateTo(parseIsoDate("2022-01-01"));
        request.setDeadlineDateTimeTo(parseIsoDateTime("2022-01-01T00:00"));
        assertEquals(2L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineLessThanOrEqual() {
        LocalDateTime deadlineDateTimeTo = parseIsoDateTime("2022-01-01T00:00");

        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineDateTo(parseIsoDate("2022-01-01"));
        request.setDeadlineDateTimeTo(deadlineDateTimeTo);

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertAllWithStatuses(result, TaskStatus.PROCESSED);
        assertAllWithDeadlineLessThanOrEqual(result, deadlineDateTimeTo);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineGreaterThanOrEqual() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineDateFrom(parseIsoDate("2022-01-02"));
        request.setDeadlineDateTimeFrom(parseIsoDateTime("2022-01-02T23:59"));
        assertEquals(2L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineGreaterThanOrEqual() {
        LocalDateTime deadlineDateTimeFrom = parseIsoDateTime("2022-01-02T23:59");

        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineDateFrom(parseIsoDate("2022-01-02"));
        request.setDeadlineDateTimeFrom(deadlineDateTimeFrom);

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertAllWithStatuses(result, TaskStatus.PROCESSED);
        assertAllWithDeadlineGreaterThanOrEqual(result, deadlineDateTimeFrom);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithoutDeadline() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setWithoutDeadline(true);
        assertEquals(1L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnProcessedTasksWithoutDeadline() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setWithoutDeadline(true);

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertAllWithStatuses(result, TaskStatus.PROCESSED);
        assertAllWithoutDeadline(result);
    }

    @Test
    void shouldReturnNumberOfAllUncompletedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.UNPROCESSED, TaskStatus.PROCESSED));
        assertEquals(8L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnAllUncompletedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.UNPROCESSED, TaskStatus.PROCESSED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(8L, result.size());
        assertAllWithStatuses(result, TaskStatus.UNPROCESSED, TaskStatus.PROCESSED);
    }

    @Test
    void shouldReturnUncompletedTasksWithPagingRestriction() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.UNPROCESSED, TaskStatus.PROCESSED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.ofSize(2)).collectList().block();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertAllWithStatuses(result, TaskStatus.UNPROCESSED, TaskStatus.PROCESSED);
    }

    @Test
    void shouldReturnNumberOfAllCompletedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.COMPLETED));
        assertEquals(4L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnAllCompletedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.COMPLETED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(4L, result.size());
        assertAllWithStatuses(result, TaskStatus.COMPLETED);
    }

    @Test
    void shouldReturnCompletedTasksWithPagingRestriction() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.COMPLETED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.ofSize(2)).collectList().block();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertAllWithStatuses(result, TaskStatus.COMPLETED);
    }

    @Test
    void shouldReturnNumberOfEitherProcessedOrCompletedTasksWithDeadlineBetweenAndCompletedAtBetween() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED, TaskStatus.COMPLETED));

        LocalDateTime dateTimeFrom = parseIsoDateTime("2022-01-01T00:00");
        LocalDateTime dateTimeTo = parseIsoDateTime("2022-01-01T23:59");

        request.setDeadlineDateTimeFrom(dateTimeFrom);
        request.setDeadlineDateTimeTo(dateTimeTo);
        request.setCompletedAtFrom(dateTimeFrom);
        request.setCompletedAtTo(dateTimeTo);

        assertEquals(2L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnEitherProcessedOrCompletedTasksWithDeadlineBetweenAndCompletedAtBetween() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED, TaskStatus.COMPLETED));

        LocalDateTime dateTimeFrom = parseIsoDateTime("2022-01-01T00:00");
        LocalDateTime dateTimeTo = parseIsoDateTime("2022-01-01T23:59");

        request.setDeadlineDateTimeFrom(dateTimeFrom);
        request.setDeadlineDateTimeTo(dateTimeTo);
        request.setCompletedAtFrom(dateTimeFrom);
        request.setCompletedAtTo(dateTimeTo);

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(2L, result.size());
        assertAllByStatus(result, Map.of(
                TaskStatus.PROCESSED, t -> assertWithDeadlineWithinRange(t, dateTimeFrom, dateTimeTo),
                TaskStatus.COMPLETED, t -> assertWithCompletedAtWithinRange(t, dateTimeFrom, dateTimeTo)
        ));
    }

    @Test
    void shouldReturnNumberOfCompletedTasksWithCompletedAtLessThanOrEqual() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.COMPLETED));
        request.setCompletedAtTo(parseIsoDateTime("2022-01-01T00:00"));
        assertEquals(1L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnCompletedTasksWithCompletedAtLessThanOrEqual() {
        LocalDateTime completedAtTo = parseIsoDateTime("2022-01-01T00:00");

        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.COMPLETED));
        request.setCompletedAtTo(completedAtTo);

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertAllWithStatuses(result, TaskStatus.COMPLETED);
        assertAllWithCompletedAtLessThanOrEqual(result, completedAtTo);
    }

    @Test
    void shouldReturnNumberOfCompletedTasksWithCompletedAtGreaterThanOrEqual() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.COMPLETED));
        request.setCompletedAtFrom(parseIsoDateTime("2022-01-02T00:00"));
        assertEquals(1L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnCompletedTasksWithCompletedAtGreaterThanOrEqual() {
        LocalDateTime completedAtFrom = parseIsoDateTime("2022-01-02T00:00");

        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.COMPLETED));
        request.setCompletedAtFrom(completedAtFrom);

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertAllWithStatuses(result, TaskStatus.COMPLETED);
        assertAllWithCompletedAtGreaterThanOrEqual(result, completedAtFrom);
    }

    @Test
    void shouldReturnNumberOfUnprocessedTasksAndCompletedTasksBeingPreviouslyUnprocessed() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.UNPROCESSED, TaskStatus.COMPLETED));
        assertEquals(4L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnUnprocessedTasksAndCompletedTasksBeingPreviouslyUnprocessed() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.UNPROCESSED, TaskStatus.COMPLETED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(4, result.size());
        assertAllWithStatuses(result, TaskStatus.UNPROCESSED, TaskStatus.COMPLETED);
    }
}
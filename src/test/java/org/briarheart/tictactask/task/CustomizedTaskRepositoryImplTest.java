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
        assertEquals(3L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnAllProcessedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(3L, result.size());
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
        request.setDeadlineFrom(parseIsoDateTime("2022-01-01T00:00"));
        request.setDeadlineTo(parseIsoDateTime("2022-01-02T23:59"));
        assertEquals(2L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineBetween() {
        LocalDateTime deadlineFrom = parseIsoDateTime("2022-01-01T00:00");
        LocalDateTime deadlineTo = parseIsoDateTime("2022-01-02T23:59");

        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineFrom(deadlineFrom);
        request.setDeadlineTo(deadlineTo);

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertAllWithStatuses(result, TaskStatus.PROCESSED);
        assertAllWithDeadlineWithinRange(result, deadlineFrom, deadlineTo);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineLessThanOrEqual() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineTo(parseIsoDateTime("2022-01-01T00:00"));
        assertEquals(1L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineLessThanOrEqual() {
        LocalDateTime deadlineTo = parseIsoDateTime("2022-01-01T00:00");

        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineTo(deadlineTo);

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertAllWithStatuses(result, TaskStatus.PROCESSED);
        assertAllDeadlineLessThanOrEqual(result, deadlineTo);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineGreaterThanOrEqual() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineFrom(parseIsoDateTime("2022-01-02T23:59"));
        assertEquals(1L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineGreaterThanOrEqual() {
        LocalDateTime deadlineFrom = parseIsoDateTime("2022-01-02T23:59");

        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineFrom(deadlineFrom);

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertAllWithStatuses(result, TaskStatus.PROCESSED);
        assertAllWithDeadlineGreaterThanOrEqual(result, deadlineFrom);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithoutDeadline() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineFrom(null);
        request.setDeadlineTo(null);
        assertEquals(1L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnProcessedTasksWithoutDeadline() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineFrom(null);
        request.setDeadlineTo(null);

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
        assertEquals(6L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnAllUncompletedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.UNPROCESSED, TaskStatus.PROCESSED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(6L, result.size());
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
        assertEquals(3L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnAllCompletedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.COMPLETED));

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(3L, result.size());
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

        LocalDateTime dateFrom = parseIsoDateTime("2022-01-01T00:00");
        LocalDateTime dateTo = parseIsoDateTime("2022-01-01T23:59");

        request.setDeadlineFrom(dateFrom);
        request.setDeadlineTo(dateTo);
        request.setCompletedAtFrom(dateFrom);
        request.setCompletedAtTo(dateTo);

        assertEquals(2L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnEitherProcessedOrCompletedTasksWithDeadlineBetweenAndCompletedAtBetween() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED, TaskStatus.COMPLETED));

        LocalDateTime dateFrom = parseIsoDateTime("2022-01-01T00:00");
        LocalDateTime dateTo = parseIsoDateTime("2022-01-01T23:59");

        request.setDeadlineFrom(dateFrom);
        request.setDeadlineTo(dateTo);
        request.setCompletedAtFrom(dateFrom);
        request.setCompletedAtTo(dateTo);

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(2L, result.size());
        assertAllByStatus(result, Map.of(
                TaskStatus.PROCESSED, t -> assertWithDeadlineWithinRange(t, dateFrom, dateTo),
                TaskStatus.COMPLETED, t -> assertWithCompletedAtWithinRange(t, dateFrom, dateTo)
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
        request.setCompletedAtFrom(parseIsoDateTime("2022-01-02T23:59"));
        assertEquals(1L, repository.count(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnCompletedTasksWithCompletedAtGreaterThanOrEqual() {
        LocalDateTime completedAtFrom = parseIsoDateTime("2022-01-02T23:59");

        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.COMPLETED));
        request.setCompletedAtFrom(completedAtFrom);

        List<Task> result = repository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged()).collectList().block();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertAllWithStatuses(result, TaskStatus.COMPLETED);
        assertAllWithCompletedAtGreaterThanOrEqual(result, completedAtFrom);
    }
}
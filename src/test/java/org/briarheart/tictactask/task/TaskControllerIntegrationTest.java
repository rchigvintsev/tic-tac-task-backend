package org.briarheart.tictactask.task;

import org.briarheart.tictactask.config.TestJavaMailSenderConfiguration;
import org.briarheart.tictactask.task.TaskController.TaskResponse;
import org.briarheart.tictactask.util.TestAccessTokens;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "security.disabled=false")
@Import(TestJavaMailSenderConfiguration.class)
@ActiveProfiles("test")
class TaskControllerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void shouldReturnNumberOfAllUnprocessedTasks() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks/count?statuses=UNPROCESSED";
        ResponseEntity<Long> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                Long.class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Long taskCount = response.getBody();
        assertNotNull(taskCount);
        assertTrue(taskCount > 2, "At least three unprocessed tasks were expected but got " + taskCount);
    }

    @Test
    void shouldReturnAllUnprocessedTasks() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks?statuses=UNPROCESSED";
        ResponseEntity<TaskResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                TaskResponse[].class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        TaskResponse[] tasks = response.getBody();
        assertNotNull(tasks);
        assertTrue(tasks.length > 2, "At least three unprocessed tasks were expected but got " + tasks.length);
        assertAllTasksWithStatuses(tasks, TaskStatus.UNPROCESSED);
    }

    @Test
    void shouldReturnUnprocessedTasksWithPagingRestriction() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks?statuses=UNPROCESSED&page=0&size=2";
        ResponseEntity<TaskResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                TaskResponse[].class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        TaskResponse[] tasks = response.getBody();
        assertNotNull(tasks);
        assertEquals(2, tasks.length);
        assertAllTasksWithStatuses(tasks, TaskStatus.UNPROCESSED);
    }

    @Test
    void shouldReturnNumberOfAllProcessedTasks() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks/count?statuses=PROCESSED";
        ResponseEntity<Long> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                Long.class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Long taskCount = response.getBody();
        assertNotNull(taskCount);
        assertTrue(taskCount > 2, "At least three processed tasks were expected but got " + taskCount);
    }

    @Test
    void shouldReturnAllProcessedTasks() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks?statuses=PROCESSED";
        ResponseEntity<TaskResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                TaskResponse[].class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        TaskResponse[] tasks = response.getBody();
        assertNotNull(tasks);
        assertTrue(tasks.length > 2, "At least three processed tasks were expected but got " + tasks.length);
        assertAllTasksWithStatuses(tasks, TaskStatus.PROCESSED);
    }

    @Test
    void shouldReturnProcessedTasksWithPagingRestriction() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks?statuses=PROCESSED&page=0&size=2";
        ResponseEntity<TaskResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                TaskResponse[].class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        TaskResponse[] tasks = response.getBody();
        assertNotNull(tasks);
        assertEquals(2, tasks.length);
        assertAllTasksWithStatuses(tasks, TaskStatus.PROCESSED);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateBetween() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks/count?statuses=PROCESSED&deadlineFrom=2022-01-01T00:00" +
                "&deadlineTo=2022-01-02T23:59";
        ResponseEntity<Long> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                Long.class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Long taskCount = response.getBody();
        assertNotNull(taskCount);
        assertEquals(2, taskCount);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateBetween() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String deadlineFrom = "2022-01-01T00:00";
        String deadlineTo = "2022-01-02T23:59";
        String url = "http://localhost:{port}/api/v1/tasks?statuses=PROCESSED&deadlineFrom=" + deadlineFrom
                + "&deadlineTo=" + deadlineTo;
        ResponseEntity<TaskResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                TaskResponse[].class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        TaskResponse[] tasks = response.getBody();
        assertNotNull(tasks);
        assertEquals(2, tasks.length);
        assertAllTasksWithStatuses(tasks, TaskStatus.PROCESSED);
        assertAllTasksWithDeadlineWithinRange(tasks, deadlineFrom, deadlineTo);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateLessThanOrEqual() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks/count?statuses=PROCESSED&deadlineTo=2022-01-01T00:00";
        ResponseEntity<Long> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                Long.class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Long taskCount = response.getBody();
        assertNotNull(taskCount);
        assertEquals(1, taskCount);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateLessThanOrEqual() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String deadlineTo = "2022-01-01T00:00";
        String url = "http://localhost:{port}/api/v1/tasks?statuses=PROCESSED&deadlineTo=" + deadlineTo;
        ResponseEntity<TaskResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                TaskResponse[].class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        TaskResponse[] tasks = response.getBody();
        assertNotNull(tasks);
        assertEquals(1, tasks.length);
        assertAllTasksWithStatuses(tasks, TaskStatus.PROCESSED);
        assertAllTasksWithDeadlineLessThanOrEqual(tasks, deadlineTo);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateGreaterThanOrEqual() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks/count?statuses=PROCESSED&deadlineFrom=2022-01-02T23:59";
        ResponseEntity<Long> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                Long.class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Long taskCount = response.getBody();
        assertNotNull(taskCount);
        assertEquals(1, taskCount);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateGreaterThanOrEqual() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String deadlineFrom = "2022-01-02T23:59";
        String url = "http://localhost:{port}/api/v1/tasks?statuses=PROCESSED&deadlineFrom=" + deadlineFrom;
        ResponseEntity<TaskResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                TaskResponse[].class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        TaskResponse[] tasks = response.getBody();
        assertNotNull(tasks);
        assertEquals(1, tasks.length);
        assertAllTasksWithStatuses(tasks, TaskStatus.PROCESSED);
        assertAllTasksWithDeadlineGreaterThanOrEqual(tasks, deadlineFrom);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithoutDeadlineDate() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks/count?statuses=PROCESSED&deadlineFrom=&deadlineTo=";
        ResponseEntity<Long> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                Long.class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Long taskCount = response.getBody();
        assertNotNull(taskCount);
        assertEquals(1, taskCount);
    }

    @Test
    void shouldReturnProcessedTasksWithoutDeadlineDate() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks?statuses=PROCESSED&deadlineFrom=&deadlineTo=";
        ResponseEntity<TaskResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                TaskResponse[].class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        TaskResponse[] tasks = response.getBody();
        assertNotNull(tasks);
        assertEquals(1, tasks.length);
        assertAllTasksWithStatuses(tasks, TaskStatus.PROCESSED);
        assertAllTasksWithoutDeadline(tasks);
    }

    @Test
    void shouldReturnNumberOfAllUncompletedTasks() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks/count?statuses=UNPROCESSED,PROCESSED";
        ResponseEntity<Long> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                Long.class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Long taskCount = response.getBody();
        assertNotNull(taskCount);
        assertTrue(taskCount > 5, "At least six uncompleted tasks were expected but got " + taskCount);
    }

    @Test
    void shouldReturnAllUncompletedTasks() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks?statuses=UNPROCESSED,PROCESSED";
        ResponseEntity<TaskResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                TaskResponse[].class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        TaskResponse[] tasks = response.getBody();
        assertNotNull(tasks);
        assertTrue(tasks.length > 5, "At least six uncompleted tasks were expected but got " + tasks.length);
        assertAllTasksWithStatuses(tasks, TaskStatus.UNPROCESSED, TaskStatus.PROCESSED);
    }

    @Test
    void shouldReturnUncompletedTasksWithPagingRestriction() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks?statuses=UNPROCESSED,PROCESSED&page=0&size=2";
        ResponseEntity<TaskResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                TaskResponse[].class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        TaskResponse[] tasks = response.getBody();
        assertNotNull(tasks);
        assertEquals(2, tasks.length);
        assertAllTasksWithStatuses(tasks, TaskStatus.UNPROCESSED, TaskStatus.PROCESSED);
    }

    @Test
    void shouldReturnNumberOfAllCompletedTasks() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks/count?statuses=COMPLETED";
        ResponseEntity<Long> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                Long.class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Long taskCount = response.getBody();
        assertNotNull(taskCount);
        assertTrue(taskCount > 2, "At three completed tasks were expected but got " + taskCount);
    }

    @Test
    void shouldReturnAllCompletedTasks() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks?statuses=COMPLETED";
        ResponseEntity<TaskResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                TaskResponse[].class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        TaskResponse[] tasks = response.getBody();
        assertNotNull(tasks);
        assertTrue(tasks.length > 2, "At least three completed tasks were expected but got " + tasks.length);
        assertAllTasksWithStatuses(tasks, TaskStatus.COMPLETED);
    }

    @Test
    void shouldReturnCompletedTasksWithPagingRestriction() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks?statuses=COMPLETED&page=0&size=2";
        ResponseEntity<TaskResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                TaskResponse[].class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        TaskResponse[] tasks = response.getBody();
        assertNotNull(tasks);
        assertEquals(2, tasks.length);
        assertAllTasksWithStatuses(tasks, TaskStatus.COMPLETED);
    }

    @Test
    void shouldCreateTask() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        String body = "{\"title\": \"Do morning exercises\", \"recurrenceStrategy\": {\"type\": \"daily\"}}";

        ResponseEntity<TaskResponse> response = restTemplate.exchange("http://localhost:{port}/api/v1/tasks",
                HttpMethod.POST, new HttpEntity<>(body, headers), TaskResponse.class, port);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void shouldAssignTagToTask() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        String url = "http://localhost:{port}/api/v1/tasks/{taskId}/tags/{tagId}";
        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(headers),
                Void.class, port, 1, 1);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    private void addCookieHeader(HttpHeaders headers) {
        headers.add(HttpHeaders.COOKIE, "access_token=" + TestAccessTokens.JOHN_DOE);
    }

    private void assertAllTasksWithStatuses(TaskResponse[] tasks, TaskStatus... expectedStatuses) {
        Set<TaskStatus> statusSet = Arrays.stream(expectedStatuses).collect(Collectors.toSet());
        for (TaskResponse task : tasks) {
            assertTrue(statusSet.contains(task.getStatus()), "Unexpected task status: " + task.getStatus());
        }
    }

    private void assertAllTasksWithDeadlineWithinRange(TaskResponse[] tasks, String deadlineFrom, String deadlineTo) {
        for (TaskResponse task : tasks) {
            LocalDateTime deadline = task.getDeadline();
            assertNotNull(deadline);
            assertTrue(!deadline.isBefore(parseIsoDateTime(deadlineFrom))
                            && !deadline.isAfter(parseIsoDateTime(deadlineTo)), "Task deadline (" + deadline
                    + ") was expected to be within range from " + deadlineFrom + " to " + deadlineTo);
        }
    }

    private void assertAllTasksWithDeadlineLessThanOrEqual(TaskResponse[] tasks, String deadlineTo) {
        for (TaskResponse task : tasks) {
            LocalDateTime deadline = task.getDeadline();
            assertNotNull(deadline);
            assertFalse(deadline.isAfter(parseIsoDateTime(deadlineTo)), "Task deadline (" + deadline
                    + ") was expected to be less than or equal to " + deadlineTo);
        }
    }

    private void assertAllTasksWithDeadlineGreaterThanOrEqual(TaskResponse[] tasks, String deadlineFrom) {
        for (TaskResponse task : tasks) {
            LocalDateTime deadline = task.getDeadline();
            assertNotNull(deadline);
            assertFalse(deadline.isBefore(parseIsoDateTime(deadlineFrom)), "Task deadline (" + deadline
                    + ") was expected to be greater than or equal to " + deadlineFrom);
        }
    }

    private void assertAllTasksWithoutDeadline(TaskResponse[] tasks) {
        for (TaskResponse task : tasks) {
            assertNull(task.getDeadline());
        }
    }

    private LocalDateTime parseIsoDateTime(String dateTime) {
        return LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_DATE_TIME);
    }
}

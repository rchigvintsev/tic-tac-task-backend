package org.briarheart.tictactask.task;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.briarheart.tictactask.task.TaskController.TaskResponse;
import org.briarheart.tictactask.util.TestAccessTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.briarheart.tictactask.task.TaskAssertions.*;
import static org.briarheart.tictactask.util.DateTimeUtils.parseIsoDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roman Chigvintsev
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase
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
        assertAllWithStatuses(tasks, TaskStatus.UNPROCESSED);
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
        assertAllWithStatuses(tasks, TaskStatus.UNPROCESSED);
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
        assertAllWithStatuses(tasks, TaskStatus.PROCESSED);
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
        assertAllWithStatuses(tasks, TaskStatus.PROCESSED);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineBetween() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks/count?statuses=PROCESSED"
                + "&deadlineDateFrom=2022-01-01"
                + "&deadlineDateTo=2022-01-02"
                + "&deadlineDateTimeFrom=2022-01-01T00:00"
                + "&deadlineDateTimeTo=2022-01-02T23:59";
        ResponseEntity<Long> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                Long.class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Long taskCount = response.getBody();
        assertNotNull(taskCount);
        assertEquals(4, taskCount);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateBetween() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String deadlineDateTimeFrom = "2022-01-01T00:00";
        String deadlineDateTimeTo = "2022-01-02T23:59";
        String url = "http://localhost:{port}/api/v1/tasks?statuses=PROCESSED"
                + "&deadlineDateFrom=2022-01-01"
                + "&deadlineDateTo=2022-01-02"
                + "&deadlineDateTimeFrom=" + deadlineDateTimeFrom
                + "&deadlineDateTimeTo=" + deadlineDateTimeTo;
        ResponseEntity<TaskResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                TaskResponse[].class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        TaskResponse[] tasks = response.getBody();
        assertNotNull(tasks);
        assertEquals(4, tasks.length);
        assertAllWithStatuses(tasks, TaskStatus.PROCESSED);
        assertAllWithDeadlineWithinRange(tasks, parseIsoDateTime(deadlineDateTimeFrom),
                parseIsoDateTime(deadlineDateTimeTo));
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateLessThanOrEqual() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks/count?statuses=PROCESSED"
                + "&deadlineDateTo=2022-01-01"
                + "&deadlineDateTimeTo=2022-01-01T00:00";
        ResponseEntity<Long> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                Long.class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Long taskCount = response.getBody();
        assertNotNull(taskCount);
        assertEquals(2, taskCount);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateLessThanOrEqual() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String deadlineDateTimeTo = "2022-01-01T00:00";
        String url = "http://localhost:{port}/api/v1/tasks?statuses=PROCESSED"
                + "&deadlineDateTo=2022-01-01"
                + "&deadlineDateTimeTo=" + deadlineDateTimeTo;
        ResponseEntity<TaskResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                TaskResponse[].class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        TaskResponse[] tasks = response.getBody();
        assertNotNull(tasks);
        assertEquals(2, tasks.length);
        assertAllWithStatuses(tasks, TaskStatus.PROCESSED);
        assertAllWithDeadlineLessThanOrEqual(tasks, parseIsoDateTime(deadlineDateTimeTo));
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateGreaterThanOrEqual() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks/count?statuses=PROCESSED"
                + "&deadlineDateFrom=2022-01-02"
                + "&deadlineDateTimeFrom=2022-01-02T23:59";
        ResponseEntity<Long> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                Long.class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Long taskCount = response.getBody();
        assertNotNull(taskCount);
        assertEquals(2, taskCount);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateGreaterThanOrEqual() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String deadlineDateTimeFrom = "2022-01-02T23:59";
        String url = "http://localhost:{port}/api/v1/tasks?statuses=PROCESSED"
                + "&deadlineDateFrom=2022-01-02"
                + "&deadlineDateTimeFrom=" + deadlineDateTimeFrom;
        ResponseEntity<TaskResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                TaskResponse[].class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        TaskResponse[] tasks = response.getBody();
        assertNotNull(tasks);
        assertEquals(2, tasks.length);
        assertAllWithStatuses(tasks, TaskStatus.PROCESSED);
        assertAllWithDeadlineGreaterThanOrEqual(tasks, parseIsoDateTime(deadlineDateTimeFrom));
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithoutDeadlineDate() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks/count?statuses=PROCESSED&withoutDeadline=true";
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

        String url = "http://localhost:{port}/api/v1/tasks?statuses=PROCESSED&withoutDeadline=true";
        ResponseEntity<TaskResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                TaskResponse[].class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        TaskResponse[] tasks = response.getBody();
        assertNotNull(tasks);
        assertEquals(1, tasks.length);
        assertAllWithStatuses(tasks, TaskStatus.PROCESSED);
        assertAllWithoutDeadline(tasks);
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
        assertAllWithStatuses(tasks, TaskStatus.UNPROCESSED, TaskStatus.PROCESSED);
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
        assertAllWithStatuses(tasks, TaskStatus.UNPROCESSED, TaskStatus.PROCESSED);
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
        assertAllWithStatuses(tasks, TaskStatus.COMPLETED);
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
        assertAllWithStatuses(tasks, TaskStatus.COMPLETED);
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
}

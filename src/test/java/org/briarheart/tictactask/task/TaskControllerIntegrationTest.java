package org.briarheart.tictactask.task;

import org.briarheart.tictactask.task.TaskController.TaskResponse;
import org.briarheart.tictactask.util.TestAccessTokens;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "security.disabled=false")
@ActiveProfiles("test")
class TaskControllerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void shouldReturnUnprocessedTasks() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tasks/unprocessed";
        ResponseEntity<TaskResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                TaskResponse[].class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        TaskResponse[] tasks = response.getBody();
        assertTrue(tasks != null && tasks.length > 0);
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

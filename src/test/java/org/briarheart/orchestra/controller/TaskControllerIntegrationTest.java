package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.util.TestAccessTokens;
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

        ResponseEntity<Task[]> response = restTemplate.exchange("http://localhost:{port}/v1/tasks/unprocessed",
                HttpMethod.GET, new HttpEntity<>(headers), Task[].class, port);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Task[] tasks = response.getBody();
        assertTrue(tasks != null && tasks.length > 0);
    }

    @Test
    void shouldAssignTagToTask() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        ResponseEntity<Void> response = restTemplate.exchange("http://localhost:{port}/v1/tasks/{taskId}/tags/{tagId}",
                HttpMethod.PUT, new HttpEntity<>(headers), Void.class, port, 1, 1);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    private void addCookieHeader(HttpHeaders headers) {
        headers.add(HttpHeaders.COOKIE, "access_token=" + TestAccessTokens.JOHN_DOE);
    }
}

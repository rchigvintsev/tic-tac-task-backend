package org.briarheart.tictactask.task.tag;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.briarheart.tictactask.task.TaskController.TaskResponse;
import org.briarheart.tictactask.task.tag.TaskTagController.TaskTagResponse;
import org.briarheart.tictactask.util.TestAccessTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Roman Chigvintsev
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase
class TaskTagControllerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void shouldCreateTag() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        String body = "{\"name\": \"My tag\"}";

        ResponseEntity<TaskTagResponse> response = restTemplate.exchange("http://localhost:{port}/api/v1/tags",
                HttpMethod.POST, new HttpEntity<>(body, headers), TaskTagResponse.class, port);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void shouldReturnUncompletedTasks() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/api/v1/tags/{tagId}/tasks/uncompleted";
        ResponseEntity<TaskResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                TaskResponse[].class, port, 1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        TaskResponse[] tasks = response.getBody();
        assertTrue(tasks != null && tasks.length > 0);
    }

    private void addCookieHeader(HttpHeaders headers) {
        headers.add(HttpHeaders.COOKIE, "access_token=" + TestAccessTokens.JOHN_DOE);
    }
}

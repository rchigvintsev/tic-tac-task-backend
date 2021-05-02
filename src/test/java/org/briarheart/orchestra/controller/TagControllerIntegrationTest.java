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
class TagControllerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void shouldReturnUncompletedTasks() {
        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);

        String url = "http://localhost:{port}/v1/tags/{tagId}/tasks/uncompleted";
        ResponseEntity<Task[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                Task[].class, port, 1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Task[] tasks = response.getBody();
        assertTrue(tasks != null && tasks.length > 0);
    }

    private void addCookieHeader(HttpHeaders headers) {
        headers.add(HttpHeaders.COOKIE, "access_token=" + TestAccessTokens.JOHN_DOE);
    }
}

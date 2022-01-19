package org.briarheart.tictactask.task.tag;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "security.disabled=false")
@Import(TestJavaMailSenderConfiguration.class)
@ActiveProfiles("test")
class TaskTagControllerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

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

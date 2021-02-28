package org.briarheart.orchestra.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.util.TestAccessTokens;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "security.disabled=false")
@Import(UserControllerIntegrationTest.TestJavaMailSenderConfiguration.class)
@ActiveProfiles("test")
class UserControllerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    @Test
    void shouldCreateUser() throws Exception {
        User newUser = User.builder().email("alice@mail.com").fullName("Alice").password("secret").build();
        String requestBody = objectMapper.writeValueAsString(newUser);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        ResponseEntity<Void> response = restTemplate.exchange("http://localhost:{port}/users",
                HttpMethod.POST, new HttpEntity<>(requestBody, headers), Void.class, port);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void shouldDenyAccessToUserCreationServiceWhenCurrentUserIsAuthenticated() throws Exception {
        User newUser = User.builder().email("john.doe@mail.com").fullName("John Doe").password("secret").build();
        String requestBody = objectMapper.writeValueAsString(newUser);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "access_token=" + TestAccessTokens.JOHN_DOE);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        ResponseEntity<Void> response = restTemplate.exchange("http://localhost:{port}/users",
                HttpMethod.POST, new HttpEntity<>(requestBody, headers), Void.class, port);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    public static class TestJavaMailSenderConfiguration {
        @Bean
        public JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }
}

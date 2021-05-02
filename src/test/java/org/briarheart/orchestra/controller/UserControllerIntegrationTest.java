package org.briarheart.orchestra.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.util.TestAccessTokens;
import org.briarheart.orchestra.util.TestUsers;
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

import static org.junit.jupiter.api.Assertions.*;
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
    @Autowired
    private UserRepository userRepository;


    @LocalServerPort
    private int port;

    @Test
    void shouldCreateUser() throws Exception {
        User newUser = User.builder().email("alice@mail.com").fullName("Alice").password("secret").build();
        String requestBody = objectMapper.writeValueAsString(newUser);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        ResponseEntity<User> response = restTemplate.exchange("http://localhost:{port}/v1/users",
                HttpMethod.POST, new HttpEntity<>(requestBody, headers), User.class, port);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void shouldDenyAccessToUserCreationServiceWhenCurrentUserIsAuthenticated() throws Exception {
        String requestBody = objectMapper.writeValueAsString(TestUsers.JOHN_DOE);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "access_token=" + TestAccessTokens.JOHN_DOE);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        ResponseEntity<String> response = restTemplate.exchange("http://localhost:{port}/v1/users",
                HttpMethod.POST, new HttpEntity<>(requestBody, headers), String.class, port);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void shouldConfirmUserEmail() {
        long userId = TestUsers.JANE_DOE.getId();
        String token = "4b1f7955-a406-4d36-8cbe-d6c61f39e27d";

        String url = "http://localhost:{port}/v1/users/{userId}/email/confirmation/{token}";
        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, null, Void.class, port, userId,
                token);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        User user = userRepository.findById(userId).block();
        assertNotNull(user);
        assertTrue(user.isEmailConfirmed());
        assertTrue(user.isEnabled());
    }

    @Test
    void shouldConfirmPasswordReset() {
        long userId = TestUsers.JOHN_DOE.getId();
        String token = "cf575578-cddf-4773-b1e0-5f37cbb0a8d9";

        String url = "http://localhost:{port}/v1/users/{userId}/password/reset/confirmation/{token}";

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity<>("password=qwerty", headers), Void.class, port, userId, token);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    public static class TestJavaMailSenderConfiguration {
        @Bean
        public JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }
}

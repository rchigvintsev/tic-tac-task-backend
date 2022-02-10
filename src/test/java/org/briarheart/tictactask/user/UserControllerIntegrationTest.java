package org.briarheart.tictactask.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.briarheart.tictactask.user.UserController.CreateUserRequest;
import org.briarheart.tictactask.util.TestAccessTokens;
import org.briarheart.tictactask.util.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roman Chigvintsev
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase
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
        CreateUserRequest createRequest = new CreateUserRequest();
        createRequest.setEmail("alice@mail.com");
        createRequest.setFullName("Alice");
        createRequest.setPassword("secret");
        String requestBody = objectMapper.writeValueAsString(createRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        ResponseEntity<User> response = restTemplate.exchange("http://localhost:{port}/api/v1/users",
                HttpMethod.POST, new HttpEntity<>(requestBody, headers), User.class, port);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void shouldDenyAccessToUserCreationServiceWhenCurrentUserIsAuthenticated() throws Exception {
        CreateUserRequest createRequest = new CreateUserRequest();
        createRequest.setEmail(TestUsers.JANE_DOE.getEmail());
        createRequest.setFullName(TestUsers.JOHN_DOE.getFullName());
        createRequest.setPassword(TestUsers.JOHN_DOE.getPassword());
        String requestBody = objectMapper.writeValueAsString(createRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "access_token=" + TestAccessTokens.JOHN_DOE);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        ResponseEntity<String> response = restTemplate.exchange("http://localhost:{port}/api/v1/users",
                HttpMethod.POST, new HttpEntity<>(requestBody, headers), String.class, port);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void shouldConfirmUserEmail() {
        long userId = TestUsers.JANE_DOE.getId();
        String token = "4b1f7955-a406-4d36-8cbe-d6c61f39e27d";

        String url = "http://localhost:{port}/api/v1/users/{userId}/email/confirmation/{token}";
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

        String url = "http://localhost:{port}/api/v1/users/{userId}/password/reset/confirmation/{token}";

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity<>("password=qwerty", headers), Void.class, port, userId, token);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldSaveProfilePicture() {
        long userId = TestUsers.JOHN_DOE.getId();
        String url = "http://localhost:{port}/api/v1/users/{userId}/profile-picture";

        HttpHeaders headers = new HttpHeaders();
        addCookieHeader(headers);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("profilePicture", new ClassPathResource("test-image.png"))
                .filename("test-image.png")
                .contentType(MediaType.IMAGE_PNG);
        MultiValueMap<String, HttpEntity<?>> form = multipartBodyBuilder.build();

        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(form, headers),
                Void.class, port, userId);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    private void addCookieHeader(HttpHeaders headers) {
        headers.add(HttpHeaders.COOKIE, "access_token=" + TestAccessTokens.JOHN_DOE);
    }
}

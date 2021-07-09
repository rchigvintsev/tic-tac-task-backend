package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.config.PermitAllSecurityConfig;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.ProfilePicture;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.service.EmailConfirmationService;
import org.briarheart.orchestra.service.PasswordService;
import org.briarheart.orchestra.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
@WebFluxTest(UserController.class)
@Import(PermitAllSecurityConfig.class)
class UserControllerTest {
    private static final String TEST_JPEG = "/9j/4AAQSkZJRgABAQEAeAB4AAD/4QAiRXhpZgAATU0AKgAAAAgAAQESAAMAAAABAAEAAAAAA"
            + "AD/2wBDAAIBAQIBAQICAgICAgICAwUDAwMDAwYEBAMFBwYHBwcGBwcICQsJCAgKCAcHCg0KCgsMDAwMBwkODw0MDgsMDAz/2wBDAQIC"
            + "AgMDAwYDAwYMCAcIDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAz/wAARCAAyADIDASIAAhE"
            + "BAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFD"
            + "KBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJi"
            + "pKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEB"
            + "AQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnL"
            + "RChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpa"
            + "anqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwDHooor++D/ACjCiiigA"
            + "ooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKAP/9k=";

    @Autowired
    private WebTestClient testClient;

    @MockBean
    private UserService userService;
    @MockBean
    private EmailConfirmationService emailConfirmationService;
    @MockBean
    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        when(userService.updateUser(any(User.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));
    }

    @Test
    void shouldCreateUser() {
        User user = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userService.createUser(any(User.class), eq(Locale.ENGLISH))).thenReturn(Mono.just(user));

        testClient.mutateWith(csrf())
                .post().uri("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Accept-Language", "en")
                .bodyValue(user)
                .exchange()

                .expectStatus().isCreated();
        verify(userService, times(1)).createUser(any(User.class), eq(Locale.ENGLISH));
    }

    @Test
    void shouldConfirmEmail() {
        long userId = 1L;
        String token = "K1Mb2ByFcfYndPmuFijB";
        when(emailConfirmationService.confirmEmail(userId, token)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(csrf())
                .post().uri("/v1/users/{userId}/email/confirmation/{token}", userId, token)
                .exchange()

                .expectStatus().isOk();
        verify(emailConfirmationService, times(1)).confirmEmail(userId, token);
    }

    @Test
    void shouldResetPassword() {
        String email = "alice@mail.com";
        when(passwordService.resetPassword(email, Locale.ENGLISH)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(csrf())
                .post().uri("/v1/users/password/reset")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("email=" + email)
                .header("Accept-Language", "en")
                .exchange()

                .expectStatus().isOk();
        verify(passwordService, times(1)).resetPassword(email, Locale.ENGLISH);
    }

    @Test
    void shouldReturnBadRequestStatusCodeOnPasswordResetWhenEmailFormParameterIsNotProvided() {
        testClient.mutateWith(csrf())
                .post().uri("/v1/users/password/reset")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Accept-Language", "en")
                .exchange()

                .expectStatus().isBadRequest();
    }

    @Test
    void shouldConfirmPasswordReset() {
        long userId = 1L;
        String token = "K1Mb2ByFcfYndPmuFijB";
        String newPassword = "qwerty";
        when(passwordService.confirmPasswordReset(userId, token, newPassword)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(csrf())
                .post().uri("/v1/users/{userId}/password/reset/confirmation/{token}", userId, token)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("password=" + newPassword)
                .exchange()

                .expectStatus().isOk();
        verify(passwordService, times(1)).confirmPasswordReset(userId, token, newPassword);
    }

    @Test
    void shouldUpdateUser() {
        User authenticatedUser = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(authenticatedUser);

        User updatedUser = User.builder()
                .id(authenticatedUser.getId())
                .email("alice.wonderland@mail.com")
                .fullName("Alice Wonderland")
                .build();

        User expectedResult = new User(updatedUser);
        expectedResult.setEmail(authenticatedUser.getEmail());
        expectedResult.setAuthorities(new ArrayList<>());

        testClient.mutateWith(csrf()).mutateWith(mockAuthentication(authenticationMock))
                .put().uri("/v1/users/{userId}", updatedUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedUser)
                .exchange()

                .expectStatus().isOk()
                .expectBody(User.class).isEqualTo(expectedResult);
    }

    @Test
    void shouldReturnProfilePicture() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        ProfilePicture profilePicture = ProfilePicture.builder()
                .userId(user.getId())
                .data(Base64.getDecoder().decode(TEST_JPEG))
                .type(MediaType.IMAGE_JPEG.toString())
                .build();
        when(userService.getProfilePicture(user.getId())).thenReturn(Mono.just(profilePicture));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/v1/users/{userId}/profile-picture", user.getId())
                .exchange()

                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_JPEG)
                .expectBody(byte[].class).isEqualTo(profilePicture.getData());
    }

    @Test
    void shouldReturnNotFoundStatusCodeWhenProfilePictureIsNotFound() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        String errorMessage = "Image is not found";
        when(userService.getProfilePicture(user.getId()))
                .thenReturn(Mono.error(new EntityNotFoundException(errorMessage)));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/v1/users/{userId}/profile-picture", user.getId()).exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo(errorMessage);
    }

    @Test
    void shouldSaveProfilePicture() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        when(userService.saveProfilePicture(any(ProfilePicture.class)))
                .thenAnswer(args -> Mono.just(args.getArgument(0)));

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("profilePicture", new ByteArrayResource(Base64.getDecoder().decode(TEST_JPEG)))
                .filename("test.jpg")
                .contentType(MediaType.IMAGE_JPEG);
        MultiValueMap<String, HttpEntity<?>> form = multipartBodyBuilder.build();

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .put().uri("/v1/users/{userId}/profile-picture", user.getId())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(form)
                .exchange()

                .expectStatus().isNoContent();
    }

    @Test
    void shouldUpdateUserProfilePictureUrlOnProfilePictureSave() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        when(userService.saveProfilePicture(any(ProfilePicture.class)))
                .thenAnswer(args -> Mono.just(args.getArgument(0)));

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("profilePicture", new ByteArrayResource(Base64.getDecoder().decode(TEST_JPEG)))
                .filename("test.jpg")
                .contentType(MediaType.IMAGE_JPEG);
        MultiValueMap<String, HttpEntity<?>> form = multipartBodyBuilder.build();

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .put().uri("/v1/users/{userId}/profile-picture", user.getId())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(form)
                .exchange();

        assertEquals("/v1/users/" + user.getId() + "/profile-picture", user.getProfilePictureUrl());
    }

    private Authentication createAuthentication(User user) {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn(user.getEmail());
        when(authenticationMock.getPrincipal()).thenReturn(user);
        return authenticationMock;
    }
}

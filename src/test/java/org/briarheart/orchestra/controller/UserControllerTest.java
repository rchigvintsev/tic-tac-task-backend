package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.config.PermitAllSecurityConfig;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.service.EmailConfirmationService;
import org.briarheart.orchestra.service.PasswordService;
import org.briarheart.orchestra.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
@WebFluxTest(UserController.class)
@Import(PermitAllSecurityConfig.class)
class UserControllerTest {
    @Autowired
    private WebTestClient testClient;

    @MockBean
    private UserService userService;
    @MockBean
    private EmailConfirmationService emailConfirmationService;
    @MockBean
    private PasswordService passwordService;

    @Test
    void shouldCreateUser() {
        User user = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userService.createUser(any(User.class), eq(Locale.ENGLISH))).thenReturn(Mono.just(user));

        testClient.mutateWith(csrf())
                .post().uri("/users")
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
                .post().uri("/users/" + userId + "/email/confirmation/" + token)
                .exchange()

                .expectStatus().isOk();
        verify(emailConfirmationService, times(1)).confirmEmail(userId, token);
    }

    @Test
    void shouldResetPassword() {
        String email = "alice@mail.com";
        when(userService.resetPassword(email, Locale.ENGLISH)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(csrf())
                .post().uri("/users/password/reset")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("email=" + email)
                .header("Accept-Language", "en")
                .exchange()

                .expectStatus().isOk();
        verify(userService, times(1)).resetPassword(email, Locale.ENGLISH);
    }

    @Test
    void shouldReturnBadRequestStatusCodeOnPasswordResetWhenEmailFormParameterIsNotProvided() {
        testClient.mutateWith(csrf())
                .post().uri("/users/password/reset")
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
                .post().uri("/users/" + userId + "/password/reset/confirmation/" + token)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("password=" + newPassword)
                .exchange()

                .expectStatus().isOk();
        verify(passwordService, times(1)).confirmPasswordReset(userId, token, newPassword);
    }
}

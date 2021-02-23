package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.config.PermitAllSecurityConfig;
import org.briarheart.orchestra.model.User;
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

    @Test
    void shouldCreateUser() {
        User user = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userService.createUser(any(User.class), eq(Locale.ENGLISH))).thenReturn(Mono.just(user));

        testClient.mutateWith(csrf()).post()
                .uri("/users")
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
        when(userService.confirmEmail(userId, token)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(csrf()).put()
                .uri("/users/" + userId + "/email/confirmation/" + token)
                .exchange()

                .expectStatus().isOk();
        verify(userService, times(1)).confirmEmail(userId, token);
    }
}

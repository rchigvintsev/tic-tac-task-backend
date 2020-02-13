package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.config.PermitAllSecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
@WebFluxTest(TestController.class)
@Import(PermitAllSecurityConfig.class)
class GlobalExceptionHandlingControllerAdviceTest {
    @Autowired
    private WebTestClient testClient;

    @Test
    void shouldGetBadRequestResponseStatusInCaseOfWebExchangeBindException() {
        testClient.mutateWith(csrf())
                .post()
                .uri("/webExchangeBindException")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldGetErrorMessagesInResponseBodyInCaseOfWebExchangeBindException() {
        testClient.mutateWith(csrf())
                .post()
                .uri("/webExchangeBindException")
                .exchange()
                .expectBody().jsonPath("$.errors").exists();
    }
}

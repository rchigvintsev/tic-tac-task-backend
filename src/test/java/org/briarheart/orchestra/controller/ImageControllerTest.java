package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.config.PermitAllSecurityConfig;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.Image;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.service.ImageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
@WebFluxTest(ImageController.class)
@Import(PermitAllSecurityConfig.class)
class ImageControllerTest {
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
    private ImageService imageService;

    @Test
    void shouldReturnImageById() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        Image image = Image.builder()
                .id(2L)
                .userId(user.getId())
                .imageData(Base64.getDecoder().decode(TEST_JPEG))
                .build();
        when(imageService.getImage(image.getId(), user)).thenReturn(Mono.just(image));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/v1/images/" + image.getId())
                .exchange()

                .expectStatus().isOk()
                .expectBody(byte[].class).isEqualTo(image.getImageData());
    }

    @Test
    void shouldReturnNotFoundStatusCodeWhenImageIsNotFoundById() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        String errorMessage = "Image is not found";
        when(imageService.getImage(anyLong(), any(User.class)))
                .thenReturn(Mono.error(new EntityNotFoundException(errorMessage)));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/v1/images/2").exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo(errorMessage);
    }

    private Authentication createAuthentication(User user) {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn(user.getEmail());
        when(authenticationMock.getPrincipal()).thenReturn(user);
        return authenticationMock;
    }
}

package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.config.PermitAllSecurityConfig;
import org.briarheart.orchestra.model.Tag;
import org.briarheart.orchestra.service.TagService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
@WebFluxTest(TagController.class)
@Import(PermitAllSecurityConfig.class)
class TagControllerTest {
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();

    @Autowired
    private WebTestClient testClient;

    @MockBean
    private TagService tagService;

    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @AfterAll
    static void afterAll() {
        Locale.setDefault(DEFAULT_LOCALE);
    }

    @Test
    void shouldReturnAllTags() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Tag tag = Tag.builder().id(1L).name("Test tag").author(authenticationMock.getName()).build();
        when(tagService.getTags(eq(authenticationMock.getName()), any())).thenReturn(Flux.just(tag));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tags").exchange()
                .expectStatus().isOk()
                .expectBody(Tag[].class).isEqualTo(new Tag[] {tag});
    }

    @Test
    void shouldUpdateTag() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Tag tag = Tag.builder().id(1L).name("Test tag").build();
        Tag updatedTag = tag.copy();
        updatedTag.setId(1L);
        updatedTag.setName("Updated test tag");
        Mockito.when(tagService.updateTag(tag, tag.getId(), authenticationMock.getName()))
                .thenReturn(Mono.just(updatedTag));

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf()).put()
                .uri("/tags/" + tag.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tag)
                .exchange()

                .expectStatus().isOk()
                .expectBody(Tag.class).isEqualTo(updatedTag);
    }

    @Test
    void shouldDeleteTag() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        long tagId = 1L;
        Mockito.when(tagService.deleteTag(tagId, authenticationMock.getName())).thenReturn(Mono.empty());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf()).delete()
                .uri("/tags/" + tagId).exchange()
                .expectStatus().isNoContent();
    }
}

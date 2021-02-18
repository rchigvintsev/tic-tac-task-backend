package org.briarheart.orchestra.controller;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.Tag;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.service.TagService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.net.URI;

/**
 * REST-controller for task tag managing.
 *
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController extends AbstractController {
    private final TagService tagService;

    @GetMapping
    public Flux<Tag> getTags(Authentication authentication) {
        return tagService.getTags(getUser(authentication));
    }

    @GetMapping("/{id}")
    public Mono<Tag> getTag(@PathVariable Long id, Authentication authentication) {
        return tagService.getTag(id, getUser(authentication));
    }

    @PostMapping
    public Mono<ResponseEntity<Tag>> createTag(@Valid @RequestBody Tag tag,
                                               Authentication authentication,
                                               ServerHttpRequest request) {
        tag.setUserId(getUser(authentication).getId());
        return tagService.createTag(tag).map(createdTag -> {
            URI tagLocation = UriComponentsBuilder.fromHttpRequest(request)
                    .path("/{id}")
                    .buildAndExpand(createdTag.getId())
                    .toUri();
            return ResponseEntity.created(tagLocation).body(createdTag);
        });
    }

    @PutMapping("/{id}")
    public Mono<Tag> updateTag(@Valid @RequestBody Tag tag, @PathVariable Long id, Authentication authentication) {
        tag.setId(id);
        tag.setUserId(getUser(authentication).getId());

        return tagService.updateTag(tag);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteTag(@PathVariable Long id, Authentication authentication) {
        return tagService.deleteTag(id, getUser(authentication));
    }

    @GetMapping("/{tagId}/tasks/uncompleted")
    public Flux<Task> getUncompletedTasks(@PathVariable("tagId") Long tagId,
                                          Authentication authentication,
                                          Pageable pageable) {
        return tagService.getUncompletedTasks(tagId, getUser(authentication), pageable);
    }
}

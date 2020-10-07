package org.briarheart.orchestra.controller;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.Tag;
import org.briarheart.orchestra.service.TagService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.security.Principal;

/**
 * REST-controller for task tag managing.
 *
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;

    @GetMapping
    public Flux<Tag> getTags(Principal user) {
        return tagService.getTags(user.getName());
    }

    @PutMapping("/{id}")
    public Mono<Tag> updateTag(@Valid @RequestBody Tag tag, @PathVariable Long id, Principal user) {
        return tagService.updateTag(tag, id, user.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteTag(@PathVariable Long id, Principal user) {
        return tagService.deleteTag(id, user.getName());
    }
}

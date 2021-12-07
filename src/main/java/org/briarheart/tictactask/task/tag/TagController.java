package org.briarheart.tictactask.task.tag;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.briarheart.tictactask.controller.AbstractController;
import org.briarheart.tictactask.task.TaskController.TaskResponse;
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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.net.URI;

/**
 * REST-controller for task tag managing.
 *
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController extends AbstractController {
    private final TagService tagService;

    @GetMapping
    public Flux<TagResponse> getTags(Authentication authentication) {
        return tagService.getTags(getUser(authentication)).map(TagResponse::new);
    }

    @GetMapping("/{id}")
    public Mono<TagResponse> getTag(@PathVariable Long id, Authentication authentication) {
        return tagService.getTag(id, getUser(authentication)).map(TagResponse::new);
    }

    @PostMapping
    public Mono<ResponseEntity<TagResponse>> createTag(@Valid @RequestBody CreateTagRequest createRequest,
                                                       Authentication authentication,
                                                       ServerHttpRequest request) {
        Tag tag = createRequest.toTag();
        tag.setUserId(getUser(authentication).getId());
        return tagService.createTag(tag).map(createdTag -> {
            URI tagLocation = UriComponentsBuilder.fromHttpRequest(request)
                    .path("/{id}")
                    .buildAndExpand(createdTag.getId())
                    .toUri();
            return ResponseEntity.created(tagLocation).body(new TagResponse(tag));
        });
    }

    @PutMapping("/{id}")
    public Mono<TagResponse> updateTag(@Valid @RequestBody UpdateTagRequest updateRequest,
                                       @PathVariable Long id,
                                       Authentication authentication) {
        Tag tag = updateRequest.toTag();
        tag.setId(id);
        tag.setUserId(getUser(authentication).getId());
        return tagService.updateTag(tag).map(TagResponse::new);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteTag(@PathVariable Long id, Authentication authentication) {
        return tagService.deleteTag(id, getUser(authentication));
    }

    @GetMapping("/{tagId}/tasks/uncompleted")
    public Flux<TaskResponse> getUncompletedTasks(@PathVariable("tagId") Long tagId,
                                                  Authentication authentication,
                                                  Pageable pageable) {
        return tagService.getUncompletedTasks(tagId, getUser(authentication), pageable).map(TaskResponse::new);
    }

    @Data
    @NoArgsConstructor
    public static class TagResponse {
        private Long id;
        private String name;
        private Integer color;

        public TagResponse(Tag tag) {
            this.id = tag.getId();
            this.name = tag.getName();
            this.color = tag.getColor();
        }
    }

    @Data
    public abstract static class CreateOrUpdateTagRequest {
        @NotBlank
        @Size(max = 50)
        private String name;
        private Integer color;

        public Tag toTag() {
            return Tag.builder().name(name).color(color).build();
        }
    }

    public static class CreateTagRequest extends CreateOrUpdateTagRequest {
    }

    public static class UpdateTagRequest extends CreateOrUpdateTagRequest {
    }
}

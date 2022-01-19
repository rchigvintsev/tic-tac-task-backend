package org.briarheart.tictactask.task.tag;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Task tags", description = "Allows to manage task tags, get tasks by tag")
public class TaskTagController extends AbstractController {
    private final TaskTagService taskTagService;

    @GetMapping
    @Operation(summary = "Get all tags", description = "Returns all tags created by current user")
    public Flux<TaskTagResponse> getTags(Authentication authentication) {
        return taskTagService.getTags(getUser(authentication)).map(TaskTagResponse::new);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tag by id", description = "Returns tag by id")
    public Mono<TaskTagResponse> getTag(@PathVariable Long id, Authentication authentication) {
        return taskTagService.getTag(id, getUser(authentication)).map(TaskTagResponse::new);
    }

    @PostMapping
    @Operation(summary = "Create new tag", description = "Allows to create new tag")
    public Mono<ResponseEntity<TaskTagResponse>> createTag(@Valid @RequestBody CreateTagRequest createRequest,
                                                           Authentication authentication,
                                                           ServerHttpRequest request) {
        TaskTag tag = createRequest.toTag();
        tag.setUserId(getUser(authentication).getId());
        return taskTagService.createTag(tag).map(createdTag -> {
            URI tagLocation = UriComponentsBuilder.fromHttpRequest(request)
                    .path("/{id}")
                    .buildAndExpand(createdTag.getId())
                    .toUri();
            return ResponseEntity.created(tagLocation).body(new TaskTagResponse(createdTag));
        });
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update tag", description = "Allows to update tag")
    public Mono<TaskTagResponse> updateTag(@Valid @RequestBody UpdateTagRequest updateRequest,
                                           @PathVariable Long id,
                                           Authentication authentication) {
        TaskTag tag = updateRequest.toTag();
        tag.setId(id);
        tag.setUserId(getUser(authentication).getId());
        return taskTagService.updateTag(tag).map(TaskTagResponse::new);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete tag", description = "Allows to delete tag")
    public Mono<Void> deleteTag(@PathVariable Long id, Authentication authentication) {
        return taskTagService.deleteTag(id, getUser(authentication));
    }

    @GetMapping("/{tagId}/tasks/uncompleted")
    @Operation(
            summary = "Get uncompleted tasks with tag",
            description = "Returns uncompleted tasks with particular tag assigned"
    )
    public Flux<TaskResponse> getUncompletedTasks(@PathVariable("tagId") Long tagId,
                                                  Authentication authentication,
                                                  Pageable pageable) {
        return taskTagService.getUncompletedTasks(tagId, getUser(authentication), pageable).map(TaskResponse::new);
    }

    @Data
    @NoArgsConstructor
    public static class TaskTagResponse {
        private Long id;
        private String name;
        private Integer color;

        public TaskTagResponse(TaskTag tag) {
            this.id = tag.getId();
            this.name = tag.getName();
            this.color = tag.getColor();
        }
    }

    @Data
    public abstract static class CreateOrUpdateTaskTagRequest {
        @NotBlank
        @Size(max = 50)
        private String name;
        private Integer color;

        public TaskTag toTag() {
            return TaskTag.builder().name(name).color(color).build();
        }
    }

    public static class CreateTagRequest extends CreateOrUpdateTaskTagRequest {
    }

    public static class UpdateTagRequest extends CreateOrUpdateTaskTagRequest {
    }
}

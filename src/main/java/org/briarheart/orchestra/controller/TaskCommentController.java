package org.briarheart.orchestra.controller;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.TaskComment;
import org.briarheart.orchestra.service.TaskCommentService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.net.URI;
import java.security.Principal;

/**
 * REST-controller for task comment managing.
 *
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/taskComments")
@RequiredArgsConstructor
public class TaskCommentController {
    private final TaskCommentService taskCommentService;

    @GetMapping
    public Flux<TaskComment> getComments(@RequestParam("taskId") Long taskId, Principal user, Pageable pageable) {
        return taskCommentService.getComments(taskId, user.getName(), pageable);
    }

    @PostMapping
    public Mono<ResponseEntity<TaskComment>> createComment(@Valid @RequestBody TaskComment comment,
                                                           @RequestParam("taskId") Long taskId,
                                                           Principal user,
                                                           ServerHttpRequest request) {
        return taskCommentService.createComment(comment, user.getName(), taskId).map(createdComment -> {
            URI commentLocation = UriComponentsBuilder.fromHttpRequest(request)
                    .path("/{id}")
                    .replaceQueryParams(null)
                    .buildAndExpand(createdComment.getId())
                    .toUri();
            return ResponseEntity.created(commentLocation).body(createdComment);
        });
    }

    @PutMapping("/{id}")
    public Mono<TaskComment> updateComment(@Valid @RequestBody TaskComment comment,
                                           @PathVariable Long id,
                                           Principal user) {
        return taskCommentService.updateComment(comment, id, user.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteComment(@PathVariable Long id, Principal user) {
        return taskCommentService.deleteComment(id, user.getName());
    }
}

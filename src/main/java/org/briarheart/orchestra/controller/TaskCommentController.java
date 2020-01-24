package org.briarheart.orchestra.controller;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.TaskComment;
import org.briarheart.orchestra.service.TaskCommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Flux<TaskComment> getComments(@RequestParam(name = "taskId") Long taskId, Principal user) {
        return taskCommentService.getComments(taskId, user.getName());
    }

    @PostMapping
    public Mono<ResponseEntity<TaskComment>> createComment(@RequestBody TaskComment comment,
                                                           @RequestParam(name = "taskId") Long taskId,
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
    public Mono<TaskComment> updateComment(@RequestBody TaskComment comment, @PathVariable Long id, Principal user) {
        return taskCommentService.updateComment(comment, id, user.getName());
    }

    @DeleteMapping
    @RequestMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteComment(@PathVariable Long id, Principal user) {
        return taskCommentService.deleteComment(id, user.getName());
    }
}

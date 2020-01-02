package org.briarheart.orchestra.controller;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.TaskComment;
import org.briarheart.orchestra.service.TaskCommentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TaskComment> createComment(@RequestBody TaskComment comment,
                                           @RequestParam(name = "taskId") Long taskId,
                                           Principal user) {
        return taskCommentService.createComment(comment, user.getName(), taskId);
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

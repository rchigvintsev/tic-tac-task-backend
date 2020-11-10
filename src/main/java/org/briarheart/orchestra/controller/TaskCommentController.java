package org.briarheart.orchestra.controller;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.TaskComment;
import org.briarheart.orchestra.service.TaskCommentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.security.Principal;

/**
 * REST-controller for task comment managing.
 *
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/task-comments")
@RequiredArgsConstructor
public class TaskCommentController {
    private final TaskCommentService taskCommentService;

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

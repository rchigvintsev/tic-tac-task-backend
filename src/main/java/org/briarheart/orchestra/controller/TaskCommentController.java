package org.briarheart.orchestra.controller;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.TaskComment;
import org.briarheart.orchestra.service.TaskCommentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

/**
 * REST-controller for task comment managing.
 *
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/v1/task-comments")
@RequiredArgsConstructor
public class TaskCommentController extends AbstractController {
    private final TaskCommentService taskCommentService;

    @PutMapping("/{id}")
    public Mono<TaskComment> updateComment(@Valid @RequestBody TaskComment comment,
                                           @PathVariable Long id,
                                           Authentication authentication) {
        comment.setId(id);
        comment.setUserId(getUser(authentication).getId());
        return taskCommentService.updateComment(comment);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteComment(@PathVariable Long id, Authentication authentication) {
        return taskCommentService.deleteComment(id, getUser(authentication));
    }
}

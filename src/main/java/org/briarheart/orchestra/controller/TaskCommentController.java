package org.briarheart.orchestra.controller;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.TaskCommentRepository;
import org.briarheart.orchestra.model.TaskComment;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/taskComments")
@RequiredArgsConstructor
public class TaskCommentController {
    private final TaskCommentRepository taskCommentRepository;

    @GetMapping
    public Flux<TaskComment> getComments(@RequestParam(name = "taskId") Long taskId) {
        return taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
    }

    @PostMapping
    public Mono<TaskComment> createComment(@RequestBody TaskComment comment) {
        return taskCommentRepository.save(comment);
    }

    @DeleteMapping
    @RequestMapping("/{id}")
    public Mono<Void> deleteComment(@PathVariable Long id) {
        return taskCommentRepository.deleteById(id);
    }
}

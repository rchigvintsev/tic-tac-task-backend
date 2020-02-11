package org.briarheart.orchestra.controller;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.service.TaskService;
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
 * REST-controller for task managing.
 *
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @GetMapping
    public Flux<Task> getTasks(@RequestParam(name = "completed", defaultValue = "false") Boolean completed,
                               Principal user) {
        if (completed) {
            return taskService.getCompletedTasks(user.getName());
        }
        return taskService.getUncompletedTasks(user.getName());
    }

    @GetMapping("/{id}")
    public Mono<Task> getTask(@PathVariable("id") Long id, Principal user) {
        return taskService.getTask(id, user.getName());
    }

    @PostMapping
    public Mono<ResponseEntity<Task>> createTask(@Valid @RequestBody Task task,
                                                 Principal user,
                                                 ServerHttpRequest request) {
        return taskService.createTask(task, user.getName()).map(createdTask -> {
            URI taskLocation = UriComponentsBuilder.fromHttpRequest(request)
                    .path("/{id}")
                    .buildAndExpand(createdTask.getId())
                    .toUri();
            return ResponseEntity.created(taskLocation).body(createdTask);
        });
    }

    @PutMapping("/{id}")
    public Mono<Task> updateTask(@Valid @RequestBody Task task, @PathVariable Long id, Principal user) {
        return taskService.updateTask(task, id, user.getName());
    }
}

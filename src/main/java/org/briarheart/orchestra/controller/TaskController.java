package org.briarheart.orchestra.controller;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;

/**
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
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Task> createTask(@RequestBody Task task, Principal user) {
        return taskService.createTask(task, user.getName());
    }

    @PutMapping("/{id}")
    public Mono<Task> updateTask(@RequestBody Task task, @PathVariable("id") Long id, Principal user) {
        return taskService.updateTask(task, id, user.getName());
    }
}

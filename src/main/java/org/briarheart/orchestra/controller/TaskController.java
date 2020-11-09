package org.briarheart.orchestra.controller;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.Tag;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskComment;
import org.briarheart.orchestra.service.TaskService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.time.LocalDateTime;

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

    @GetMapping("/unprocessed")
    public Flux<Task> getUnprocessedTasks(Principal user, Pageable pageable) {
        return taskService.getUnprocessedTasks(user.getName(), pageable);
    }

    @GetMapping("/unprocessed/count")
    public Mono<Long> getUnprocessedTaskCount(Principal user) {
        return taskService.getUnprocessedTaskCount(user.getName());
    }

    @GetMapping("/processed")
    public Flux<Task> getProcessedTasks(
            @RequestParam(name = "deadlineFrom", required = false) LocalDateTime deadlineFrom,
            @RequestParam(name = "deadlineTo", required = false) LocalDateTime deadlineTo,
            Principal user,
            ServerHttpRequest request,
            Pageable pageable
    ) {
        MultiValueMap<String, String> queryParams = request.getQueryParams();
        if (!queryParams.containsKey("deadlineFrom") && !queryParams.containsKey("deadlineTo")) {
            return taskService.getProcessedTasks(user.getName(), pageable);
        }
        return taskService.getProcessedTasks(deadlineFrom, deadlineTo, user.getName(), pageable);
    }

    @GetMapping("/processed/count")
    public Mono<Long> getProcessedTaskCount(
            @RequestParam(name = "deadlineFrom", required = false) LocalDateTime deadlineFrom,
            @RequestParam(name = "deadlineTo", required = false) LocalDateTime deadlineTo,
            Principal user,
            ServerHttpRequest request
    ) {
        MultiValueMap<String, String> queryParams = request.getQueryParams();
        if (!queryParams.containsKey("deadlineFrom") && !queryParams.containsKey("deadlineTo")) {
            return taskService.getProcessedTaskCount(user.getName());
        }
        return taskService.getProcessedTaskCount(deadlineFrom, deadlineTo, user.getName());
    }

    @GetMapping("/uncompleted")
    public Flux<Task> getUncompletedTasks(Principal user, Pageable pageable) {
        return taskService.getUncompletedTasks(user.getName(), pageable);
    }

    @GetMapping("/uncompleted/count")
    public Mono<Long> getUncompletedTaskCount(Principal user) {
        return taskService.getUncompletedTaskCount(user.getName());
    }

    @GetMapping("/{id}")
    public Mono<Task> getTask(@PathVariable("id") Long id, Principal user) {
        return taskService.getTask(id, user.getName());
    }

    @GetMapping("/{taskId}/tags")
    public Flux<Tag> getTags(@PathVariable("taskId") Long taskId, Principal user) {
        return taskService.getTags(taskId, user.getName());
    }

    @PutMapping("/{taskId}/tags/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> assignTag(@PathVariable("taskId") Long taskId,
                                @PathVariable("tagId") Long tagId,
                                Principal user) {
        return taskService.assignTag(taskId, tagId, user.getName());
    }

    @DeleteMapping("/{taskId}/tags/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeTag(@PathVariable("taskId") Long taskId,
                                @PathVariable("tagId") Long tagId,
                                Principal user) {
        return taskService.removeTag(taskId, user.getName(), tagId);
    }

    @GetMapping("/{taskId}/comments")
    public Flux<TaskComment> getComments(@PathVariable("taskId") Long taskId, Principal user, Pageable pageable) {
        return taskService.getComments(taskId, user.getName(), pageable);
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

    @PostMapping("/{id}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> completeTask(@PathVariable Long id, Principal user) {
        return taskService.completeTask(id, user.getName());
    }
}

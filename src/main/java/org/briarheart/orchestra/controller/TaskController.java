package org.briarheart.orchestra.controller;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.Tag;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskComment;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.service.TaskService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.net.URI;
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
    public Flux<Task> getUnprocessedTasks(Authentication authentication, Pageable pageable) {
        return taskService.getUnprocessedTasks(getUser(authentication), pageable);
    }

    @GetMapping("/unprocessed/count")
    public Mono<Long> getUnprocessedTaskCount(Authentication authentication) {
        return taskService.getUnprocessedTaskCount(getUser(authentication));
    }

    @GetMapping("/processed")
    public Flux<Task> getProcessedTasks(
            @RequestParam(name = "deadlineFrom", required = false) LocalDateTime deadlineFrom,
            @RequestParam(name = "deadlineTo", required = false) LocalDateTime deadlineTo,
            Authentication authentication,
            ServerHttpRequest request,
            Pageable pageable
    ) {
        MultiValueMap<String, String> queryParams = request.getQueryParams();
        if (!queryParams.containsKey("deadlineFrom") && !queryParams.containsKey("deadlineTo")) {
            return taskService.getProcessedTasks(getUser(authentication), pageable);
        }
        return taskService.getProcessedTasks(deadlineFrom, deadlineTo, getUser(authentication), pageable);
    }

    @GetMapping("/processed/count")
    public Mono<Long> getProcessedTaskCount(
            @RequestParam(name = "deadlineFrom", required = false) LocalDateTime deadlineFrom,
            @RequestParam(name = "deadlineTo", required = false) LocalDateTime deadlineTo,
            Authentication authentication,
            ServerHttpRequest request
    ) {
        MultiValueMap<String, String> queryParams = request.getQueryParams();
        if (!queryParams.containsKey("deadlineFrom") && !queryParams.containsKey("deadlineTo")) {
            return taskService.getProcessedTaskCount(getUser(authentication));
        }
        return taskService.getProcessedTaskCount(deadlineFrom, deadlineTo, getUser(authentication));
    }

    @GetMapping("/uncompleted")
    public Flux<Task> getUncompletedTasks(Authentication authentication, Pageable pageable) {
        return taskService.getUncompletedTasks(getUser(authentication), pageable);
    }

    @GetMapping("/uncompleted/count")
    public Mono<Long> getUncompletedTaskCount(Authentication authentication) {
        return taskService.getUncompletedTaskCount(getUser(authentication));
    }

    @GetMapping("/{id}")
    public Mono<Task> getTask(@PathVariable("id") Long id, Authentication authentication) {
        return taskService.getTask(id, getUser(authentication));
    }

    @PostMapping
    public Mono<ResponseEntity<Task>> createTask(@Valid @RequestBody Task task,
                                                 Authentication authentication,
                                                 ServerHttpRequest request) {
        task.setUserId(getUser(authentication).getId());
        return taskService.createTask(task).map(createdTask -> {
            URI taskLocation = UriComponentsBuilder.fromHttpRequest(request)
                    .path("/{id}")
                    .buildAndExpand(createdTask.getId())
                    .toUri();
            return ResponseEntity.created(taskLocation).body(createdTask);
        });
    }

    @PutMapping("/{id}")
    public Mono<Task> updateTask(@Valid @RequestBody Task task, @PathVariable Long id, Authentication authentication) {
        task.setId(id);
        task.setUserId(getUser(authentication).getId());
        return taskService.updateTask(task);
    }

    @PutMapping("/completed/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> completeTask(@PathVariable Long id, Authentication authentication) {
        return taskService.completeTask(id, getUser(authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteTask(@PathVariable Long id, Authentication authentication) {
        return taskService.deleteTask(id, getUser(authentication));
    }

    @GetMapping("/{taskId}/tags")
    public Flux<Tag> getTags(@PathVariable("taskId") Long taskId, Authentication authentication) {
        return taskService.getTags(taskId, getUser(authentication));
    }

    @PutMapping("/{taskId}/tags/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> assignTag(@PathVariable("taskId") Long taskId,
                                @PathVariable("tagId") Long tagId,
                                Authentication authentication) {
        return taskService.assignTag(taskId, tagId, getUser(authentication));
    }

    @DeleteMapping("/{taskId}/tags/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeTag(@PathVariable("taskId") Long taskId,
                                @PathVariable("tagId") Long tagId,
                                Authentication authentication) {
        return taskService.removeTag(taskId, tagId, getUser(authentication));
    }

    @GetMapping("/{taskId}/comments")
    public Flux<TaskComment> getComments(@PathVariable("taskId") Long taskId,
                                         Authentication authentication,
                                         Pageable pageable) {
        return taskService.getComments(taskId, getUser(authentication), pageable);
    }

    @PostMapping("/{taskId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TaskComment> addComment(@PathVariable("taskId") Long taskId,
                                        @Valid @RequestBody TaskComment comment,
                                        Authentication authentication) {
        comment.setId(null);
        comment.setUserId(getUser(authentication).getId());
        comment.setTaskId(taskId);

        return taskService.addComment(comment);
    }

    private User getUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}

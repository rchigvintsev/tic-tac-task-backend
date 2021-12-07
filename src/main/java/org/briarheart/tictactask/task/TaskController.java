package org.briarheart.tictactask.task;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.briarheart.tictactask.controller.AbstractController;
import org.briarheart.tictactask.task.comment.TaskComment;
import org.briarheart.tictactask.task.comment.TaskCommentController.CreateTaskCommentRequest;
import org.briarheart.tictactask.task.comment.TaskCommentController.TaskCommentResponse;
import org.briarheart.tictactask.task.recurrence.TaskRecurrenceStrategy;
import org.briarheart.tictactask.task.tag.TagController.TagResponse;
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
import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.net.URI;
import java.time.LocalDateTime;

/**
 * REST-controller for task managing.
 *
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController extends AbstractController {
    private final TaskService taskService;

    @GetMapping("/unprocessed")
    public Flux<TaskResponse> getUnprocessedTasks(Authentication authentication, Pageable pageable) {
        return taskService.getUnprocessedTasks(getUser(authentication), pageable).map(TaskResponse::new);
    }

    @GetMapping("/unprocessed/count")
    public Mono<Long> getUnprocessedTaskCount(Authentication authentication) {
        return taskService.getUnprocessedTaskCount(getUser(authentication));
    }

    @GetMapping("/processed")
    public Flux<TaskResponse> getProcessedTasks(
            @RequestParam(name = "deadlineFrom", required = false) LocalDateTime deadlineFrom,
            @RequestParam(name = "deadlineTo", required = false) LocalDateTime deadlineTo,
            Authentication authentication,
            ServerHttpRequest request,
            Pageable pageable
    ) {
        MultiValueMap<String, String> queryParams = request.getQueryParams();
        if (!queryParams.containsKey("deadlineFrom") && !queryParams.containsKey("deadlineTo")) {
            return taskService.getProcessedTasks(getUser(authentication), pageable).map(TaskResponse::new);
        }
        return taskService.getProcessedTasks(deadlineFrom, deadlineTo, getUser(authentication), pageable)
                .map(TaskResponse::new);
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
    public Flux<TaskResponse> getUncompletedTasks(Authentication authentication, Pageable pageable) {
        return taskService.getUncompletedTasks(getUser(authentication), pageable).map(TaskResponse::new);
    }

    @GetMapping("/uncompleted/count")
    public Mono<Long> getUncompletedTaskCount(Authentication authentication) {
        return taskService.getUncompletedTaskCount(getUser(authentication));
    }

    @GetMapping("/completed")
    public Flux<TaskResponse> getCompletedTasks(Authentication authentication, Pageable pageable) {
        return taskService.getCompletedTasks(getUser(authentication), pageable).map(TaskResponse::new);
    }

    @GetMapping("/{id}")
    public Mono<TaskResponse> getTask(@PathVariable("id") Long id, Authentication authentication) {
        return taskService.getTask(id, getUser(authentication)).map(TaskResponse::new);
    }

    @PostMapping
    public Mono<ResponseEntity<TaskResponse>> createTask(@Valid @RequestBody CreateTaskRequest createRequest,
                                                         Authentication authentication,
                                                         ServerHttpRequest request) {
        Task task = createRequest.toTask();
        task.setUserId(getUser(authentication).getId());
        return taskService.createTask(task).map(createdTask -> {
            URI taskLocation = UriComponentsBuilder.fromHttpRequest(request)
                    .path("/{id}")
                    .buildAndExpand(createdTask.getId())
                    .toUri();
            return ResponseEntity.created(taskLocation).body(new TaskResponse(createdTask));
        });
    }

    @PutMapping("/{id}")
    public Mono<TaskResponse> updateTask(@Valid @RequestBody UpdateTaskRequest updateRequest,
                                         @PathVariable Long id,
                                         Authentication authentication) {
        Task task = updateRequest.toTask();
        task.setId(id);
        task.setUserId(getUser(authentication).getId());
        return taskService.updateTask(task).map(TaskResponse::new);
    }

    @PutMapping("/completed/{id}")
    public Mono<TaskResponse> completeTask(@PathVariable Long id, Authentication authentication) {
        return taskService.completeTask(id, getUser(authentication)).map(TaskResponse::new);
    }

    @DeleteMapping("/completed/{id}")
    public Mono<TaskResponse> restoreTask(@PathVariable Long id, Authentication authentication) {
        return taskService.restoreTask(id, getUser(authentication)).map(TaskResponse::new);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteTask(@PathVariable Long id, Authentication authentication) {
        return taskService.deleteTask(id, getUser(authentication));
    }

    @GetMapping("/{taskId}/tags")
    public Flux<TagResponse> getTags(@PathVariable("taskId") Long taskId, Authentication authentication) {
        return taskService.getTags(taskId, getUser(authentication)).map(TagResponse::new);
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
    public Flux<TaskCommentResponse> getComments(@PathVariable("taskId") Long taskId,
                                                 Authentication authentication,
                                                 Pageable pageable) {
        return taskService.getComments(taskId, getUser(authentication), pageable).map(TaskCommentResponse::new);
    }

    @PostMapping("/{taskId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TaskCommentResponse> addComment(@PathVariable("taskId") Long taskId,
                                                @Valid @RequestBody CreateTaskCommentRequest createRequest,
                                                Authentication authentication) {
        TaskComment comment = createRequest.toTaskComment();
        comment.setUserId(getUser(authentication).getId());
        comment.setTaskId(taskId);
        return taskService.addComment(comment).map(TaskCommentResponse::new);
    }

    @Data
    @NoArgsConstructor
    public static class TaskResponse {
        private Long id;
        private Long taskListId;
        private String title;
        private String description;
        private TaskStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime deadline;
        private boolean deadlineTimeExplicitlySet;
        private TaskRecurrenceStrategy recurrenceStrategy;

        public TaskResponse(Task task) {
            this.id = task.getId();
            this.taskListId = task.getTaskListId();
            this.title = task.getTitle();
            this.description = task.getDescription();
            this.status = task.getStatus();
            this.createdAt = task.getCreatedAt();
            this.deadline = task.getDeadline();
            this.deadlineTimeExplicitlySet = task.isDeadlineTimeExplicitlySet();
            this.recurrenceStrategy = task.getRecurrenceStrategy();
        }
    }

    @Data
    public static abstract class CreateOrUpdateTaskRequest {
        @NotBlank
        @Size(max = 255)
        private String title;
        @Size(max = 10_000)
        private String description;
        private TaskStatus status;
        @FutureOrPresent
        private LocalDateTime deadline;
        private boolean deadlineTimeExplicitlySet;
        private TaskRecurrenceStrategy recurrenceStrategy;

        public Task toTask() {
            return Task.builder()
                    .title(title)
                    .description(description)
                    .status(status)
                    .deadline(deadline)
                    .deadlineTimeExplicitlySet(deadlineTimeExplicitlySet)
                    .recurrenceStrategy(recurrenceStrategy)
                    .build();
        }
    }

    public static class CreateTaskRequest extends CreateOrUpdateTaskRequest {
    }

    public static class UpdateTaskRequest extends CreateOrUpdateTaskRequest {
    }
}

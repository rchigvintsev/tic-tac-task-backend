package org.briarheart.tictactask.task;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import org.briarheart.tictactask.controller.AbstractController;
import org.briarheart.tictactask.task.comment.TaskComment;
import org.briarheart.tictactask.task.comment.TaskCommentController.CreateTaskCommentRequest;
import org.briarheart.tictactask.task.comment.TaskCommentController.TaskCommentResponse;
import org.briarheart.tictactask.task.recurrence.TaskRecurrenceStrategy;
import org.briarheart.tictactask.task.tag.TaskTagController.TaskTagResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY;

/**
 * REST-controller for task managing.
 *
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Allows to manage tasks, add/remove task tags, add/remove task comments")
@SecurityRequirement(name = "apiSecurityScheme")
public class TaskController extends AbstractController {
    private final TaskService taskService;

    @GetMapping("/count")
    @Operation(
            summary = "Get number of tasks",
            description = "Returns number of tasks created by current user",
            parameters = {
                    @Parameter(
                            name = "statuses",
                            description = "Task statuses",
                            in = QUERY,
                            array = @ArraySchema(schema = @Schema(type = "string"), uniqueItems = true)
                    ),
                    @Parameter(name = "deadlineDateTimeFrom", description = "Lower bound of task deadline", in = QUERY),
                    @Parameter(name = "deadlineDateTimeTo", description = "Upper bound of task deadline", in = QUERY)
            }
    )
    public Mono<Long> getTaskCount(@Parameter(hidden = true) GetTasksRequest request, Authentication authentication) {
        return taskService.getTaskCount(request, getUser(authentication));
    }

    @GetMapping
    @Operation(
            summary = "Get tasks",
            description = "Returns tasks created by current user",
            parameters = {
                    @Parameter(
                            name = "statuses",
                            description = "Task statuses",
                            in = QUERY,
                            array = @ArraySchema(schema = @Schema(type = "string"), uniqueItems = true)
                    ),
                    @Parameter(name = "deadlineDateTimeFrom", description = "Lower bound of task deadline", in = QUERY),
                    @Parameter(name = "deadlineDateTimeTo", description = "Upper bound of task deadline", in = QUERY),
                    @Parameter(name = "page", description = "Number of requested page", in = QUERY),
                    @Parameter(name = "size", description = "Requested page size", in = QUERY)
            }
    )
    public Flux<TaskResponse> getTasks(@Parameter(hidden = true) GetTasksRequest request,
                                       Authentication authentication,
                                       @Parameter(hidden = true) Pageable pageable) {
        return taskService.getTasks(request, getUser(authentication), pageable).map(TaskResponse::new);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by id", description = "Returns task by id")
    public Mono<TaskResponse> getTask(@PathVariable("id") Long id, Authentication authentication) {
        return taskService.getTask(id, getUser(authentication)).map(TaskResponse::new);
    }

    @PostMapping
    @Operation(summary = "Create new task", description = "Allows to create new task")
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
    @Operation(summary = "Update task", description = "Allows to update task")
    public Mono<TaskResponse> updateTask(@Valid @RequestBody UpdateTaskRequest updateRequest,
                                         @Parameter(description = "Task id") @PathVariable Long id,
                                         Authentication authentication) {
        Task task = updateRequest.toTask();
        task.setId(id);
        task.setUserId(getUser(authentication).getId());
        return taskService.updateTask(task).map(TaskResponse::new);
    }

    @PutMapping("/completed/{id}")
    @Operation(summary = "Complete task", description = "Allows to complete task")
    public Mono<TaskResponse> completeTask(@Parameter(description = "Task id") @PathVariable Long id,
                                           Authentication authentication) {
        return taskService.completeTask(id, getUser(authentication)).map(TaskResponse::new);
    }

    @DeleteMapping("/completed/{id}")
    @Operation(summary = "Restore task", description = "Allows to restore previously completed task")
    public Mono<TaskResponse> restoreTask(@Parameter(description = "Task id") @PathVariable Long id,
                                          Authentication authentication) {
        return taskService.restoreTask(id, getUser(authentication)).map(TaskResponse::new);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete task", description = "Allows to completely delete task")
    public Mono<Void> deleteTask(@Parameter(description = "Task id") @PathVariable Long id,
                                 Authentication authentication) {
        return taskService.deleteTask(id, getUser(authentication));
    }

    @GetMapping("/{taskId}/tags")
    @Operation(summary = "Get task tags", description = "Returns tags assigned to task")
    public Flux<TaskTagResponse> getTags(@Parameter(description = "Task id") @PathVariable("taskId") Long taskId,
                                         Authentication authentication) {
        return taskService.getTags(taskId, getUser(authentication)).map(TaskTagResponse::new);
    }

    @PutMapping("/{taskId}/tags/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Assign tag to task", description = "Allows to assign tag to task")
    public Mono<Void> assignTag(@Parameter(description = "Task id") @PathVariable("taskId") Long taskId,
                                @Parameter(description = "Tag id") @PathVariable("tagId") Long tagId,
                                Authentication authentication) {
        return taskService.assignTag(taskId, tagId, getUser(authentication));
    }

    @DeleteMapping("/{taskId}/tags/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove tag", description = "Allows to remove tag previously assigned to task")
    public Mono<Void> removeTag(@Parameter(description = "Task id") @PathVariable("taskId") Long taskId,
                                @Parameter(description = "Tag id") @PathVariable("tagId") Long tagId,
                                Authentication authentication) {
        return taskService.removeTag(taskId, tagId, getUser(authentication));
    }

    @GetMapping("/{taskId}/comments")
    @Operation(
            summary = "Get task comments",
            description = "Returns task comments",
            parameters = {
                    @Parameter(name = "page", description = "Number of requested page", in = QUERY),
                    @Parameter(name = "size", description = "Requested page size", in = QUERY),
            }
    )
    public Flux<TaskCommentResponse> getComments(
            @Parameter(description = "Task id") @PathVariable("taskId") Long taskId,
            Authentication authentication,
            @Parameter(hidden = true) Pageable pageable
    ) {
        return taskService.getComments(taskId, getUser(authentication), pageable).map(TaskCommentResponse::new);
    }

    @PostMapping("/{taskId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add task comment", description = "Allows to add task comment")
    public Mono<TaskCommentResponse> addComment(@Parameter(description = "Task id") @PathVariable("taskId") Long taskId,
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
        private Long parentId;
        private Long taskListId;
        private String title;
        private String description;
        private TaskStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
        private LocalDate deadlineDate;
        private LocalDateTime deadlineDateTime;
        private TaskRecurrenceStrategy recurrenceStrategy;

        public TaskResponse(Task task) {
            this.id = task.getId();
            this.parentId = task.getParentId();
            this.taskListId = task.getTaskListId();
            this.title = task.getTitle();
            this.description = task.getDescription();
            this.status = task.getStatus();
            this.createdAt = task.getCreatedAt();
            this.completedAt = task.getCompletedAt();
            this.deadlineDate = task.getDeadlineDate();
            this.deadlineDateTime = task.getDeadlineDateTime();
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
        private LocalDate deadlineDate;
        @FutureOrPresent
        private LocalDateTime deadlineDateTime;
        private boolean deadlineTimeSpecified;
        private TaskRecurrenceStrategy recurrenceStrategy;

        public Task toTask() {
            return Task.builder()
                    .title(title)
                    .description(description)
                    .status(status)
                    .deadlineDate(deadlineDate)
                    .deadlineDateTime(deadlineDateTime)
                    .recurrenceStrategy(recurrenceStrategy)
                    .build();
        }
    }

    @Getter
    @Setter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class CreateTaskRequest extends CreateOrUpdateTaskRequest {
        private Long parentId;

        @Override
        public Task toTask() {
            Task task = super.toTask();
            task.setParentId(parentId);
            return task;
        }
    }

    public static class UpdateTaskRequest extends CreateOrUpdateTaskRequest {
    }
}

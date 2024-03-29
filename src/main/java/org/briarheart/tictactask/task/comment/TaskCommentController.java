package org.briarheart.tictactask.task.comment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.briarheart.tictactask.controller.AbstractController;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * REST-controller for task comment managing.
 *
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/api/v1/task-comments")
@RequiredArgsConstructor
@Tag(name = "Task comments", description = "Allows to update/delete task comments")
@SecurityRequirement(name = "apiSecurityScheme")
public class TaskCommentController extends AbstractController {
    private final TaskCommentService taskCommentService;

    @PutMapping("/{id}")
    @Operation(summary = "Update task comment", description = "Allows to update task comment")
    public Mono<TaskCommentResponse> updateComment(@Valid @RequestBody UpdateTaskCommentRequest updateRequest,
                                                   @Parameter(description = "Task comment id") @PathVariable Long id,
                                                   Authentication authentication) {
        TaskComment comment = updateRequest.toTaskComment();
        comment.setId(id);
        comment.setUserId(getUser(authentication).getId());
        return taskCommentService.updateComment(comment).map(TaskCommentResponse::new);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete task comment", description = "Allows to completely delete task comment")
    public Mono<Void> deleteComment(@Parameter(description = "Task comment id") @PathVariable Long id,
                                    Authentication authentication) {
        return taskCommentService.deleteComment(id, getUser(authentication));
    }

    @Data
    @NoArgsConstructor
    public static class TaskCommentResponse {
        private Long id;
        private Long taskId;
        private String commentText;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public TaskCommentResponse(TaskComment taskComment) {
            this.id = taskComment.getId();
            this.taskId = taskComment.getTaskId();
            this.commentText = taskComment.getCommentText();
            this.createdAt = taskComment.getCreatedAt();
            this.updatedAt = taskComment.getUpdatedAt();
        }
    }

    @Data
    public static abstract class CreateOrUpdateTaskCommentRequest {
        @NotBlank
        @Size(max = 10_000)
        private String commentText;

        public TaskComment toTaskComment() {
            return TaskComment.builder().commentText(commentText).build();
        }
    }

    public static class CreateTaskCommentRequest extends CreateOrUpdateTaskCommentRequest {
    }

    public static class UpdateTaskCommentRequest extends CreateOrUpdateTaskCommentRequest {
    }
}

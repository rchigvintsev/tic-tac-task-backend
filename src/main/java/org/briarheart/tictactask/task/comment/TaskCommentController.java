package org.briarheart.tictactask.task.comment;

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
public class TaskCommentController extends AbstractController {
    private final TaskCommentService taskCommentService;

    @PutMapping("/{id}")
    public Mono<TaskCommentResponse> updateComment(@Valid @RequestBody UpdateTaskCommentRequest updateRequest,
                                                   @PathVariable Long id,
                                                   Authentication authentication) {
        TaskComment comment = updateRequest.toTaskComment();
        comment.setId(id);
        comment.setUserId(getUser(authentication).getId());
        return taskCommentService.updateComment(comment).map(TaskCommentResponse::new);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteComment(@PathVariable Long id, Authentication authentication) {
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
    @NoArgsConstructor
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

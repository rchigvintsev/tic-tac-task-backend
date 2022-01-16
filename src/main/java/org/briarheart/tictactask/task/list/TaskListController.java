package org.briarheart.tictactask.task.list;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.briarheart.tictactask.controller.AbstractController;
import org.briarheart.tictactask.task.TaskController.TaskResponse;
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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.net.URI;

/**
 * REST-controller for task list managing.
 *
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/api/v1/task-lists")
@RequiredArgsConstructor
@Tag(
        name = "Task lists",
        description = "Allows to manage task lists, include task in list, exclude task from list, " +
                "get tasks included in list"
)
public class TaskListController extends AbstractController {
    private final TaskListService taskListService;

    @GetMapping("/uncompleted")
    public Flux<TaskListResponse> getUncompletedTaskLists(Authentication authentication) {
        return taskListService.getUncompletedTaskLists(getUser(authentication)).map(TaskListResponse::new);
    }

    @GetMapping("/{id}")
    public Mono<TaskListResponse> getTaskList(@PathVariable("id") Long id, Authentication authentication) {
        return taskListService.getTaskList(id, getUser(authentication)).map(TaskListResponse::new);
    }

    @PostMapping
    public Mono<ResponseEntity<TaskListResponse>> createTaskList(@Valid @RequestBody CreateTaskListRequest createRequest,
                                                                 Authentication authentication,
                                                                 ServerHttpRequest request) {
        TaskList taskList = createRequest.toTaskList();
        taskList.setUserId(getUser(authentication).getId());
        return taskListService.createTaskList(taskList).map(createdTaskList -> {
            URI taskListLocation = UriComponentsBuilder.fromHttpRequest(request)
                    .path("/{id}")
                    .buildAndExpand(createdTaskList.getId())
                    .toUri();
            return ResponseEntity.created(taskListLocation).body(new TaskListResponse(createdTaskList));
        });
    }

    @PutMapping("/{id}")
    public Mono<TaskListResponse> updateTaskList(@Valid @RequestBody UpdateTaskListRequest updateRequest,
                                                 @PathVariable Long id,
                                                 Authentication authentication) {
        TaskList taskList = updateRequest.toTaskList();
        taskList.setId(id);
        taskList.setUserId(getUser(authentication).getId());
        return taskListService.updateTaskList(taskList).map(TaskListResponse::new);
    }

    @PutMapping("/completed/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> completeTaskList(@PathVariable Long id, Authentication authentication) {
        return taskListService.completeTaskList(id, getUser(authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteTaskList(@PathVariable Long id, Authentication authentication) {
        return taskListService.deleteTaskList(id, getUser(authentication));
    }

    @GetMapping("/{taskListId}/tasks")
    public Flux<TaskResponse> getTasks(@PathVariable Long taskListId,
                                       Authentication authentication,
                                       Pageable pageable) {
        return taskListService.getTasks(taskListId, getUser(authentication), pageable).map(TaskResponse::new);
    }

    @PutMapping("/{taskListId}/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> addTask(@PathVariable Long taskListId, @PathVariable Long taskId, Authentication authentication) {
        return taskListService.addTask(taskListId, taskId, getUser(authentication));
    }

    @DeleteMapping("/{taskListId}/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeTask(@PathVariable Long taskListId,
                                 @PathVariable Long taskId,
                                 Authentication authentication) {
        return taskListService.removeTask(taskListId, taskId, getUser(authentication));
    }

    @Data
    @NoArgsConstructor
    public static class TaskListResponse {
        private Long id;
        private String name;
        private boolean completed;

        public TaskListResponse(TaskList taskList) {
            this.id = taskList.getId();
            this.name = taskList.getName();
            this.completed = taskList.isCompleted();
        }
    }

    @Data
    public static abstract class CreateOrUpdateTaskListRequest {
        @NotBlank
        @Size(max = 255)
        private String name;
        private boolean completed;

        public TaskList toTaskList() {
            return TaskList.builder().name(name).completed(completed).build();
        }
    }

    public static class CreateTaskListRequest extends CreateOrUpdateTaskListRequest {
    }

    public static class UpdateTaskListRequest extends CreateOrUpdateTaskListRequest {
    }
}

package org.briarheart.tictactask.task.list;

import lombok.RequiredArgsConstructor;
import org.briarheart.tictactask.controller.AbstractController;
import org.briarheart.tictactask.task.Task;
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
import java.net.URI;

/**
 * REST-controller for task list managing.
 *
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/api/v1/task-lists")
@RequiredArgsConstructor
public class TaskListController extends AbstractController {
    private final TaskListService taskListService;

    @GetMapping("/uncompleted")
    public Flux<TaskList> getUncompletedTaskLists(Authentication authentication) {
        return taskListService.getUncompletedTaskLists(getUser(authentication));
    }

    @GetMapping("/{id}")
    public Mono<TaskList> getTaskList(@PathVariable("id") Long id, Authentication authentication) {
        return taskListService.getTaskList(id, getUser(authentication));
    }

    @PostMapping
    public Mono<ResponseEntity<TaskList>> createTaskList(@Valid @RequestBody TaskList taskList,
                                                         Authentication authentication,
                                                         ServerHttpRequest request) {
        taskList.setUserId(getUser(authentication).getId());
        return taskListService.createTaskList(taskList).map(createdTaskList -> {
            URI taskListLocation = UriComponentsBuilder.fromHttpRequest(request)
                    .path("/{id}")
                    .buildAndExpand(createdTaskList.getId())
                    .toUri();
            return ResponseEntity.created(taskListLocation).body(createdTaskList);
        });
    }

    @PutMapping("/{id}")
    public Mono<TaskList> updateTaskList(@Valid @RequestBody TaskList taskList,
                                         @PathVariable Long id,
                                         Authentication authentication) {
        taskList.setId(id);
        taskList.setUserId(getUser(authentication).getId());
        return taskListService.updateTaskList(taskList);
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
    public Flux<Task> getTasks(@PathVariable Long taskListId,
                               Authentication authentication,
                               Pageable pageable) {
        return taskListService.getTasks(taskListId, getUser(authentication), pageable);
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
}

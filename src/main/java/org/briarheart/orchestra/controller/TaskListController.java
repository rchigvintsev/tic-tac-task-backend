package org.briarheart.orchestra.controller;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.TaskList;
import org.briarheart.orchestra.service.TaskListService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;

/**
 * REST-controller for task list managing.
 *
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/task-lists")
@RequiredArgsConstructor
public class TaskListController {
    private final TaskListService taskListService;

    @GetMapping("/uncompleted")
    public Flux<TaskList> getUncompletedTaskLists(Principal user) {
        return taskListService.getUncompletedTaskLists(user.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteTaskList(@PathVariable Long id, Principal user) {
        return taskListService.deleteTaskList(id, user.getName());
    }
}

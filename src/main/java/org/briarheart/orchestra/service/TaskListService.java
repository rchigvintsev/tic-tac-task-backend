package org.briarheart.orchestra.service;

import org.briarheart.orchestra.model.TaskList;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for task list managing.
 *
 * @author Roman Chigvintsev
 * @see TaskList
 */
public interface TaskListService {
    /**
     * Returns uncompleted task lists belonging to the given author.
     *
     * @param author task list author
     * @return found task lists or empty stream when there is no task list meeting the given criteria
     */
    Flux<TaskList> getUncompletedTaskLists(String author);

    /**
     * Deletes task list with the given id and belonging to the given author.
     *
     * @param id task list id
     * @param author task list author
     */
    Mono<Void> deleteTaskList(Long id, String author);
}

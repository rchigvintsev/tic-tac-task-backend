package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TaskListRepository;
import org.briarheart.orchestra.model.TaskList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultTaskListServiceTest {
    private TaskListRepository taskListRepository;
    private DefaultTaskListService taskListService;

    @BeforeEach
    void setUp() {
        taskListRepository = mock(TaskListRepository.class);
        taskListService = new DefaultTaskListService(taskListRepository);
    }

    @Test
    void shouldReturnAllUncompletedTaskLists() {
        String author = "alice";
        when(taskListRepository.findByCompletedAndAuthor(false, author)).thenReturn(Flux.empty());
        taskListService.getUncompletedTaskLists(author).blockFirst();
        verify(taskListRepository, times(1)).findByCompletedAndAuthor(false, author);
    }

    @Test
    void shouldReturnTaskListById() {
        TaskList taskList = TaskList.builder().id(1L).name("Test task list").author("alice").build();
        when(taskListRepository.findByIdAndAuthor(taskList.getId(), taskList.getAuthor()))
                .thenReturn(Mono.just(taskList));

        TaskList result = taskListService.getTaskList(taskList.getId(), taskList.getAuthor()).block();
        assertNotNull(result);
        assertEquals(taskList, result);
    }

    @Test
    void shouldThrowExceptionOnTaskListGetWhenTaskListIsNotFound() {
        when(taskListRepository.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class, () -> taskListService.getTaskList(1L, "alice").block());
    }

    @Test
    void shouldDeleteTaskList() {
        TaskList taskList = TaskList.builder().id(1L).name("Test task list").author("alice").build();

        when(taskListRepository.findByIdAndAuthor(taskList.getId(), taskList.getAuthor()))
                .thenReturn(Mono.just(taskList));
        when(taskListRepository.deleteByIdAndAuthor(taskList.getId(), taskList.getAuthor())).thenReturn(Mono.empty());

        taskListService.deleteTaskList(taskList.getId(), taskList.getAuthor()).block();
        verify(taskListRepository, times(1)).deleteByIdAndAuthor(taskList.getId(), taskList.getAuthor());
    }

    @Test
    void shouldThrowExceptionOnTaskListDeleteWhenTaskListIsNotFound() {
        when(taskListRepository.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        long taskListId = 1L;
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> taskListService.deleteTaskList(taskListId, "alice").block());
        assertEquals("Task list with id " + taskListId + " is not found", exception.getMessage());
    }
}

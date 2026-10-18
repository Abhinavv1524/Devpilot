package com.devpilot.orchestrator.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devpilot.orchestrator.model.TaskState;

@ExtendWith(MockitoExtension.class)
class TaskHistoryServiceTest {

    @Mock
    private TaskHistoryRepository repository;

    @Test
    void historyListIsQueriedForTheSignedInUser() {
        TaskHistoryService service = new TaskHistoryService(repository);
        when(repository.findTop20ByOwnerUsernameOrderByCreatedAtDesc("alice"))
                .thenReturn(List.of());

        assertEquals(List.of(), service.findAll("alice"));

        verify(repository).findTop20ByOwnerUsernameOrderByCreatedAtDesc("alice");
    }

    @Test
    void historyDetailsAreQueriedByTaskAndOwner() {
        TaskHistoryService service = new TaskHistoryService(repository);
        when(repository.findByTaskIdAndOwnerUsername("task-1", "alice"))
                .thenReturn(Optional.empty());

        assertEquals(Optional.empty(), service.findById("task-1", "alice"));

        verify(repository).findByTaskIdAndOwnerUsername("task-1", "alice");
    }

    @Test
    void relatedHistorySearchIsScopedToTheSignedInUser() {
        TaskHistoryService service = new TaskHistoryService(repository);
        when(repository.searchByTerm("alice", "project")).thenReturn(List.of());

        assertEquals(List.of(), service.findRelevant("alice", "project"));

        verify(repository).searchByTerm("alice", "project");
    }

    @Test
    void terminalHistoryRecordStoresItsOwner() {
        TaskHistoryService service = new TaskHistoryService(repository);
        TaskState task = new TaskState("task prompt", "alice");
        task.setStatus("COMPLETED");
        when(repository.save(any(TaskHistoryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.saveTerminalTask(task);

        ArgumentCaptor<TaskHistoryEntity> captor = ArgumentCaptor.forClass(TaskHistoryEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("alice", captor.getValue().getOwnerUsername());
    }

    @Test
    void cancelledTaskIsPersistedToHistory() {
        TaskHistoryService service = new TaskHistoryService(repository);
        TaskState task = new TaskState("task prompt", "alice");
        task.setStatus("CANCELLED");
        when(repository.save(any(TaskHistoryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.saveTerminalTask(task);

        ArgumentCaptor<TaskHistoryEntity> captor = ArgumentCaptor.forClass(TaskHistoryEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("CANCELLED", captor.getValue().getTaskStatus());
    }

    @Test
    void nonTerminalTaskIsNotPersistedToHistory() {
        TaskHistoryService service = new TaskHistoryService(repository);
        TaskState task = new TaskState("task prompt", "alice");
        task.setStatus("RESEARCHING");

        service.saveTerminalTask(task);

        verify(repository, org.mockito.Mockito.never()).save(any(TaskHistoryEntity.class));
    }
}

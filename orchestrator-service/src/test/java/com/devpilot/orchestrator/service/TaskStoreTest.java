package com.devpilot.orchestrator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.devpilot.orchestrator.model.TaskState;

class TaskStoreTest {

    @Test
    void taskLookupRequiresTheMatchingOwner() {
        TaskStore taskStore = new TaskStore();
        TaskState task = new TaskState("Alice task", "alice");
        taskStore.save(task);

        assertEquals(task, taskStore.findById(task.getTaskId(), "alice").orElseThrow());
        assertTrue(taskStore.findById(task.getTaskId(), "bob").isEmpty());
    }
}

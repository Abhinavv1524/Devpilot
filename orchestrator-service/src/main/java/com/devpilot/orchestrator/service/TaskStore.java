
package com.devpilot.orchestrator.service;

import com.devpilot.orchestrator.model.TaskState;
import com.devpilot.orchestrator.history.TaskHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskStore {

    private final Map<String, TaskState> tasks = new ConcurrentHashMap<>();
    private final TaskHistoryService taskHistoryService;

    public TaskStore() {
        this.taskHistoryService = null;
    }

    @Autowired
    public TaskStore(TaskHistoryService taskHistoryService) {
        this.taskHistoryService = taskHistoryService;
    }

    public TaskState save(TaskState task) {
        tasks.put(task.getTaskId(), task);
        if (taskHistoryService != null) {
            taskHistoryService.saveTerminalTask(task);
        }
        return task;
    }

    public Optional<TaskState> findById(String taskId, String ownerUsername) {
        return Optional.ofNullable(tasks.get(taskId)).filter(task -> java.util.Objects.equals(
                task.getOwnerUsername(), ownerUsername));
    }
}

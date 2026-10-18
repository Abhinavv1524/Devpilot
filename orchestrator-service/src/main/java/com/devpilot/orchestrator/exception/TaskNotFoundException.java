
package com.devpilot.orchestrator.exception;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(String taskId) {
        super("Task not found: " + taskId);
    }
}

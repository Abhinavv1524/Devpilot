package com.devpilot.orchestrator.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;

import com.devpilot.orchestrator.history.TaskHistoryResponse;
import com.devpilot.orchestrator.history.TaskHistoryService;

@RestController
@RequestMapping("/tasks/history")
public class TaskHistoryController {

    private final TaskHistoryService taskHistoryService;

    public TaskHistoryController(TaskHistoryService taskHistoryService) {
        this.taskHistoryService = taskHistoryService;
    }

    @GetMapping
    public List<TaskHistoryResponse> getHistory(Principal principal) {
        return taskHistoryService.findAll(principal.getName());
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskHistoryResponse> getHistory(
            @PathVariable String taskId, Principal principal) {
        return taskHistoryService.findById(taskId, principal.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

package com.devpilot.orchestrator.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devpilot.orchestrator.model.TaskRequest;
import com.devpilot.orchestrator.model.TaskState;
import com.devpilot.orchestrator.service.OrchestrationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.security.Principal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/tasks")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {
    private final OrchestrationService orchestrationService;

    public TaskController(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping()
    public ResponseEntity<TaskState> createTask(
            @Valid @RequestBody TaskRequest request, Principal principal) {

        TaskState task = orchestrationService.createTask(request, principal.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskState> getTask(
            @Valid @PathVariable String taskId, Principal principal) {

        TaskState task = orchestrationService.getTask(taskId, principal.getName());

        return ResponseEntity.ok(task);
    }

    @PostMapping("/{taskId}/approve")
    public ResponseEntity<TaskState> approveTask(
            @PathVariable String taskId, Principal principal) {

        TaskState task = orchestrationService.approveAndApply(taskId, principal.getName());

        return ResponseEntity.ok(task);
    }

    @PostMapping("/{taskId}/cancel")
    public ResponseEntity<TaskState> cancelTask(
            @PathVariable String taskId, Principal principal) {

        TaskState task = orchestrationService.cancelTask(taskId, principal.getName());

        return ResponseEntity.ok(task);
    }

}

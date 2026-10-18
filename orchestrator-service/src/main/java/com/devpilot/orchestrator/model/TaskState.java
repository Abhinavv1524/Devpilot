package com.devpilot.orchestrator.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.devpilot.orchestrator.routing.RoutingDecision;

public class TaskState {
    private String taskId;
    private String ownerUsername;
    private String description;
    private String status;
    private String createdAt;

    // retryCount — how many times we've retried coding after failed tests.
    // message — a short explanation of the current progress or failure.
    private int retryCount;
    private String message;
    private boolean automaticRepairsAuthorized;

    /** Set when a user requests cancellation; checked cooperatively by the async workflow. */
    private boolean cancelRequested;

    private RoutingDecision routingDecision;
    private ProjectAnalysis projectAnalysis;
    private CodingResult codingResult;

    private String projectPath;
    private java.util.List<TaskRequest.SmokeTestCase> smokeTests = new java.util.ArrayList<>();
    private TestingResult testingResult;

    public TaskState() {
    }

    public TaskState(String description) {
        this(description, null);
    }

    public TaskState(String description, String ownerUsername) {
        this.taskId = UUID.randomUUID().toString();
        this.ownerUsername = ownerUsername;
        this.description = description;
        this.status = TaskStatus.RECEIVED.name();
        this.createdAt = LocalDateTime.now().toString();
        this.retryCount = 0;
        this.message = "Task received and waiting to be processed.";
    }

    public String getTaskId() {
        return taskId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getMessage() {
        return message;
    }

    public boolean isAutomaticRepairsAuthorized() {
        return automaticRepairsAuthorized;
    }

    public void setAutomaticRepairsAuthorized(boolean automaticRepairsAuthorized) {
        this.automaticRepairsAuthorized = automaticRepairsAuthorized;
    }

    public boolean isCancelRequested() {
        return cancelRequested;
    }

    public void setCancelRequested(boolean cancelRequested) {
        this.cancelRequested = cancelRequested;
    }

    public boolean isTerminal() {
        return TaskStatus.isTerminal(this.status);
    }

    public RoutingDecision getRoutingDecision() {
        return routingDecision;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setRoutingDecision(RoutingDecision routingDecision) {
        this.routingDecision = routingDecision;
    }

    public ProjectAnalysis getProjectAnalysis() {
        return projectAnalysis;
    }

    public void setProjectAnalysis(ProjectAnalysis projectAnalysis) {
        this.projectAnalysis = projectAnalysis;
    }

    public CodingResult getCodingResult() {
        return codingResult;
    }

    public void setCodingResult(CodingResult codingResult) {
        this.codingResult = codingResult;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public java.util.List<TaskRequest.SmokeTestCase> getSmokeTests() {
        return smokeTests;
    }

    public void setSmokeTests(java.util.List<TaskRequest.SmokeTestCase> smokeTests) {
        this.smokeTests = smokeTests;
    }

    public TestingResult getTestingResult() {
        return testingResult;
    }

    public void setTestingResult(TestingResult testingResult) {
        this.testingResult = testingResult;
    }
}

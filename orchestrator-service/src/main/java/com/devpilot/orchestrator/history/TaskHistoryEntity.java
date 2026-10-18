package com.devpilot.orchestrator.history;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "task_history")
public class TaskHistoryEntity {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String taskId;

    @Column(length = 50)
    private String ownerUsername;

    @Column(nullable = false, length = 4000)
    private String userPrompt;

    @Column(nullable = false, length = 40)
    private String taskStatus;

    @Column(length = 12000)
    private String researchSummary;

    @Column(length = 12000)
    private String codingSummary;

    @Column(length = 12000)
    private String testingSummary;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    protected TaskHistoryEntity() {
    }

    public TaskHistoryEntity(
            String taskId,
            String ownerUsername,
            String userPrompt,
            String taskStatus,
            String researchSummary,
            String codingSummary,
            String testingSummary,
            LocalDateTime createdAt,
            LocalDateTime completedAt) {
        this.taskId = taskId;
        this.ownerUsername = ownerUsername;
        this.userPrompt = userPrompt;
        this.taskStatus = taskStatus;
        this.researchSummary = researchSummary;
        this.codingSummary = codingSummary;
        this.testingSummary = testingSummary;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public String getTaskId() { return taskId; }
    public String getOwnerUsername() { return ownerUsername; }
    public String getUserPrompt() { return userPrompt; }
    public String getTaskStatus() { return taskStatus; }
    public String getResearchSummary() { return researchSummary; }
    public String getCodingSummary() { return codingSummary; }
    public String getTestingSummary() { return testingSummary; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}

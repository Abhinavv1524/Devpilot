package com.devpilot.orchestrator.history;

import java.time.LocalDateTime;

public record TaskHistoryResponse(
        String taskId,
        String userPrompt,
        String taskStatus,
        String researchSummary,
        String codingSummary,
        String testingSummary,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {

    public static TaskHistoryResponse from(TaskHistoryEntity entity) {
        return new TaskHistoryResponse(
                entity.getTaskId(),
                entity.getUserPrompt(),
                entity.getTaskStatus(),
                entity.getResearchSummary(),
                entity.getCodingSummary(),
                entity.getTestingSummary(),
                entity.getCreatedAt(),
                entity.getCompletedAt());
    }
}

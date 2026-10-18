package com.devpilot.orchestrator.model;

public enum TaskStatus {
    RECEIVED,
    RESEARCHING,
    CODING,
    AWAITING_APPROVAL,
    TESTING,
    DOCUMENTING,
    COMPLETED,
    FAILED,
    CANCELLED;

    /** Statuses from which a task can no longer progress or be cancelled. */
    public static boolean isTerminal(String status) {
        return "COMPLETED".equals(status)
                || "FAILED".equals(status)
                || "CANCELLED".equals(status);
    }
}

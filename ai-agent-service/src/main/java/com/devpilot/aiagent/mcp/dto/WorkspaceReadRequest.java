package com.devpilot.aiagent.mcp.dto;

public record WorkspaceReadRequest(
        Operation operation,
        String projectPath,
        String filePath,
        String query) {

    public WorkspaceReadRequest(
            Operation operation,
            String projectPath,
            String filePath) {
        this(operation, projectPath, filePath, null);
    }

    public enum Operation {
        PROJECT_CONTEXT,
        SOURCE_FILE,
        FILE_EXISTS
    }
}

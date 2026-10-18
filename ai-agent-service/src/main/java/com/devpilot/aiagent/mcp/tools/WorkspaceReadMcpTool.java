package com.devpilot.aiagent.mcp.tools;

import java.io.IOException;

import com.devpilot.aiagent.mcp.McpTool;
import com.devpilot.aiagent.mcp.dto.WorkspaceReadRequest;
import com.devpilot.aiagent.mcp.dto.WorkspaceReadResult;
import com.devpilot.aiagent.tools.WorkspaceTool;

public class WorkspaceReadMcpTool
        implements McpTool<WorkspaceReadRequest, WorkspaceReadResult> {

    public static final String NAME = "workspace-read";

    private final WorkspaceTool workspaceTool;

    public WorkspaceReadMcpTool(WorkspaceTool workspaceTool) {
        this.workspaceTool = workspaceTool;
    }

    @Override
    public String toolName() {
        return NAME;
    }

    @Override
    public String description() {
        return "Read bounded project context or an eligible source file, or check a workspace file path.";
    }

    @Override
    public WorkspaceReadResult execute(WorkspaceReadRequest input) {
        if (input == null || input.operation() == null) {
            throw new IllegalArgumentException("A workspace read operation is required.");
        }

        try {
            return switch (input.operation()) {
                case PROJECT_CONTEXT -> new WorkspaceReadResult(
                        workspaceTool.getProjectContext(
                                input.projectPath(), input.query()), null);
                case SOURCE_FILE -> new WorkspaceReadResult(
                        workspaceTool.readSourceFile(input.filePath()), null);
                case FILE_EXISTS -> new WorkspaceReadResult(
                        null, workspaceTool.isExistingRegularFile(input.filePath()));
            };
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Workspace read tool failed: " + exception.getMessage(), exception);
        }
    }
}

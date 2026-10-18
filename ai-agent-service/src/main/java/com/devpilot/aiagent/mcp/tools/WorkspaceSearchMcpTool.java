package com.devpilot.aiagent.mcp.tools;

import java.io.IOException;

import com.devpilot.aiagent.mcp.McpTool;
import com.devpilot.aiagent.mcp.dto.WorkspaceSearchRequest;
import com.devpilot.aiagent.tools.WorkspaceTool;

public class WorkspaceSearchMcpTool implements McpTool<WorkspaceSearchRequest, String> {

    public static final String NAME = "workspace-search";

    private final WorkspaceTool workspaceTool;

    public WorkspaceSearchMcpTool(WorkspaceTool workspaceTool) {
        this.workspaceTool = workspaceTool;
    }

    @Override
    public String toolName() {
        return NAME;
    }

    @Override
    public String description() {
        return "Search eligible source files for a literal text query inside a workspace project.";
    }

    @Override
    public String execute(WorkspaceSearchRequest input) {
        if (input == null) {
            throw new IllegalArgumentException("A workspace search request is required.");
        }

        try {
            return workspaceTool.searchProjectFiles(input.projectPath(), input.query());
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Workspace search tool failed: " + exception.getMessage(), exception);
        }
    }
}

package com.devpilot.aiagent.mcp;

import java.util.Map;
import java.util.Set;

import com.devpilot.aiagent.mcp.tools.TestExecutionMcpTool;
import com.devpilot.aiagent.mcp.tools.WorkspaceReadMcpTool;
import com.devpilot.aiagent.mcp.tools.WorkspaceSearchMcpTool;

public final class AgentMcpToolAccess {

    public static final Map<String, Set<String>> ALLOWED_TOOLS = Map.of(
            "Research Agent", Set.of(
                    WorkspaceSearchMcpTool.NAME,
                    WorkspaceReadMcpTool.NAME),
            "Coding Agent", Set.of(
                    WorkspaceSearchMcpTool.NAME,
                    WorkspaceReadMcpTool.NAME),
            "Testing Agent", Set.of(TestExecutionMcpTool.NAME));

    private AgentMcpToolAccess() {
    }
}

package com.devpilot.aiagent.agent;

import com.devpilot.aiagent.dto.TestingRequest;
import com.devpilot.aiagent.dto.TestingResult;
import com.devpilot.aiagent.mcp.AgentMcpToolAccess;
import com.devpilot.aiagent.mcp.McpToolRegistry;
import com.devpilot.aiagent.mcp.tools.TestExecutionMcpTool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestingAgent {

    private final TestExecutionMcpTool testExecutionTool;

    public TestingAgent(McpToolRegistry mcpToolRegistry) {
        this.testExecutionTool = mcpToolRegistry.getTool(
                TestExecutionMcpTool.NAME,
                AgentMcpToolAccess.ALLOWED_TOOLS.get("Testing Agent"),
                TestExecutionMcpTool.class);
    }

    public TestingResult test(TestingRequest request) {
        if (request == null) {
            return rejected("Testing request cannot be null.");
        }

        if (request.taskDescription() == null
                || request.taskDescription().isBlank()) {
            return rejected("Task description is required.");
        }

        if (request.projectPath() == null
                || request.projectPath().isBlank()) {
            return rejected("Project path is required.");
        }

        if (request.testType() == null) {
            return rejected("Test type is required.");
        }

        return testExecutionTool.execute(request);
    }

    private TestingResult rejected(String message) {
        return new TestingResult(
                TestingResult.Status.REJECTED,
                message,
                null,
                null,
                null,
                "",
                "",
                false,
                List.of(message));
    }
}

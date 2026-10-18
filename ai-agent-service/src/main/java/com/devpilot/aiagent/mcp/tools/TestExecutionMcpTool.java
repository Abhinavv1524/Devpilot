package com.devpilot.aiagent.mcp.tools;

import com.devpilot.aiagent.dto.TestingRequest;
import com.devpilot.aiagent.dto.TestingResult;
import com.devpilot.aiagent.mcp.McpTool;
import com.devpilot.aiagent.tools.TestExecutionTool;

public class TestExecutionMcpTool implements McpTool<TestingRequest, TestingResult> {

    public static final String NAME = "test-execution";

    private final TestExecutionTool testExecutionTool;

    public TestExecutionMcpTool(TestExecutionTool testExecutionTool) {
        this.testExecutionTool = testExecutionTool;
    }

    @Override
    public String toolName() {
        return NAME;
    }

    @Override
    public String description() {
        return "Validate a project and execute one of DevPilot's supported test modes.";
    }

    @Override
    public TestingResult execute(TestingRequest input) {
        return testExecutionTool.execute(input);
    }
}

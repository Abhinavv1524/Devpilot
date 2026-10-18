package com.devpilot.aiagent.mcp;

public interface McpTool<I, O> {

    String toolName();

    String description();

    O execute(I input);
}

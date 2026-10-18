package com.devpilot.aiagent.mcp.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.devpilot.aiagent.mcp.McpTool;
import com.devpilot.aiagent.mcp.McpToolRegistry;
import com.devpilot.aiagent.mcp.tools.TestExecutionMcpTool;
import com.devpilot.aiagent.mcp.tools.WorkspaceChangeMcpTool;
import com.devpilot.aiagent.mcp.tools.WorkspaceReadMcpTool;
import com.devpilot.aiagent.mcp.tools.WorkspaceSearchMcpTool;
import com.devpilot.aiagent.service.WorkspaceChangeService;
import com.devpilot.aiagent.tools.TestExecutionTool;
import com.devpilot.aiagent.tools.WorkspaceTool;

@Configuration
public class McpToolConfiguration {

    @Bean
    WorkspaceSearchMcpTool workspaceSearchMcpTool(WorkspaceTool workspaceTool) {
        return new WorkspaceSearchMcpTool(workspaceTool);
    }

    @Bean
    WorkspaceReadMcpTool workspaceReadMcpTool(WorkspaceTool workspaceTool) {
        return new WorkspaceReadMcpTool(workspaceTool);
    }

    @Bean
    TestExecutionMcpTool testExecutionMcpTool(TestExecutionTool testExecutionTool) {
        return new TestExecutionMcpTool(testExecutionTool);
    }

    @Bean
    WorkspaceChangeMcpTool workspaceChangeMcpTool(
            WorkspaceChangeService workspaceChangeService) {
        return new WorkspaceChangeMcpTool(workspaceChangeService);
    }

    @Bean
    McpToolRegistry mcpToolRegistry(
            WorkspaceSearchMcpTool workspaceSearchMcpTool,
            WorkspaceReadMcpTool workspaceReadMcpTool,
            TestExecutionMcpTool testExecutionMcpTool,
            WorkspaceChangeMcpTool workspaceChangeMcpTool) {
        return new McpToolRegistry(List.<McpTool<?, ?>>of(
                workspaceSearchMcpTool,
                workspaceReadMcpTool,
                testExecutionMcpTool,
                workspaceChangeMcpTool));
    }
}

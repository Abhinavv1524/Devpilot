package com.devpilot.aiagent.mcp;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class McpToolRegistry {

    private final Map<String, McpTool<?, ?>> tools = new ConcurrentHashMap<>();

    public McpToolRegistry(Collection<? extends McpTool<?, ?>> initialTools) {
        initialTools.forEach(this::register);
    }

    public void register(McpTool<?, ?> tool) {
        if (tool == null || tool.toolName() == null || tool.toolName().isBlank()) {
            throw new IllegalArgumentException("An MCP tool must have a name.");
        }

        McpTool<?, ?> existing = tools.putIfAbsent(tool.toolName(), tool);
        if (existing != null) {
            throw new IllegalStateException(
                    "An MCP tool is already registered with name: " + tool.toolName());
        }
    }

    public List<McpToolDescriptor> discoverTools() {
        return tools.values().stream()
                .map(tool -> new McpToolDescriptor(tool.toolName(), tool.description()))
                .sorted(java.util.Comparator.comparing(McpToolDescriptor::toolName))
                .toList();
    }

    public McpTool<?, ?> getTool(String toolName) {
        McpTool<?, ?> tool = tools.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown MCP tool: " + toolName);
        }
        return tool;
    }

    public <T extends McpTool<?, ?>> T getTool(
            String toolName,
            Set<String> allowedTools,
            Class<T> toolType) {
        if (allowedTools == null || !allowedTools.contains(toolName)) {
            throw new SecurityException(
                    "MCP tool is not allowed for this agent: " + toolName);
        }
        return toolType.cast(getTool(toolName));
    }
}

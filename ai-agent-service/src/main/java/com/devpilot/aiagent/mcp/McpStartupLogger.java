package com.devpilot.aiagent.mcp;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class McpStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(McpStartupLogger.class);

    private final McpToolRegistry toolRegistry;

    public McpStartupLogger(McpToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logRegisteredTools() {
        var tools = toolRegistry.discoverTools();
        log.info("Registered MCP Tools ({}):\n{}", tools.size(),
                tools.stream()
                        .map(tool -> "- " + tool.toolName())
                        .collect(Collectors.joining("\n")));

        AgentMcpToolAccess.ALLOWED_TOOLS.forEach((agent, allowedTools) ->
                log.info("{} Tools:\n{}", agent,
                        allowedTools.stream()
                                .sorted()
                                .map(toolName -> "- " + toolName)
                                .collect(Collectors.joining("\n"))));
    }
}

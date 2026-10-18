package com.devpilot.aiagent.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class McpToolRegistryTest {

    @Test
    void registersAndDiscoversToolsByName() {
        McpTool<String, String> tool = new TestTool("workspace.read");
        McpToolRegistry registry = new McpToolRegistry(List.of(tool));

        assertEquals("workspace.read", registry.getTool("workspace.read").toolName());
        assertEquals("workspace.read", registry.discoverTools().getFirst().toolName());
    }

    @Test
    void rejectsDuplicateNamesAndToolsOutsideTheAgentAllowlist() {
        McpTool<String, String> first = new TestTool("workspace.read");
        McpToolRegistry registry = new McpToolRegistry(List.of(first));

        assertThrows(IllegalStateException.class,
                () -> registry.register(new TestTool("workspace.read")));
        assertThrows(SecurityException.class,
                () -> registry.getTool(
                        "workspace.read", Set.of("test.execute"), TestTool.class));
    }

    @Test
    void rejectsUnknownToolNames() {
        McpToolRegistry registry = new McpToolRegistry(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> registry.getTool("missing"));
    }

    private static final class TestTool implements McpTool<String, String> {
        private final String name;

        private TestTool(String name) {
            this.name = name;
        }

        @Override
        public String toolName() {
            return name;
        }

        @Override
        public String description() {
            return "Test tool";
        }

        @Override
        public String execute(String input) {
            return input;
        }
    }
}

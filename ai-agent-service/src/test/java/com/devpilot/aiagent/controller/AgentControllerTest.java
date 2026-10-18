package com.devpilot.aiagent.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;

import com.devpilot.aiagent.agent.CodingAgent;
import com.devpilot.aiagent.agent.ResearchAgent;
import com.devpilot.aiagent.agent.TestingAgent;
import com.devpilot.aiagent.dto.CodingRequest;
import com.devpilot.aiagent.dto.CodingResult;
import com.devpilot.aiagent.mcp.McpToolRegistry;
import com.devpilot.aiagent.mcp.tools.WorkspaceChangeMcpTool;
import com.devpilot.aiagent.service.WorkspaceChangeService;
import com.devpilot.aiagent.tools.WorkspaceTool;

class AgentControllerTest {

    @TempDir
    Path workspaceRoot;

    @Test
    void planCodeMarksUnsupportedProposalAsNeedingClarificationBeforeApproval() {
        CodingAgent codingAgent = org.mockito.Mockito.mock(CodingAgent.class);
        CodingResult proposal = proposal("foodMenuProject/assets/logo.png");
        when(codingAgent.plan(org.mockito.ArgumentMatchers.any(CodingRequest.class)))
                .thenReturn(proposal);
        WorkspaceChangeMcpTool changeTool = new WorkspaceChangeMcpTool(
                new WorkspaceChangeService(new WorkspaceTool(workspaceRoot.toString())));
        AgentController controller = new AgentController(
                org.mockito.Mockito.mock(ResearchAgent.class),
                codingAgent,
                new McpToolRegistry(List.of(changeTool)),
                org.mockito.Mockito.mock(TestingAgent.class));

        ResponseEntity<CodingResult> response =
                controller.planCode(new CodingRequest());

        assertEquals("NEEDS_CLARIFICATION", response.getBody().getStatus());
        assertTrue(response.getBody().getSummary().contains("logo.png"));
        assertTrue(response.getBody().getOpenQuestions().stream()
                .anyMatch(question -> question.contains("Unsupported project file type")));
    }

    @Test
    void planCodeKeepsEligibleSpringConfigurationProposalAvailable() {
        CodingAgent codingAgent = org.mockito.Mockito.mock(CodingAgent.class);
        when(codingAgent.plan(org.mockito.ArgumentMatchers.any(CodingRequest.class)))
                .thenReturn(proposal(
                        "foodMenuProject/src/main/resources/application.yml"));
        WorkspaceChangeMcpTool changeTool = new WorkspaceChangeMcpTool(
                new WorkspaceChangeService(new WorkspaceTool(workspaceRoot.toString())));
        AgentController controller = new AgentController(
                org.mockito.Mockito.mock(ResearchAgent.class),
                codingAgent,
                new McpToolRegistry(List.of(changeTool)),
                org.mockito.Mockito.mock(TestingAgent.class));

        ResponseEntity<CodingResult> response =
                controller.planCode(new CodingRequest());

        assertEquals("PROPOSED", response.getBody().getStatus());
    }

    private CodingResult proposal(String path) {
        CodingResult proposal = new CodingResult();
        proposal.setStatus("PROPOSED");
        CodingResult.FileChangeProposal change = new CodingResult.FileChangeProposal();
        change.setFilePath(path);
        change.setAction("CREATE");
        change.setContent("some file content");
        proposal.setProposedChanges(List.of(change));
        return proposal;
    }
}


package com.devpilot.aiagent.controller;

import com.devpilot.aiagent.agent.CodingAgent;
import com.devpilot.aiagent.agent.ResearchAgent;
import com.devpilot.aiagent.dto.ApplyChangesRequest;
import com.devpilot.aiagent.dto.ApplyChangesResult;
import com.devpilot.aiagent.dto.CodingRequest;
import com.devpilot.aiagent.dto.CodingResult;
import com.devpilot.aiagent.dto.ProjectAnalysis;
import com.devpilot.aiagent.dto.ResearchRequest;
import com.devpilot.aiagent.agent.TestingAgent;
import com.devpilot.aiagent.dto.TestingRequest;
import com.devpilot.aiagent.dto.TestingResult;
import com.devpilot.aiagent.mcp.McpToolRegistry;
import com.devpilot.aiagent.mcp.tools.WorkspaceChangeMcpTool;

import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/agents")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {

    private final ResearchAgent researchAgent;
    private final CodingAgent codingAgent;
    private final WorkspaceChangeMcpTool workspaceChangeTool;
    private final TestingAgent testingAgent;

    public AgentController(ResearchAgent researchAgent, CodingAgent codingAgent,
            McpToolRegistry mcpToolRegistry, TestingAgent testingAgent) {
        this.researchAgent = researchAgent;
        this.codingAgent = codingAgent;
        this.workspaceChangeTool = mcpToolRegistry.getTool(
                WorkspaceChangeMcpTool.NAME,
                java.util.Set.of(WorkspaceChangeMcpTool.NAME),
                WorkspaceChangeMcpTool.class);
        this.testingAgent = testingAgent;
    }

    @PostMapping("/research")
    public ResponseEntity<ProjectAnalysis> research(
            @Valid @RequestBody ResearchRequest request) {

        ProjectAnalysis analysis = researchAgent.research(request);

        return ResponseEntity.ok(analysis);
    }

    @PostMapping("/code/plan")
    public ResponseEntity<CodingResult> planCode(@Valid @RequestBody CodingRequest request) {
        CodingResult result = codingAgent.plan(request);
        if (result == null) {
            throw new IllegalStateException(
                    "Coding Agent returned an empty proposal result.");
        }
        List<String> validationErrors =
                workspaceChangeTool.validateProposal(result);
        if (!validationErrors.isEmpty()) {
            result.setStatus("NEEDS_CLARIFICATION");
            result.setSummary(
                    "Proposal failed workspace preflight validation: "
                            + String.join("; ", validationErrors));
            List<String> openQuestions = result.getOpenQuestions() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(result.getOpenQuestions());
            openQuestions.addAll(validationErrors);
            result.setOpenQuestions(openQuestions);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/code/apply")
    public ResponseEntity<?> applyCodeChanges(
            @Valid @RequestBody ApplyChangesRequest request) {

        try {
            ApplyChangesResult result = workspaceChangeTool.execute(request);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/test")
    public ResponseEntity<TestingResult> test(
            @jakarta.validation.Valid @RequestBody TestingRequest request) {

        TestingResult result = testingAgent.test(request);
        return ResponseEntity.ok(result);
    }

}

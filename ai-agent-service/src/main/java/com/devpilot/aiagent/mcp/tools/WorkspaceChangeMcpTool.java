package com.devpilot.aiagent.mcp.tools;

import com.devpilot.aiagent.dto.ApplyChangesRequest;
import com.devpilot.aiagent.dto.ApplyChangesResult;
import com.devpilot.aiagent.dto.CodingResult;
import com.devpilot.aiagent.mcp.McpTool;
import com.devpilot.aiagent.service.WorkspaceChangeService;
import java.util.List;

public class WorkspaceChangeMcpTool
        implements McpTool<ApplyChangesRequest, ApplyChangesResult> {

    public static final String NAME = "workspace-change";

    private final WorkspaceChangeService workspaceChangeService;

    public WorkspaceChangeMcpTool(WorkspaceChangeService workspaceChangeService) {
        this.workspaceChangeService = workspaceChangeService;
    }

    @Override
    public String toolName() {
        return NAME;
    }

    @Override
    public String description() {
        return "Apply file changes only after explicit approval and proposal validation.";
    }

    @Override
    public ApplyChangesResult execute(ApplyChangesRequest input) {
        return workspaceChangeService.applyApprovedChanges(input);
    }

    public List<String> validateProposal(CodingResult proposal) {
        return workspaceChangeService.validateProposalForApproval(proposal);
    }
}

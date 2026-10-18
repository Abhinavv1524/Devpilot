package com.devpilot.aiagent.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devpilot.aiagent.dto.ApplyChangesRequest;
import com.devpilot.aiagent.dto.CodingResult;
import com.devpilot.aiagent.dto.TestingRequest;
import com.devpilot.aiagent.dto.TestingResult;
import com.devpilot.aiagent.mcp.dto.WorkspaceReadRequest;
import com.devpilot.aiagent.mcp.dto.WorkspaceSearchRequest;
import com.devpilot.aiagent.service.WorkspaceChangeService;
import com.devpilot.aiagent.tools.TestExecutionTool;
import com.devpilot.aiagent.tools.WorkspaceTool;

class McpToolDelegationTest {

    @TempDir
    Path workspaceRoot;

    @Test
    void workspaceReadAndSearchToolsDelegateToWorkspaceTool() throws IOException {
        Path source = workspaceRoot.resolve("project/src/Main.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "class Main { // search-token\n}");
        WorkspaceTool workspaceTool = new WorkspaceTool(workspaceRoot.toString());

        WorkspaceReadMcpTool readTool = new WorkspaceReadMcpTool(workspaceTool);
        WorkspaceSearchMcpTool searchTool = new WorkspaceSearchMcpTool(workspaceTool);

        assertEquals("class Main { // search-token\n}",
                readTool.execute(new WorkspaceReadRequest(
                        WorkspaceReadRequest.Operation.SOURCE_FILE,
                        null,
                        "project/src/Main.java")).content());
        assertEquals(true, readTool.execute(new WorkspaceReadRequest(
                WorkspaceReadRequest.Operation.FILE_EXISTS,
                null,
                "project/src/Main.java")).exists());
        assertTrue(searchTool.execute(
                new WorkspaceSearchRequest("project", "search-token"))
                .contains("src/Main.java:1:"));
    }

    @Test
    void testExecutionToolDelegatesValidationToExistingProvider() {
        TestExecutionMcpTool tool = new TestExecutionMcpTool(
                new TestExecutionTool(workspaceRoot.toString()));

        TestingResult result = tool.execute(new TestingRequest(
                "Run smoke tests",
                "project",
                TestingRequest.TestType.JAVA_SMOKE,
                List.of()));

        assertEquals(TestingResult.Status.REJECTED, result.status());
        assertEquals("At least one smoke test case is required.", result.summary());
    }

    @Test
    void workspaceChangeToolStillRequiresApprovalAndDelegatesApprovedWrites()
            throws IOException {
        WorkspaceChangeMcpTool tool = new WorkspaceChangeMcpTool(
                new WorkspaceChangeService(new WorkspaceTool(workspaceRoot.toString())));
        ApplyChangesRequest request = new ApplyChangesRequest();
        CodingResult proposal = new CodingResult();
        proposal.setStatus("PROPOSED");
        CodingResult.FileChangeProposal change = new CodingResult.FileChangeProposal();
        change.setFilePath("project/src/Main.java");
        change.setAction("CREATE");
        change.setContent("class Main {}");
        proposal.setProposedChanges(List.of(change));
        request.setProposal(proposal);
        request.setApproved(false);

        assertThrows(IllegalArgumentException.class, () -> tool.execute(request));

        request.setApproved(true);
        assertEquals("APPLIED", tool.execute(request).status());
        assertEquals("class Main {}",
                Files.readString(workspaceRoot.resolve("project/src/Main.java")));
    }
}

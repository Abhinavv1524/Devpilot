package com.devpilot.aiagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devpilot.aiagent.dto.CodingResult;
import com.devpilot.aiagent.tools.WorkspaceTool;

class WorkspaceChangeServiceTest {

    @TempDir
    Path workspaceRoot;

    @Test
    void preflightAcceptsAllFilesInAStandardSpringProjectProposal() {
        WorkspaceChangeService service = new WorkspaceChangeService(
                new WorkspaceTool(workspaceRoot.toString()));
        CodingResult proposal = proposal(
                "foodMenuProject/pom.xml",
                "foodMenuProject/README.md",
                "foodMenuProject/.gitignore",
                "foodMenuProject/src/main/java/example/App.java",
                "foodMenuProject/src/main/resources/application.yml",
                "foodMenuProject/src/test/java/example/AppTest.java");

        assertTrue(service.validateProposalForApproval(proposal).isEmpty());
    }

    @Test
    void preflightReportsEveryInvalidTargetBeforeApproval() {
        WorkspaceChangeService service = new WorkspaceChangeService(
                new WorkspaceTool(workspaceRoot.toString()));
        CodingResult proposal = proposal(
                "project/src/Main.java",
                "project/assets/logo.png",
                "project/target/Generated.java");

        List<String> errors = service.validateProposalForApproval(proposal);

        assertEquals(2, errors.size());
        assertTrue(errors.get(0).contains("logo.png"));
        assertTrue(errors.get(0).contains("Unsupported project file type"));
        assertTrue(errors.get(1).contains("target/Generated.java"));
        assertTrue(errors.get(1).contains("excluded directory"));
    }

    @Test
    void preflightRejectsExistingCreateTargetsBeforeApproval() throws Exception {
        Path existingFile = workspaceRoot.resolve("project/src/Main.java");
        java.nio.file.Files.createDirectories(existingFile.getParent());
        java.nio.file.Files.writeString(existingFile, "class Main {}");
        WorkspaceChangeService service = new WorkspaceChangeService(
                new WorkspaceTool(workspaceRoot.toString()));

        List<String> errors = service.validateProposalForApproval(
                proposal("project/src/Main.java"));

        assertEquals(1, errors.size());
        assertTrue(errors.getFirst().contains("CREATE rejected because the target already exists."));
    }

    @Test
    void preflightRejectsBinaryControlCharactersInTextContent() {
        WorkspaceChangeService service = new WorkspaceChangeService(
                new WorkspaceTool(workspaceRoot.toString()));
        CodingResult proposal = proposal("project/resources/data.txt");
        proposal.getProposedChanges().getFirst().setContent("text\u0000data");

        List<String> errors = service.validateProposalForApproval(proposal);

        assertEquals(1, errors.size());
        assertTrue(errors.getFirst().contains("binary control characters"));
    }

    private CodingResult proposal(String... paths) {
        CodingResult proposal = new CodingResult();
        proposal.setStatus("PROPOSED");
        List<CodingResult.FileChangeProposal> changes = new ArrayList<>();
        for (String path : paths) {
            CodingResult.FileChangeProposal change =
                    new CodingResult.FileChangeProposal();
            change.setFilePath(path);
            change.setAction("CREATE");
            change.setContent("Generated content");
            changes.add(change);
        }
        proposal.setProposedChanges(changes);
        return proposal;
    }
}

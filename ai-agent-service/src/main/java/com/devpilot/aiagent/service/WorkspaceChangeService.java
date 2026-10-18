package com.devpilot.aiagent.service;

import com.devpilot.aiagent.dto.ApplyChangesRequest;
import com.devpilot.aiagent.dto.ApplyChangesResult;
import com.devpilot.aiagent.dto.CodingResult;
import com.devpilot.aiagent.tools.WorkspaceTool;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class WorkspaceChangeService {

    private static final int MAX_CHANGES = 20;
    private static final int MAX_FILE_CONTENT_BYTES = 64 * 1024;

    private final WorkspaceTool workspaceTool;

    public WorkspaceChangeService(WorkspaceTool workspaceTool) {
        this.workspaceTool = workspaceTool;
    }

    public ApplyChangesResult applyApprovedChanges(
            ApplyChangesRequest request) {

        if (request == null || !Boolean.TRUE.equals(request.getApproved())) {
            throw new IllegalArgumentException(
                    "Explicit approval is required before applying changes.");
        }

        var proposal = request.getProposal();

        // Validate the complete proposal before touching any files.
        validateProposal(proposal);

        List<String> appliedFiles = new ArrayList<>();

        try {
            for (var change : proposal.getProposedChanges()) {
                workspaceTool.writeApprovedFile(
                        change.getFilePath(),
                        change.getAction(),
                        change.getContent());

                appliedFiles.add(change.getFilePath());
            }

            return new ApplyChangesResult(
                    "APPLIED",
                    "All proposed changes were written successfully.",
                    appliedFiles,
                    List.of());

        } catch (IOException ex) {
            return new ApplyChangesResult(
                    appliedFiles.isEmpty() ? "FAILED" : "PARTIALLY_APPLIED",
                    "File application stopped: " + ex.getMessage(),
                    appliedFiles,
                    List.of(ex.getMessage()));
        }
    }

    public void validateProposal(CodingResult proposal) {
        List<String> errors = validateProposalForApproval(proposal);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                    "Proposal validation failed: " + String.join("; ", errors));
        }
    }

    public List<String> validateProposalForApproval(CodingResult proposal) {
        List<String> errors = new ArrayList<>();
        if (proposal == null) {
            return List.of("Proposal is required.");
        }

        if (!"PROPOSED".equals(proposal.getStatus())) {
            errors.add("Only proposals with PROPOSED status can be applied.");
        }

        var changes = proposal.getProposedChanges();

        if (changes == null || changes.isEmpty()) {
            errors.add("The proposal contains no file changes.");
            return errors;
        }

        if (changes.size() > MAX_CHANGES) {
            errors.add("Proposal exceeds the maximum of " + MAX_CHANGES + " file changes.");
        }

        Set<String> paths = new HashSet<>();

        for (int index = 0; index < changes.size(); index++) {
            var change = changes.get(index);
            String label = "Change " + (index + 1);
            if (change == null) {
                errors.add(label + " is empty.");
                continue;
            }

            String path = change.getFilePath();
            String action = change.getAction();
            String content = change.getContent();

            if (path == null || path.isBlank()) {
                errors.add(label + " does not specify a file path.");
                continue;
            }

            if (!paths.add(path.replace('\\', '/').toLowerCase(java.util.Locale.ROOT))) {
                errors.add(label + " duplicates a target path: " + path);
                continue;
            }

            if (!"CREATE".equals(action) && !"MODIFY".equals(action)) {
                errors.add(label + " uses an unsupported action: " + action);
                continue;
            }

            if (content == null || content.isBlank()) {
                errors.add(label + " has no file content: " + path);
                continue;
            }
            if (containsNonTextControlCharacter(content)) {
                errors.add(label + " contains binary control characters: " + path);
                continue;
            }

            int contentSize = content.getBytes(
                    java.nio.charset.StandardCharsets.UTF_8).length;

            if (contentSize > MAX_FILE_CONTENT_BYTES) {
                errors.add(label + " exceeds the 64 KiB file size limit: " + path);
                continue;
            }

            try {
                workspaceTool.validateWriteTarget(path, action);
            } catch (IOException exception) {
                errors.add(label + " (" + path + "): " + exception.getMessage());
            }
        }

        return List.copyOf(errors);
    }

    private boolean containsNonTextControlCharacter(String content) {
        return content.chars().anyMatch(character ->
                Character.isISOControl(character)
                        && character != '\n'
                        && character != '\r'
                        && character != '\t');
    }
}
package com.devpilot.aiagent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

import com.devpilot.aiagent.dto.CodingRequest;
import com.devpilot.aiagent.dto.CodingResult;
import com.devpilot.aiagent.dto.ProjectAnalysis;
import com.devpilot.aiagent.dto.TestingResult;
import com.devpilot.aiagent.dto.TestingRequest;
import com.devpilot.aiagent.mcp.McpToolRegistry;
import com.devpilot.aiagent.mcp.AgentMcpToolAccess;
import com.devpilot.aiagent.mcp.dto.WorkspaceReadRequest;
import com.devpilot.aiagent.mcp.dto.WorkspaceSearchRequest;
import com.devpilot.aiagent.mcp.tools.WorkspaceReadMcpTool;
import com.devpilot.aiagent.mcp.tools.WorkspaceSearchMcpTool;

@Service
public class CodingAgent {
    private final ChatClient chatClient;
    private final WorkspaceReadMcpTool workspaceReadTool;
    private final WorkspaceSearchMcpTool workspaceSearchTool;

    public CodingAgent(ChatClient.Builder chatClientBuilder, McpToolRegistry mcpToolRegistry) {
        this.chatClient = chatClientBuilder.build();
        this.workspaceReadTool = mcpToolRegistry.getTool(
                WorkspaceReadMcpTool.NAME,
                AgentMcpToolAccess.ALLOWED_TOOLS.get("Coding Agent"),
                WorkspaceReadMcpTool.class);
        this.workspaceSearchTool = mcpToolRegistry.getTool(
                WorkspaceSearchMcpTool.NAME,
                AgentMcpToolAccess.ALLOWED_TOOLS.get("Coding Agent"),
                WorkspaceSearchMcpTool.class);
    }

    public CodingResult plan(CodingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Project analysis is required.");
        }

        boolean repairMode = request.getRepairDiagnostics() != null;
        if (request.getProjectAnalysis() == null) {
            throw new IllegalArgumentException(
                    "Project analysis is required.");
        }

        if (repairMode) {
            validateRepairRequest(request.getRepairDiagnostics(),
                    request.getRepairAttempt());
        }

        ProjectAnalysis analysis = request.getProjectAnalysis();
        String inspectedFiles = inspectCandidateFiles(
                analysis, request.getProjectPath());
        String projectContext = workspaceReadTool.execute(
                new WorkspaceReadRequest(
                        WorkspaceReadRequest.Operation.PROJECT_CONTEXT,
                        request.getProjectPath(),
                        null,
                        analysis.getTask()))
                .content();
        inspectedFiles += "\nREQUESTED PROJECT SOURCE CONTEXT:\n"
                + projectContext
                + "\n";
        inspectedFiles += appendWorkspaceSearchResults(
                analysis, request.getProjectPath());
        boolean hasExistingFilesToModify = analysis.getFilesToModify() != null
                && !analysis.getFilesToModify().isEmpty();
        boolean hasSourceContext = inspectedFiles.contains(
                "INSPECTION_STATUS: SUCCESS")
                || !projectContext.startsWith("No existing project files found")
                        && !projectContext.contains("No eligible source files found.");

        if (hasExistingFilesToModify
                && (inspectedFiles.contains("INSPECTION_STATUS: FAILED")
                        || inspectedFiles.contains("INSPECTION FAILED:"))) {
            if (!hasSourceContext) {
                CodingResult result = new CodingResult();
                result.setStatus("NEEDS_CLARIFICATION");
                result.setSummary(
                        "Existing project files could not be inspected. "
                                + "No speculative file replacement was generated.");
                result.getRisks().add(
                        "The agent must inspect the actual source before proposing a replacement.");
                result.getOpenQuestions().add(
                        "Verify the requested project root and exact existing file paths.");
                return result;
            }
        }

        CodingResult result;
        if (repairMode) {
            TestingResult diagnostics = request.getRepairDiagnostics();
            result = chatClient.prompt()
                    .system(
                            """
                                    You are the Coding Agent in DevPilot AI handling a repair proposal.

                                    Rules:
                                    1. Diagnose the reported test failure using the actual inspected source files
                                       and the supplied test diagnostics.
                                    2. Propose the smallest targeted correction that addresses the failure.
                                    3. Do not propose unrelated changes, refactoring, or speculative replacements.
                                    4. Return status PROPOSED when a useful repair proposal can be produced, or
                                       NEEDS_CLARIFICATION when essential information is missing.
                                    5. Never claim that files have been changed, tests have passed, or commands have run.
                                    6. Do not execute commands, modify files, or bypass user approval.
                                    7. For every proposed file change, return the complete proposed file contents
                                       in the content field, without Markdown fences.
                                    8. For MODIFY, return a complete replacement of the actual inspected file,
                                       not a partial code fragment or diff.
                                    9. Do not invent unseen existing code. If a required existing file could not be
                                       inspected, return NEEDS_CLARIFICATION rather than fabricating its contents.
                                    Return only a structured CodingResult.
                                    """)
                    .user("""
                            Create a repair proposal for this ProjectAnalysis:

                            %s

                            ACTUAL SOURCE FILE CONTENTS:
                            %s

                            ACTUAL TEST DIAGNOSTICS:
                            %s

                            SUPPLIED SMOKE TESTS:
                            %s

                            Use the actual source contents and diagnostics above to diagnose the failure.
                            Preserve each expected output exactly. Propose only the smallest targeted
                            correction and return complete file contents for every proposed change.
                            """.formatted(
                            toAnalysisText(analysis),
                            inspectedFiles,
                            toDiagnosticText(diagnostics,
                                    request.getRepairAttempt()),
                            toSmokeTestsText(request.getSmokeTests())))
                    .call()
                    .entity(CodingResult.class);
        } else {
            result = chatClient.prompt()
                    .system(
                            """
                                           You are the Coding Agent in DevPilot AI.

                                        Your responsibility is to create a structured
                                        implementation proposal from ProjectAnalysis.

                                        Rules:
                                        1. Propose changes only for the user's task.
                                        2. Use the supplied requirements, technologies,
                                           recommendations, and risks.
                                        3. Use filesToModify and newFiles as candidate
                                           paths, not proof that files exist.
                                        4. Do not invent existing file contents.
                                        5. Do not invent dependencies without justification.
                                        6. Prefer the smallest implementation that meets
                                           the requirements.
                                        7. Use action MODIFY for existing files and CREATE
                                           for new files. If existence is uncertain, explain
                                           that uncertainty in the description or risks.
                                        8. Include relevant risks and unresolved questions.
                                        9. Prefer a useful proposal with clearly stated assumptions
                                           over NEEDS_CLARIFICATION when reasonable implementation
                                           choices can be inferred from the task or project conventions.
                                           Return NEEDS_CLARIFICATION only when missing information
                                           makes a safe, useful proposal impossible.
                                        10. Never claim that files have been changed,
                                            tests have passed, or commands have run.
                                        11. Do not execute commands or modify files.
                                        12. For every proposed file change, return the complete
                                            proposed file contents in the content field.
                                        13. For MODIFY, generate a complete replacement file,
                                            not a partial code fragment or diff.
                                        14. For CREATE, generate the complete new file contents.
                                        15. Do not invent unseen existing code. For MODIFY actions,
                                            inspect the actual existing file before proposing a replacement.
                                            If an existing file required for modification cannot be inspected,
                                            return NEEDS_CLARIFICATION rather than fabricating its contents.
                                            For CREATE actions, the target file does not need to exist.
                                            Generate complete new file contents from the supplied requirements
                                            without requiring inspection of the nonexistent target.
                                        16. If essential information is missing, do not generate
                                            speculative file contents.
                                                16a. Do not treat unspecified implementation details
                                                    (such as route naming, ID types, response DTOs,
                                                    pagination, package names, or test conventions) as
                                                    blockers when they can be inferred from the supplied
                                                    project source. Follow existing conventions; otherwise
                                                    choose the smallest conventional implementation and
                                                    record the assumption.
                                                16b. Ask only about genuine blockers that cannot be
                                                    resolved from the task or project. Do not create a
                                                    separate open question for each design choice.
                                                16c. For a request to add one data-retrieval endpoint,
                                                    do not block on whether a collection endpoint is also
                                                    wanted. Follow the closest existing route convention;
                                                    for a named record and an existing ID-based API, propose
                                                    GET by ID and state any inferred ID type as an assumption.
                                                16d. For a new project containing only CREATE actions, generate a
                                                    concrete proposal even when no existing files are available
                                                    for inspection, provided the requirements are sufficient.

                                                16e. When the task supplies smoke tests, include the supplied tests
                                                    in the implementation plan and ensure the proposed code's output
                                                    matches their expected output exactly.

                                                16f. Do not create DevPilot platform configuration files, test-runner
                                                    configuration files, or custom smoke-test JSON files unless the
                                                    user explicitly requests them or the supplied project requirements
                                                    require them.

                                                16g. Smoke tests are supplied to the orchestration/testing workflow.
                                                    Use them to validate the proposed implementation's expected behavior;
                                                    do not invent a separate configuration format for those tests.

                                                16h. For a simple Java demonstration project whose requirements specify
                                                    the source path and expected output, create the required Java source
                                                    file and only essential supporting documentation. Return PROPOSED
                                                    when the implementation can be generated without unresolved
                                                    essential information.

                                        17. The content field must contain source code only,
                                            without Markdown fences.
                                        18. Preserve every explicitly specified string literal exactly,
                                            character for character, including spaces, punctuation,
                                            capitalization, and apostrophes.
                                        19. Before returning proposed Java code, verify that all required
                                            messages appear exactly as specified in the requirements.
                                        20. Do not remove, insert, or alter spaces inside required strings.
                                        21. For overflow handling, use this exact message when specified:
                                        "Error: integer addition overflow. Please enter a different second integer."
                                        22. Ensure the proposed implementation satisfies every smoke test
                                            supplied in the task, including expected console output and
                                            newline placement.
                                        23. Treat projectPath only as the filesystem project root. Do not
                                            infer a Java package name from it. When no package is specified,
                                            choose one consistent with the requested application and do not
                                            reuse an unrelated example or previous task's package.
                                        24. Every proposed file change must have non-blank complete file
                                            content. Omit unnecessary files instead of proposing empty files.
                                        Return only a structured CodingResult.
                                    """)
                    .user("""
                            The authoritative project root is: %s

                            Every proposed filePath must be relative to the configured workspace
                            and must start with that exact project root. For a new project,
                            place pom.xml, source files, resources, and tests directly under
                            that root. Do not create another module directory such as MovieApi
                            beneath it. Do not replace the requested root with a name inferred
                            from the task, package name, or ProjectAnalysis.

                            Create an implementation proposal for this ProjectAnalysis:

                            %s

                            ACTUAL SOURCE FILE CONTENTS:
                            %s

                            SUPPLIED SMOKE TESTS:
                            %s

                            Use all observed source files and project conventions in the supplied
                            context to locate the relevant implementation. Candidate file paths and
                            workspace search matches are hints, not an exhaustive list; never interpret
                            a glob or descriptive note as a literal file path.
                            Use the actual source contents when proposing modifications.
                            Do not invent unseen existing code.
                            If a required file could not be inspected, do not guess its contents.
                            For non-blocking ambiguity, choose and state the smallest conventional
                            assumption, then return a PROPOSED implementation rather than a list of
                            questions.
                            """.formatted(
                                    request.getProjectPath() == null || request.getProjectPath().isBlank()
                                            ? "(not specified)"
                                            : request.getProjectPath(),
                                    toAnalysisText(analysis),
                                    inspectedFiles,
                                    toSmokeTestsText(request.getSmokeTests())))
                    .call()
                    .entity(CodingResult.class);
        }
        if (result == null) {
            if (repairMode) {
                CodingResult clarification = new CodingResult();
                clarification.setStatus("NEEDS_CLARIFICATION");
                clarification.setSummary(
                        "The Coding Agent returned an unusable repair response.");
                return clarification;
            }

            throw new IllegalStateException(
                    "Coding Agent returned an empty result.");
        }

        if (!repairMode
                && "NEEDS_CLARIFICATION".equals(result.getStatus())
                && (result.getProposedChanges() == null
                        || result.getProposedChanges().isEmpty())) {
            CodingResult revisedProposal = retryAsProposal(
                    analysis,
                    inspectedFiles,
                    request,
                    result);
            if (isUsableProposal(revisedProposal)) {
                result = revisedProposal;
            }
        }

        if (!repairMode
                && (result.getStatus() == null
                        || result.getStatus().isBlank())) {
            result.setStatus("PROPOSED");
        }

        if (repairMode
                && !"PROPOSED".equals(result.getStatus())
                && !"NEEDS_CLARIFICATION".equals(result.getStatus())) {
            result.setStatus("NEEDS_CLARIFICATION");
        }

        if (result.getProposedChanges() == null) {
            result.setProposedChanges(
                    new java.util.ArrayList<>());
        }

        if (result.getRisks() == null) {
            result.setRisks(new java.util.ArrayList<>());
        }

        if (result.getOpenQuestions() == null) {
            result.setOpenQuestions(new java.util.ArrayList<>());
        }

        ProjectPathNormalizer.normalize(
                result, request.getProjectPath(), analysis);
        normalizeActionsToWorkspace(result);
        promoteCompleteInitialProposal(result, repairMode);
        return result;
    }

    static void promoteCompleteInitialProposal(
            CodingResult result,
            boolean repairMode) {
        if (repairMode
                || result == null
                || !"NEEDS_CLARIFICATION".equals(result.getStatus())
                || result.getProposedChanges() == null
                || result.getProposedChanges().isEmpty()
                || result.getProposedChanges().stream().anyMatch(change ->
                        change == null
                                || change.getFilePath() == null
                                || change.getFilePath().isBlank()
                                || change.getContent() == null
                                || change.getContent().isBlank()
                                || !"CREATE".equals(change.getAction())
                                        && !"MODIFY".equals(change.getAction()))) {
            return;
        }

        result.setStatus("PROPOSED");
        String summary = result.getSummary();
        result.setSummary((summary == null || summary.isBlank()
                ? "A complete implementation proposal is available."
                : summary)
                + " Review the listed open questions and assumptions before approval.");
    }

    private CodingResult retryAsProposal(
            ProjectAnalysis analysis,
            String inspectedFiles,
            CodingRequest request,
            CodingResult clarification) {
        return chatClient.prompt()
                .system("""
                        You are revising an initial CodingResult that asked questions
                        instead of proposing implementation changes.

                        Make one complete, concrete implementation proposal from the
                        supplied task analysis and actual project context. Ordinary
                        choices such as an endpoint route, an ID type, response shape,
                        pagination, package names, and test conventions are not blockers:
                        infer them from observed source and existing API patterns. If no
                        convention is visible, choose the smallest conventional option
                        and state it as an assumption or risk.

                        Treat any initial question that already describes its answer as
                        an assumption, typical convention, or inference from existing
                        code as non-blocking. Do not ask the user to confirm that choice.
                        When asked for one new data-retrieval endpoint without a list
                        requirement, do not ask whether a list endpoint is also needed.
                        Choose the narrowest matching existing route convention and
                        state that assumption.
                        If the project already has an ID-based delete endpoint and the
                        task is to get a named record, implement GET by ID. Infer the ID
                        type from the inspected entity and repository; if unavailable,
                        use the ID type supported by the project language/framework and
                        state it as an assumption rather than asking for confirmation.
                        Only return NEEDS_CLARIFICATION if the task truly cannot be
                        implemented safely with the evidence and assumptions available.
                        Do not invent unseen existing source. For each MODIFY proposal,
                        use the complete contents of the actual inspected file. For new
                        files, provide complete contents. Return status PROPOSED and at
                        least one file change whenever a safe implementation is possible.
                        Never claim that files were changed or tests were run.
                        Return only a structured CodingResult.
                        """)
                .user("""
                        Project analysis:
                        %s

                        Requested project root:
                        %s

                        Actual inspected project context:
                        %s

                        Supplied smoke tests:
                        %s

                        Initial clarification response:
                        %s

                        Revise the response into a PROPOSED implementation. Resolve
                        non-blocking questions using the task and observed conventions.
                        Include only relevant file changes and complete file contents.
                        """.formatted(
                        toAnalysisText(analysis),
                        valueOrDefault(request.getProjectPath()),
                        inspectedFiles,
                        toSmokeTestsText(request.getSmokeTests()),
                        toCodingResultText(clarification)))
                .call()
                .entity(CodingResult.class);
    }

    private boolean isUsableProposal(CodingResult result) {
        return result != null
                && "PROPOSED".equals(result.getStatus())
                && result.getProposedChanges() != null
                && !result.getProposedChanges().isEmpty();
    }

    private String toCodingResultText(CodingResult result) {
        return """
                Status: %s
                Summary: %s
                Open questions: %s
                Risks: %s
                """.formatted(
                valueOrDefault(result.getStatus()),
                valueOrDefault(result.getSummary()),
                result.getOpenQuestions(),
                result.getRisks());
    }

    private void normalizeActionsToWorkspace(CodingResult result) {
        for (var change : result.getProposedChanges()) {
            if (change == null || change.getFilePath() == null
                    || change.getAction() == null) {
                continue;
            }

            try {
                boolean exists = workspaceReadTool.execute(
                        new WorkspaceReadRequest(
                                WorkspaceReadRequest.Operation.FILE_EXISTS,
                                null,
                                change.getFilePath()))
                        .exists();
                if ("MODIFY".equals(change.getAction()) && !exists) {
                    change.setAction("CREATE");
                } else if ("CREATE".equals(change.getAction()) && exists) {
                    change.setAction("MODIFY");
                }
            } catch (IllegalStateException | IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Unable to validate proposed file path: "
                                + change.getFilePath(), ex);
            }
        }

    }

    private void validateRepairRequest(TestingResult diagnostics,
            Integer repairAttempt) {
        if (diagnostics.status() == null
                || diagnostics.status() == TestingResult.Status.PASSED) {
            throw new IllegalArgumentException(
                    "Repair diagnostics must contain a non-PASSED test status.");
        }

        if (repairAttempt == null || repairAttempt < 1) {
            throw new IllegalArgumentException(
                    "Repair attempt must be a positive integer.");
        }
    }

    private String toDiagnosticText(TestingResult diagnostics,
            Integer repairAttempt) {
        return """
                Test status: %s
                Test summary: %s
                Test type: %s
                Test command: %s
                Exit code: %s
                Standard output: %s
                Standard error: %s
                Timed out: %s
                Errors: %s
                Repair attempt: %s
                """.formatted(
                diagnostics.status() == null
                        ? "Not specified"
                        : diagnostics.status().name(),
                valueOrDefault(diagnostics.summary()),
                valueOrDefault(diagnostics.testType()),
                valueOrDefault(diagnostics.testCommand()),
                String.valueOf(diagnostics.exitCode()),
                valueOrDefault(diagnostics.standardOutput()),
                valueOrDefault(diagnostics.standardError()),
                diagnostics.timedOut(),
                String.valueOf(diagnostics.errors()),
                String.valueOf(repairAttempt));
    }

    private String toSmokeTestsText(
            List<TestingRequest.SmokeTestCase> smokeTests) {
        if (smokeTests == null || smokeTests.isEmpty()) {
            return "None supplied.";
        }

        StringBuilder formatted = new StringBuilder();
        for (int index = 0; index < smokeTests.size(); index++) {
            TestingRequest.SmokeTestCase smokeTest = smokeTests.get(index);
            formatted.append("Test ").append(index + 1).append(":\n")
                    .append("Input lines:\n");
            if (smokeTest.inputLines() != null) {
                for (String line : smokeTest.inputLines()) {
                    formatted.append(line).append('\n');
                }
            }
            formatted.append("Expected output exactly:\n<<<\n")
                    .append(smokeTest.expectedOutput())
                    .append("\n>>>\n");
        }
        return formatted.toString();
    }

    private String toAnalysisText(ProjectAnalysis analysis) {
        return """
                Task: %s
                Project type: %s
                Technologies: %s
                Requirements: %s
                Candidate files to modify: %s
                Candidate new files: %s
                Dependencies: %s
                Recommendations: %s
                Assumptions: %s
                Open questions: %s
                Risks: %s
                Further inspection required: %s
                """.formatted(
                valueOrDefault(analysis.getTask()),
                valueOrDefault(analysis.getProjectType()),
                analysis.getTechnologies(),
                analysis.getRequirements(),
                analysis.getFilesToModify(),
                analysis.getNewFiles(),
                analysis.getDependencies(),
                analysis.getRecommendations(),
                analysis.getAssumptions(),
                analysis.getOpenQuestions(),
                analysis.getRisks(),
                analysis.isRequiresProjectInspection());
    }

    private String valueOrDefault(String value) {
        return value == null || value.isBlank()
                ? "Not specified"
                : value;
    }

    private String inspectCandidateFiles(
            ProjectAnalysis analysis,
            String projectPath) {
        var paths = analysis.getFilesToModify();

        if (paths == null || paths.isEmpty()) {
            return """
                    No candidate existing file paths were supplied.
                    Use the requested project source context to locate existing
                    files before proposing modifications.
                    """;
        }

        StringBuilder inspected = new StringBuilder();
        int maxFiles = 5;
        int count = 0;
        int successfulReads = 0;

        for (String path : paths) {
            if (count >= maxFiles) {
                inspected.append("\nInspection limit reached.\n");
                break;
            }

            if (path == null || path.isBlank()) {
                continue;
            }

            count++;
            try {
                String workspacePath = ProjectPathNormalizer.resolveWorkspacePath(
                        path, projectPath);
                inspected.append("\n--- File: ")
                        .append(workspacePath)
                        .append(" ---\n");

                String content = workspaceReadTool.execute(
                        new WorkspaceReadRequest(
                                WorkspaceReadRequest.Operation.SOURCE_FILE,
                                null,
                                workspacePath))
                        .content();

                inspected.append("INSPECTION_STATUS: SUCCESS\n")
                        .append("SOURCE_CONTENT:\n")
                        .append(content)
                        .append("\n");

                successfulReads++;
            } catch (IllegalStateException | IllegalArgumentException ex) {
                inspected.append("INSPECTION_STATUS: FAILED\n")
                        .append("REASON: ")
                        .append(ex.getMessage())
                        .append("\n");
            }
        }

        inspected.append("\nSuccessfully inspected files: ")
                .append(successfulReads)
                .append(" of ")
                .append(count)
                .append(".\n");

        return inspected.toString();
    }

    private String appendWorkspaceSearchResults(
            ProjectAnalysis analysis,
            String projectPath) {
        String query = valueOrDefault(analysis.getTask());
        if (query.length() > 200) {
            query = query.substring(0, 200);
        }

        try {
            return "\nWORKSPACE SEARCH RESULTS:\n"
                    + workspaceSearchTool.execute(
                            new WorkspaceSearchRequest(projectPath, query))
                    + "\n";
        } catch (RuntimeException exception) {
            return "\nWORKSPACE SEARCH FAILED: "
                    + exception.getMessage()
                    + "\n";
        }
    }

}

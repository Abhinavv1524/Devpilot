
package com.devpilot.orchestrator.service;

import com.devpilot.orchestrator.client.AgentServiceClient;
import com.devpilot.orchestrator.history.TaskHistoryService;
import com.devpilot.orchestrator.exception.TaskNotFoundException;
import com.devpilot.orchestrator.model.ApplyChangesResult;
import com.devpilot.orchestrator.model.CodingResult;
import com.devpilot.orchestrator.model.ProjectAnalysis;
import com.devpilot.orchestrator.model.TaskRequest;
import com.devpilot.orchestrator.model.TaskState;
import com.devpilot.orchestrator.model.TestingResult;
import com.devpilot.orchestrator.routing.AgentType;
import com.devpilot.orchestrator.routing.AiTaskRouter;
import com.devpilot.orchestrator.routing.RoutingDecision;
import com.devpilot.orchestrator.routing.TaskRouter;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.devpilot.orchestrator.config.AsyncConfig;
import com.devpilot.orchestrator.model.TaskStatus;

@Service
public class OrchestrationService {
        private static final Logger LOG = LoggerFactory.getLogger(OrchestrationService.class);

        private final TaskStore taskStore;
        private final TaskRouter taskRouter;
        private final AiTaskRouter aiTaskRouter;
        private final AgentServiceClient agentServiceClient;
        private final TaskHistoryService taskHistoryService;
        private final ThreadPoolTaskExecutor taskExecutor;

        @Value("${devpilot.workflow.max-repair-attempts:2}")
        private int maxRepairAttempts = 2;

        public OrchestrationService(TaskStore taskStore, TaskRouter taskRouter, AiTaskRouter aiTaskRouter,
                        AgentServiceClient agentServiceClient) {
                this(taskStore, taskRouter, aiTaskRouter, agentServiceClient, null, null);
        }

        public OrchestrationService(TaskStore taskStore, TaskRouter taskRouter, AiTaskRouter aiTaskRouter,
                        AgentServiceClient agentServiceClient, TaskHistoryService taskHistoryService) {
                this(taskStore, taskRouter, aiTaskRouter, agentServiceClient, taskHistoryService, null);
        }

        @Autowired
        public OrchestrationService(TaskStore taskStore, TaskRouter taskRouter, AiTaskRouter aiTaskRouter,
                        AgentServiceClient agentServiceClient, TaskHistoryService taskHistoryService,
                        @Qualifier(AsyncConfig.TASK_EXECUTOR) ThreadPoolTaskExecutor taskExecutor) {
                this.taskStore = taskStore;
                this.taskRouter = taskRouter;
                this.aiTaskRouter = aiTaskRouter;
                this.agentServiceClient = agentServiceClient;
                this.taskHistoryService = taskHistoryService;
                this.taskExecutor = taskExecutor;
        }

        /**
         * Synchronous entry point used by unit tests; runs the full workflow inline.
         */
        TaskState createTask(TaskRequest request) {
                TaskState task = newTaskState(request, null);
                return runWorkflow(request, task, null);
        }

        /**
         * Public API entry point: persists the task as RECEIVED and dispatches the
         * workflow to a background thread, returning immediately so clients can poll.
         */
        public TaskState createTask(TaskRequest request, String ownerUsername) {
                TaskState task = newTaskState(request, ownerUsername);

                if (taskExecutor == null) {
                        // No executor available (e.g. isolated unit test) — run inline.
                        return runWorkflow(request, task, ownerUsername);
                }

                taskExecutor.execute(() -> runWorkflow(request, task, ownerUsername));
                return task;
        }

        private TaskState newTaskState(TaskRequest request, String ownerUsername) {
                TaskState task = new TaskState(request.getDescription(), ownerUsername);
                task.setProjectPath(request.getProjectPath());
                task.setSmokeTests(request.getSmokeTests());
                taskStore.save(task);
                return task;
        }

        /**
         * Requests cancellation of a non-terminal task. Tasks that have not yet left
         * RECEIVED are cancelled immediately; running workflows stop at the next
         * stage boundary because they check {@code cancelRequested} cooperatively.
         */
        public TaskState cancelTask(String taskId, String ownerUsername) {
                TaskState task = getTask(taskId, ownerUsername);

                if (task.isTerminal()) {
                        throw new IllegalStateException(
                                        "Task has already finished and cannot be cancelled. Current status: "
                                                        + task.getStatus());
                }

                task.setCancelRequested(true);
                if (TaskStatus.RECEIVED.name().equals(task.getStatus())) {
                        task.setStatus(TaskStatus.CANCELLED.name());
                        task.setMessage("Task was cancelled before processing started.");
                } else {
                        task.setMessage("Cancellation requested. The workflow will stop at the next stage boundary.");
                }
                taskStore.save(task);
                return task;
        }

        /**
         * Marks the task CANCELLED if a cancellation was requested; returns true when
         * the caller should stop processing.
         */
        private boolean cancelCheckpoint(TaskState task) {
                if (!task.isCancelRequested()) {
                        return false;
                }
                if (!TaskStatus.CANCELLED.name().equals(task.getStatus())) {
                        task.setStatus(TaskStatus.CANCELLED.name());
                        task.setMessage("Task was cancelled by the user.");
                        taskStore.save(task);
                        LOG.info("Task {} cancelled at stage boundary.", task.getTaskId());
                }
                return true;
        }

        TaskState runWorkflow(TaskRequest request, TaskState task, String ownerUsername) {
                if (cancelCheckpoint(task)) {
                        return task;
                }

                RoutingDecision decision;

                try {
                        decision = aiTaskRouter.route(request.getDescription());

                        if (!isValidDecision(decision)) {
                                throw new IllegalArgumentException(
                                                "AI returned an invalid routing decision.");
                        }
                } catch (Exception exception) {
                        LOG.warn(
                                        "AI routing failed with exception type: {}. Using fallback.",
                                        exception.getClass().getSimpleName());

                        decision = taskRouter.route(request.getDescription());

                        task.setMessage(
                                        "AI routing unavailable; used keyword-based fallback.");
                }

                task.setRoutingDecision(decision);
                taskStore.save(task);

                boolean requiresResearch = decision.getAgents().contains(AgentType.RESEARCH);

                boolean requiresCoding = decision.getAgents().contains(AgentType.CODING);

                /*
                 * Coding requires ProjectAnalysis, so run Research first
                 * even when the router selects CODING without RESEARCH.
                 */
                if (requiresResearch || requiresCoding) {
                        if (cancelCheckpoint(task)) {
                                return task;
                        }

                        task.setStatus("RESEARCHING");
                        task.setMessage("Research Agent is analyzing the task.");
                        taskStore.save(task);

                        try {
                                ProjectAnalysis analysis;
                                String relatedTaskContext = taskHistoryService == null
                                                || task.getProjectPath() != null
                                                                && !task.getProjectPath().isBlank()
                                                                                ? null
                                                                                : taskHistoryService.formatContext(
                                                                                                taskHistoryService
                                                                                                                .findRelevant(
                                                                                                                                ownerUsername,
                                                                                                                                request.getDescription()));
                                analysis = agentServiceClient.research(
                                                request.getDescription(),
                                                true,
                                                task.getProjectPath(),
                                                relatedTaskContext);

                                task.setProjectAnalysis(analysis);

                                if (requiresCoding) {
                                        if (cancelCheckpoint(task)) {
                                                return task;
                                        }

                                        task.setStatus("CODING");
                                        task.setMessage(
                                                        "Research completed. Coding Agent is preparing a proposal.");
                                        taskStore.save(task);

                                        CodingResult codingResult = agentServiceClient.planCode(
                                                        analysis, task.getProjectPath(), task.getSmokeTests());

                                        if (!hasExpectedProjectPath(codingResult, task.getProjectPath())) {
                                                task.setStatus("FAILED");
                                                task.setMessage("The Coding Agent returned file paths outside the requested project path.");
                                                taskStore.save(task);
                                                return task;
                                        }

                                        if (isValidRepairProposal(codingResult)) {
                                                task.setCodingResult(codingResult);
                                                task.setStatus("AWAITING_APPROVAL");
                                                task.setMessage(
                                                                "Coding proposal is ready. Review and approve changes before applying them.");
                                        } else {
                                                task.setCodingResult(codingResult);
                                                task.setStatus("FAILED");
                                                task.setMessage(describeInvalidInitialProposal(codingResult));
                                        }

                                } else {
                                        task.setStatus("COMPLETED");
                                        task.setMessage(
                                                        "Research Agent completed the task analysis.");
                                }

                        } catch (Exception exception) {
                                LOG.error(
                                                "Agent workflow failed for task {}. Exception type: {}",
                                                task.getTaskId(),
                                                exception.getClass().getSimpleName());

                                task.setStatus("FAILED");
                                task.setMessage(
                                                "The research or coding step failed: "
                                                                + safeFailureMessage(exception));
                        }

                        taskStore.save(task);
                }

                return task;
        }

        private String safeFailureMessage(Exception exception) {
                String message = exception.getMessage();
                if (message == null || message.isBlank()) {
                        return exception.getClass().getSimpleName();
                }
                return message.length() > 500
                                ? message.substring(0, 500)
                                : message;
        }

        TaskState approveAndApply(String taskId) {
                return approveAndApply(taskId, null);
        }

        public TaskState approveAndApply(String taskId, String ownerUsername) {
                TaskState task = getTask(taskId, ownerUsername);

                if (!"AWAITING_APPROVAL".equals(task.getStatus())) {
                        throw new IllegalStateException(
                                        "Task is not awaiting approval. Current status: "
                                                        + task.getStatus());
                }

                if (task.getCodingResult() == null) {
                        throw new IllegalStateException(
                                        "Task has no coding proposal to approve.");
                }

                task.setAutomaticRepairsAuthorized(true);
                return applyAndTest(task, task.getCodingResult(), true);
        }

        private TaskState applyAndTest(TaskState task, CodingResult proposal,
                        boolean initialApplication) {

                if (cancelCheckpoint(task)) {
                        return task;
                }

                ApplyChangesResult applyResult;
                try {
                        applyResult = agentServiceClient.applyCode(proposal);
                } catch (Exception exception) {
                        LOG.error(
                                        "Code application failed for task {}. Exception type: {}. Reason: {}",
                                        task.getTaskId(),
                                        exception.getClass().getSimpleName(),
                                        exception.getMessage());

                        task.setStatus("FAILED");
                        String failureSummary = initialApplication
                                        ? "Approved coding proposal could not be applied. Check the Orchestrator logs for details."
                                        : "Automatic repair could not be applied. Check the Orchestrator logs for details.";
                        task.setMessage(failureSummary + " Reason: " + safeFailureMessage(exception));
                        taskStore.save(task);
                        return task;
                }

                if (applyResult == null) {
                        task.setStatus("FAILED");
                        task.setMessage("Code proposal could not be applied: the Coding Agent returned no apply result.");
                        taskStore.save(task);
                        return task;
                }

                if (!"APPLIED".equals(applyResult.status())) {
                        task.setStatus("FAILED");
                        task.setMessage("Code proposal could not be fully applied: " + applyResult.message());
                        taskStore.save(task);
                        return task;
                }

                boolean testingRequested = task.getRoutingDecision() != null
                                && (task.getRoutingDecision().isRequiresTesting()
                                                || task.getRoutingDecision().getAgents()
                                                                .contains(AgentType.TESTING)
                                                || (task.getSmokeTests() != null
                                                                && !task.getSmokeTests().isEmpty()));

                if (!testingRequested) {
                        task.setStatus("COMPLETED");
                        task.setMessage("Approved coding proposal applied successfully. Testing was not requested.");
                        taskStore.save(task);
                        return task;
                }

                if (task.getProjectPath() == null
                                || task.getProjectPath().isBlank()) {
                        task.setStatus("FAILED");
                        task.setMessage(
                                        "Code was applied, but testing could not start: "
                                                        + "provide a workspace-relative projectPath.");
                        taskStore.save(task);
                        return task;
                }

                task.setStatus("TESTING");
                task.setMessage(
                                task.getSmokeTests() == null || task.getSmokeTests().isEmpty()
                                                ? "Code applied successfully. Testing Agent is running Maven tests."
                                                : "Code applied successfully. Testing Agent is running smoke tests.");
                taskStore.save(task);

                while (true) {
                        if (cancelCheckpoint(task)) {
                                return task;
                        }

                        TestingResult testingResult;
                        try {
                                testingResult = agentServiceClient.testProject(
                                                task.getDescription(), task.getProjectPath(), task.getSmokeTests());
                        } catch (Exception exception) {
                                LOG.error("Testing failed for task {}. Exception type: {}",
                                                task.getTaskId(), exception.getClass().getSimpleName());
                                return fail(task,
                                                "Code was applied, but the Testing Agent could not complete the test request.");
                        }

                        task.setTestingResult(testingResult);
                        if (isPassed(testingResult)) {
                                task.setStatus("COMPLETED");
                                task.setMessage("Code applied successfully and smoke tests passed.");
                                taskStore.save(task);
                                return task;
                        }

                        if (testingResult == null) {
                                return fail(task, "Testing returned no result. The task was not verified.");
                        }

                        if ("REJECTED".equals(testingResult.getStatus())) {
                                return fail(task,
                                                "Code was applied, but testing could not run: "
                                                                + testingResult.getSummary());
                        }
                        if (task.getProjectAnalysis() == null) {
                                return fail(task,
                                                "Smoke tests did not pass, and no project analysis is available for repair.");
                        }
                        if (task.getRetryCount() < 0 || task.getRetryCount() >= maxRepairAttempts) {
                                return fail(task, "Smoke tests did not pass and the maximum of " + maxRepairAttempts
                                                + " automatic repair attempts has been reached.");
                        }

                        int repairAttempt = task.getRetryCount() + 1;
                        CodingResult repairProposal;
                        try {
                                repairProposal = agentServiceClient.planRepair(
                                                task.getProjectAnalysis(), testingResult, repairAttempt,
                                                task.getProjectPath(), task.getSmokeTests());
                        } catch (Exception exception) {
                                LOG.error("Repair proposal generation failed for task {}. Exception type: {}",
                                                task.getTaskId(), exception.getClass().getSimpleName());
                                return fail(task, "Smoke tests did not pass, and repair proposal generation failed.");
                        }

                        if (!isValidRepairProposal(repairProposal)) {
                                return fail(task,
                                                "Smoke tests did not pass, and the Coding Agent returned an invalid repair proposal.");
                        }
                        if (!hasExpectedProjectPath(repairProposal, task.getProjectPath())) {
                                return fail(task,
                                                "Smoke tests did not pass, and the repair proposal targets files outside the requested project path.");
                        }

                        task.setRetryCount(repairAttempt);
                        task.setCodingResult(repairProposal);
                        if (!task.isAutomaticRepairsAuthorized()) {
                                return fail(task, "Automatic repair is not authorized for this task.");
                        }
                        task.setStatus("TESTING");
                        task.setMessage("Applying authorized repair attempt " + repairAttempt
                                        + " and rerunning tests.");
                        taskStore.save(task);

                        ApplyChangesResult repairApplyResult;
                        try {
                                repairApplyResult = agentServiceClient.applyCode(repairProposal);
                        } catch (Exception exception) {
                                LOG.error("Automatic repair application failed for task {}. Exception type: {}",
                                                task.getTaskId(), exception.getClass().getSimpleName());
                                return fail(task, "Automatic repair could not be applied safely.");
                        }
                        if (!isFullyApplied(repairApplyResult)) {
                                return fail(task,
                                                "Automatic repair was not fully applied. The workspace is not safe to test.");
                        }
                }
        }

        private TaskState fail(TaskState task, String message) {
                task.setStatus("FAILED");
                task.setMessage(message);
                taskStore.save(task);
                return task;
        }

        private boolean hasExpectedProjectPath(CodingResult proposal, String projectPath) {
                if (projectPath == null || projectPath.isBlank() || proposal == null
                                || proposal.getProposedChanges() == null) {
                        return true;
                }

                String expected = projectPath.replace('\\', '/')
                                .replaceAll("^/+|/+$", "");
                return proposal.getProposedChanges().stream()
                                .allMatch(change -> {
                                        if (change == null || change.getFilePath() == null) {
                                                return false;
                                        }
                                        String path = change.getFilePath().replace('\\', '/');
                                        return path.equals(expected) || path.startsWith(expected + "/");
                                });
        }

        private boolean isPassed(TestingResult result) {
                return result != null && "PASSED".equals(result.getStatus()) && !result.isTimedOut();
        }

        private boolean isFullyApplied(ApplyChangesResult result) {
                return result != null && "APPLIED".equals(result.status());
        }

        TaskState getTask(String taskId) {
                return getTask(taskId, null);
        }

        public TaskState getTask(String taskId, String ownerUsername) {
                return taskStore.findById(taskId, ownerUsername)
                                .orElseThrow(() -> new TaskNotFoundException(taskId));
        }

        private boolean isValidDecision(RoutingDecision decision) {
                if (decision == null
                                || decision.getIntent() == null
                                || decision.getAgents() == null
                                || decision.getAgents().isEmpty()
                                || decision.getAgents().contains(null)) {
                        return false;
                }

                List<String> allowedAgents = List.of(
                                "RESEARCH", "CODING", "TESTING", "DOCUMENTATION");

                return decision.getAgents().stream()
                                .allMatch(agent -> allowedAgents.contains(agent.name()));
        }

        private boolean isValidRepairProposal(CodingResult proposal) {
                if (proposal == null
                                || !"PROPOSED".equals(proposal.getStatus())
                                || proposal.getProposedChanges() == null
                                || proposal.getProposedChanges().isEmpty()) {
                        return false;
                }

                for (CodingResult.FileChangeProposal change : proposal.getProposedChanges()) {
                        if (change == null
                                        || change.getFilePath() == null
                                        || change.getFilePath().isBlank()
                                        || change.getContent() == null
                                        || change.getContent().isBlank()
                                        || (!"CREATE".equals(change.getAction())
                                                        && !"MODIFY".equals(change.getAction()))) {
                                return false;
                        }
                }

                return true;
        }

        private String describeInvalidInitialProposal(CodingResult proposal) {
                StringBuilder message = new StringBuilder(
                                "The Coding Agent returned an invalid initial proposal. "
                                                + "No changes are available for approval.");
                if (proposal == null) {
                        return message.append(" The agent returned no proposal result.").toString();
                }

                if (proposal.getStatus() != null && !proposal.getStatus().isBlank()) {
                        message.append(" Agent status: ").append(proposal.getStatus()).append('.');
                }
                if (!isValidRepairProposal(proposal)) {
                        message.append(" Validation details: ")
                                        .append(String.join("; ", proposalValidationErrors(proposal)));
                }
                if (proposal.getSummary() != null && !proposal.getSummary().isBlank()) {
                        message.append(" ").append(proposal.getSummary());
                }
                if (proposal.getOpenQuestions() != null
                                && !proposal.getOpenQuestions().isEmpty()) {
                        message.append(" Open questions: ")
                                        .append(String.join("; ", proposal.getOpenQuestions()));
                }
                return message.toString();
        }

        private List<String> proposalValidationErrors(CodingResult proposal) {
                if (proposal == null) {
                        return List.of("the proposal result is missing");
                }

                List<String> errors = new java.util.ArrayList<>();
                if (!"PROPOSED".equals(proposal.getStatus())) {
                        errors.add("status must be PROPOSED");
                }

                List<CodingResult.FileChangeProposal> changes = proposal.getProposedChanges();
                if (changes == null || changes.isEmpty()) {
                        errors.add("no file changes were returned");
                        return errors;
                }

                for (int index = 0; index < changes.size(); index++) {
                        CodingResult.FileChangeProposal change = changes.get(index);
                        String label = "change " + (index + 1);
                        if (change == null) {
                                errors.add(label + " is null");
                                continue;
                        }
                        if (change.getFilePath() == null || change.getFilePath().isBlank()) {
                                errors.add(label + " has no file path");
                                label += " (unknown path)";
                        } else {
                                label += " (" + change.getFilePath() + ")";
                        }
                        if (change.getContent() == null || change.getContent().isBlank()) {
                                errors.add(label + " has blank content");
                        }
                        if (!"CREATE".equals(change.getAction())
                                        && !"MODIFY".equals(change.getAction())) {
                                errors.add(label + " has an unsupported action");
                        }
                }
                return errors;
        }

}

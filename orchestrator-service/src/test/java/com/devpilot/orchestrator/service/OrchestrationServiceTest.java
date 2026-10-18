package com.devpilot.orchestrator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import com.devpilot.orchestrator.client.AgentServiceClient;
import com.devpilot.orchestrator.model.ApplyChangesResult;
import com.devpilot.orchestrator.model.CodingResult;
import com.devpilot.orchestrator.model.ProjectAnalysis;
import com.devpilot.orchestrator.model.TaskRequest;
import com.devpilot.orchestrator.model.TaskState;
import com.devpilot.orchestrator.model.TestingResult;
import com.devpilot.orchestrator.routing.AiTaskRouter;
import com.devpilot.orchestrator.routing.AgentType;
import com.devpilot.orchestrator.routing.RoutingDecision;
import com.devpilot.orchestrator.routing.TaskIntent;
import com.devpilot.orchestrator.routing.TaskRouter;

@ExtendWith(MockitoExtension.class)
class OrchestrationServiceTest {

    @Mock
    private AgentServiceClient agentClient;

    @Mock
    private AiTaskRouter aiRouter;

    private OrchestrationService service;
    private TaskStore taskStore;
    private TaskRequest request;
    private ProjectAnalysis analysis;

    @BeforeEach
    void setUp() {
        taskStore = new TaskStore();
        service = new OrchestrationService(taskStore, new com.devpilot.orchestrator.routing.TaskRouter(),
                aiRouter, agentClient);
        ReflectionTestUtils.setField(service, "maxRepairAttempts", 2);
        request = new TaskRequest("modify my code to fix the bug");
        request.setProjectPath("demo");
        TaskRequest.SmokeTestCase smokeTest = new TaskRequest.SmokeTestCase();
        smokeTest.setExpectedOutput("ok");
        request.setSmokeTests(List.of(smokeTest));
        analysis = new ProjectAnalysis();
        lenient().when(aiRouter.route(anyString())).thenReturn(testingDecision());
        lenient().when(agentClient.research(
                anyString(), anyBoolean(), any(), any())).thenReturn(analysis);
    }

    @Test
    void initialProposalWaitsForApproval() {
        when(agentClient.planCode(analysis, "demo", request.getSmokeTests()))
                .thenReturn(proposal("demo/initial.java"));

        TaskState task = service.createTask(request);

        assertEquals("AWAITING_APPROVAL", task.getStatus());
        assertFalse(task.isAutomaticRepairsAuthorized());
        verify(agentClient).planCode(analysis, "demo", request.getSmokeTests());
        verify(agentClient, never()).applyCode(any());
        verify(agentClient, never()).testProject(anyString(), anyString(), any());
    }

    @Test
    void researchReceivesTheRequestedProjectRoot() {
        when(agentClient.planCode(analysis, "demo", request.getSmokeTests()))
                .thenReturn(proposal("demo/initial.java"));

        service.createTask(request);

        verify(agentClient).research(
                request.getDescription(), true, "demo", null);
    }

    @Test
    void clarificationProposalIsRetainedAndItsQuestionsAreReturned() {
        CodingResult clarification = new CodingResult();
        clarification.setStatus("NEEDS_CLARIFICATION");
        clarification.setSummary("The implementation details are unclear.");
        clarification.setOpenQuestions(List.of("Which route should the controller expose?"));
        when(agentClient.planCode(analysis, "demo", request.getSmokeTests()))
                .thenReturn(clarification);

        TaskState task = service.createTask(request);

        assertEquals("FAILED", task.getStatus());
        assertEquals(clarification, task.getCodingResult());
        assertTrue(task.getMessage().contains("NEEDS_CLARIFICATION"));
        assertTrue(task.getMessage().contains("Which route should the controller expose?"));
        verify(agentClient, never()).applyCode(any());
    }

    @Test
    void invalidProposalReportsTheFileWithBlankContent() {
        CodingResult invalidProposal = proposal("demo/empty.properties");
        invalidProposal.getProposedChanges().get(0).setContent(" \n");
        when(agentClient.planCode(analysis, "demo", request.getSmokeTests()))
                .thenReturn(invalidProposal);

        TaskState task = service.createTask(request);

        assertEquals("FAILED", task.getStatus());
        assertTrue(task.getMessage().contains("demo/empty.properties"));
        assertTrue(task.getMessage().contains("blank content"));
        verify(agentClient, never()).applyCode(any());
    }

    @Test
    void approvalAppliesAndPassingTestsComplete() {
        when(agentClient.planCode(analysis, "demo", request.getSmokeTests()))
                .thenReturn(proposal("demo/initial.java"));
        when(agentClient.applyCode(any())).thenReturn(applied());
        when(agentClient.testProject(anyString(), anyString(), any())).thenReturn(result("PASSED"));

        TaskState created = service.createTask(request);
        TaskState completed = service.approveAndApply(created.getTaskId());

        assertEquals("COMPLETED", completed.getStatus());
        assertEquals(1, completed.getTestingResult() == null ? 0 : 1);
        verify(agentClient).applyCode(any());
        verify(agentClient).testProject(anyString(), anyString(), any());
    }

    @Test
    void suppliedSmokeTestsRequireTestingEvenWhenRouterDoesNotSelectTesting() {
        when(aiRouter.route(anyString())).thenReturn(new RoutingDecision(
                TaskIntent.CODE_MODIFICATION,
                List.of(AgentType.RESEARCH, AgentType.CODING),
                "coding", true, false, false));
        when(agentClient.planCode(analysis, "demo", request.getSmokeTests()))
                .thenReturn(proposal("demo/initial.java"));
        when(agentClient.applyCode(any())).thenReturn(applied());
        when(agentClient.testProject(anyString(), anyString(), any())).thenReturn(result("PASSED"));

        TaskState created = service.createTask(request);
        TaskState completed = service.approveAndApply(created.getTaskId());

        assertEquals("COMPLETED", completed.getStatus());
        verify(agentClient).testProject(anyString(), anyString(), any());
    }

    @Test
    void projectTestsRunWhenNoConsoleSmokeTestsWereSupplied() {
        request.setSmokeTests(List.of());
        when(agentClient.planCode(analysis, "demo", List.of()))
                .thenReturn(proposal("demo/initial.java"));
        when(agentClient.applyCode(any())).thenReturn(applied());
        when(agentClient.testProject(anyString(), eq("demo"), eq(List.of())))
                .thenReturn(result("PASSED"));

        TaskState created = service.createTask(request);
        TaskState completed = service.approveAndApply(created.getTaskId());

        assertEquals("COMPLETED", completed.getStatus());
        verify(agentClient).testProject(anyString(), eq("demo"), eq(List.of()));
    }

    @Test
    void failedTestAutomaticallyRepairsAndRetestsWithoutApproval() {
        when(agentClient.planCode(analysis, "demo", request.getSmokeTests()))
                .thenReturn(proposal("demo/initial.java"));
        when(agentClient.applyCode(any())).thenReturn(applied());
        when(agentClient.testProject(anyString(), anyString(), any()))
                .thenReturn(result("FAILED"), result("PASSED"));
        when(agentClient.planRepair(eq(analysis), any(TestingResult.class), anyInt(),
                eq("demo"), eq(request.getSmokeTests())))
            .thenReturn(proposal("demo/repair.java"));

        TaskState created = service.createTask(request);
        TaskState completed = service.approveAndApply(created.getTaskId());

        assertEquals("COMPLETED", completed.getStatus());
        assertEquals(1, completed.getRetryCount());
        verify(agentClient, times(2)).applyCode(any());
        verify(agentClient, times(2)).testProject(anyString(), anyString(), any());
        verify(agentClient).planRepair(eq(analysis), any(TestingResult.class),
                eq(1), eq("demo"), eq(request.getSmokeTests()));
    }

    @Test
    void rejectedTestingReportsReasonWithoutRequestingCodeRepair() {
        when(agentClient.planCode(analysis, "demo", request.getSmokeTests()))
                .thenReturn(proposal("demo/initial.java"));
        when(agentClient.applyCode(any())).thenReturn(applied());
        TestingResult rejected = new TestingResult();
        rejected.setStatus("REJECTED");
        rejected.setSummary("Expected src/main/java/Main.java was not found.");
        when(agentClient.testProject(anyString(), anyString(), any()))
                .thenReturn(rejected);

        TaskState created = service.createTask(request);
        TaskState failed = service.approveAndApply(created.getTaskId());

        assertEquals("FAILED", failed.getStatus());
        assertTrue(failed.getMessage().contains(
                "testing could not run: Expected src/main/java/Main.java was not found."));
        verify(agentClient, never()).planRepair(
                any(), any(), anyInt(), anyString(), anyList());
    }

    @Test
    void exhaustedRepairsFailAndDoNotApplyAnotherRepair() {
        when(agentClient.planCode(analysis, "demo", request.getSmokeTests()))
                .thenReturn(proposal("demo/initial.java"));
        when(agentClient.applyCode(any())).thenReturn(applied());
        when(agentClient.testProject(anyString(), anyString(), any()))
                .thenReturn(result("FAILED"), result("FAILED"), result("FAILED"));
        when(agentClient.planRepair(eq(analysis), any(TestingResult.class), anyInt(),
                eq("demo"), eq(request.getSmokeTests())))
            .thenReturn(proposal("demo/repair.java"));

        TaskState created = service.createTask(request);
        TaskState failed = service.approveAndApply(created.getTaskId());

        assertEquals("FAILED", failed.getStatus());
        assertEquals(2, failed.getRetryCount());
        verify(agentClient, times(3)).testProject(anyString(), anyString(), any());
        verify(agentClient, times(2)).planRepair(
                any(), any(), anyInt(), eq("demo"), anyList());
        verify(agentClient, times(3)).applyCode(any());
    }

    @Test
    void nullTestingResultCannotComplete() {
        when(agentClient.planCode(analysis, "demo", request.getSmokeTests()))
                .thenReturn(proposal("demo/initial.java"));
        when(agentClient.applyCode(any())).thenReturn(applied());
        when(agentClient.testProject(anyString(), anyString(), any())).thenReturn(null);

        TaskState created = service.createTask(request);
        TaskState failed = service.approveAndApply(created.getTaskId());

        assertEquals("FAILED", failed.getStatus());
        verify(agentClient, never()).planRepair(
                any(), any(), anyInt(), anyString(), anyList());
    }

    @Test
    void invalidRepairProposalFailsSafely() {
        when(agentClient.planCode(analysis, "demo", request.getSmokeTests()))
                .thenReturn(proposal("demo/initial.java"));
        when(agentClient.applyCode(any())).thenReturn(applied());
        when(agentClient.testProject(anyString(), anyString(), any())).thenReturn(result("FAILED"));
        when(agentClient.planRepair(eq(analysis), any(TestingResult.class), anyInt(),
                eq("demo"), eq(request.getSmokeTests())))
            .thenReturn(new CodingResult());

        TaskState created = service.createTask(request);
        TaskState failed = service.approveAndApply(created.getTaskId());

        assertEquals("FAILED", failed.getStatus());
        verify(agentClient, times(1)).applyCode(any());
    }

    @Test
    void repeatedApprovalIsRejectedAfterFirstApproval() {
        when(agentClient.planCode(analysis, "demo", request.getSmokeTests()))
                .thenReturn(proposal("demo/initial.java"));
        when(agentClient.applyCode(any())).thenReturn(applied());
        when(agentClient.testProject(anyString(), anyString(), any())).thenReturn(result("PASSED"));

        TaskState created = service.createTask(request);
        service.approveAndApply(created.getTaskId());

        assertThrows(IllegalStateException.class, () -> service.approveAndApply(created.getTaskId()));
    }

    @Test
    void applicationExceptionFailsWithoutTesting() {
        when(agentClient.planCode(analysis, "demo", request.getSmokeTests()))
                .thenReturn(proposal("demo/initial.java"));
        when(agentClient.applyCode(any())).thenThrow(
                new IllegalStateException("Unsupported project file type: config.bin"));

        TaskState created = service.createTask(request);
        TaskState failed = service.approveAndApply(created.getTaskId());

        assertEquals("FAILED", failed.getStatus());
        assertTrue(failed.getMessage().contains("Unsupported project file type: config.bin"));
        verify(agentClient, never()).testProject(anyString(), anyString(), any());
    }

    @Test
    void partialApplicationFailsWithoutTestingOrRepair() {
        when(agentClient.planCode(analysis, "demo", request.getSmokeTests()))
                .thenReturn(proposal("demo/initial.java"));
        when(agentClient.applyCode(any())).thenReturn(
                new ApplyChangesResult("PARTIALLY_APPLIED", "partial", List.of("file.java"), List.of("error")));

        TaskState created = service.createTask(request);
        TaskState failed = service.approveAndApply(created.getTaskId());

        assertEquals("FAILED", failed.getStatus());
        verify(agentClient, never()).testProject(anyString(), anyString(), any());
        verify(agentClient, never()).planRepair(
                any(), any(), anyInt(), anyString(), anyList());
    }

    @Test
    void researchOnlyTaskDoesNotInvokeCodingAgent() {
        TaskRequest researchRequest = new TaskRequest("explain this codebase architecture");
        when(aiRouter.route(anyString())).thenReturn(new RoutingDecision(
                TaskIntent.PROJECT_ANALYSIS, List.of(AgentType.RESEARCH), "research", true, false, false));

        TaskState task = service.createTask(researchRequest);

        assertEquals("COMPLETED", task.getStatus());
        verify(agentClient, never()).planCode(any());
        verify(agentClient, never()).applyCode(any());
    }

    @Test
    void fallbackRouterSeparatesProjectChangesFromResearchOnly() {
        TaskRouter router = new TaskRouter();

        RoutingDecision change = router.route("modify my code in this project");
        RoutingDecision analysisOnly = router.route("explain this project codebase");

        assertTrue(change.getAgents().contains(AgentType.CODING));
        assertEquals(TaskIntent.CODE_MODIFICATION, change.getIntent());
        assertTrue(change.isRequiresProjectAccess());
        assertEquals(List.of(AgentType.RESEARCH), analysisOnly.getAgents());
        assertEquals(TaskIntent.PROJECT_ANALYSIS, analysisOnly.getIntent());
    }

    @Test
    void cancelReceivedTaskMarksItCancelledImmediately() {
        TaskState task = new TaskState("desc", "alice");
        task.setProjectPath("demo");
        taskStore.save(task);

        TaskState cancelled = service.cancelTask(task.getTaskId(), "alice");

        assertEquals("CANCELLED", cancelled.getStatus());
        assertTrue(cancelled.isCancelRequested());
        assertTrue(cancelled.isTerminal());
    }

    @Test
    void cancelRejectsAnAlreadyFinishedTask() {
        TaskState task = new TaskState("desc", "alice");
        task.setStatus("COMPLETED");
        taskStore.save(task);

        assertThrows(IllegalStateException.class,
                () -> service.cancelTask(task.getTaskId(), "alice"));
    }

    @Test
    void cancelRequiresTheTaskOwner() {
        TaskState task = new TaskState("desc", "alice");
        taskStore.save(task);

        assertThrows(com.devpilot.orchestrator.exception.TaskNotFoundException.class,
                () -> service.cancelTask(task.getTaskId(), "bob"));
    }

    @Test
    void cancelledWorkflowStopsBeforeCallingAnyAgent() {
        TaskState task = new TaskState("desc", "alice");
        task.setProjectPath("demo");
        task.setSmokeTests(List.of());
        task.setCancelRequested(true);
        taskStore.save(task);

        TaskState result = service.runWorkflow(request, task, "alice");

        assertEquals("CANCELLED", result.getStatus());
        verify(agentClient, never()).research(anyString(), anyBoolean(), any(), any());
        verify(agentClient, never()).planCode(any());
        verify(agentClient, never()).applyCode(any());
    }

    @Test
    void cancellationRequestedWhileRunningStopsAtNextCheckpoint() {
        TaskState task = new TaskState("desc", "alice");
        task.setProjectPath("demo");
        task.setSmokeTests(List.of());
        task.setStatus("RESEARCHING");
        taskStore.save(task);

        TaskState requested = service.cancelTask(task.getTaskId(), "alice");
        assertEquals("RESEARCHING", requested.getStatus());
        assertTrue(requested.isCancelRequested());

        TaskState result = service.runWorkflow(request, task, "alice");
        assertEquals("CANCELLED", result.getStatus());
        verify(agentClient, never()).research(anyString(), anyBoolean(), any(), any());
    }

    private RoutingDecision testingDecision() {
        return new RoutingDecision(TaskIntent.CODE_MODIFICATION,
                List.of(AgentType.RESEARCH, AgentType.CODING, AgentType.TESTING),
                "test", true, true, false);
    }

    private CodingResult proposal(String path) {
        CodingResult proposal = new CodingResult();
        proposal.setStatus("PROPOSED");
        CodingResult.FileChangeProposal change = new CodingResult.FileChangeProposal();
        change.setFilePath(path);
        change.setAction("CREATE");
        change.setContent("class Example {}\n");
        proposal.setProposedChanges(List.of(change));
        return proposal;
    }

    private ApplyChangesResult applied() {
        return new ApplyChangesResult("APPLIED", "ok", List.of("file.java"), List.of());
    }

    private TestingResult result(String status) {
        TestingResult result = new TestingResult();
        result.setStatus(status);
        return result;
    }
}
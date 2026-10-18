
package com.devpilot.orchestrator.client;

import com.devpilot.orchestrator.model.ProjectAnalysis;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.devpilot.orchestrator.model.ApplyChangesResult;
import com.devpilot.orchestrator.model.CodingResult;
import com.devpilot.orchestrator.model.TaskRequest;
import com.devpilot.orchestrator.model.TestingResult;

import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class AgentServiceClient {

        private final DiscoveryClient discoveryClient;

        public AgentServiceClient(DiscoveryClient discoveryClient) {
                this.discoveryClient = discoveryClient;
        }

        public ProjectAnalysis research(
                        String taskDescription,
                        boolean inspectWorkspace) {
                return research(taskDescription, inspectWorkspace, null, null);
        }

        public ProjectAnalysis research(
                        String taskDescription,
                        boolean inspectWorkspace,
                        String relatedTaskContext) {
                return research(
                                taskDescription, inspectWorkspace, null, relatedTaskContext);
        }

        public ProjectAnalysis research(
                        String taskDescription,
                        boolean inspectWorkspace,
                        String projectPath,
                        String relatedTaskContext) {

                List<ServiceInstance> instances = discoveryClient.getInstances("ai-agent-service");

                if (instances == null || instances.isEmpty()) {
                        throw new IllegalStateException(
                                        "No instances of ai-agent-service found in Eureka");
                }

                ServiceInstance instance = instances.get(0);

                URI endpoint = instance.getUri()
                                .resolve("/api/agents/research");

                RestClient restClient = createRestClient();

                ProjectAnalysis response;
                try {
                        response = restClient.post()
                                        .uri(endpoint)
                                        .body(researchRequestBody(
                                                        taskDescription,
                                                        inspectWorkspace,
                                                        projectPath,
                                                        relatedTaskContext))
                                        .retrieve()
                                        .body(ProjectAnalysis.class);
                } catch (HttpClientErrorException exception) {
                        throw new IllegalStateException(
                                        "Research Agent rejected the request: "
                                                        + exception.getResponseBodyAsString(),
                                        exception);
                }

                if (response == null) {
                        throw new IllegalStateException(
                                        "Research Agent returned an empty response");
                }

                return response;
        }

        private Map<String, Object> researchRequestBody(
                        String taskDescription,
                        boolean inspectWorkspace,
                        String projectPath,
                        String relatedTaskContext) {
                Map<String, Object> body = new java.util.HashMap<>();
                body.put("taskDescription", taskDescription);
                body.put("inspectWorkspace", inspectWorkspace);
                if (projectPath != null && !projectPath.isBlank()) {
                        body.put("projectPath", projectPath);
                }
                if (relatedTaskContext != null && !relatedTaskContext.isBlank()) {
                        body.put("existingProjectContext", relatedTaskContext);
                }
                return body;
        }

        public CodingResult planCode(ProjectAnalysis analysis) {
                return planCode(analysis, null, null);
        }

        public CodingResult planCode(ProjectAnalysis analysis, String projectPath) {
                return planCode(analysis, projectPath, null);
        }

        public CodingResult planCode(
                        ProjectAnalysis analysis,
                        String projectPath,
                        List<TaskRequest.SmokeTestCase> smokeTests) {

                if (analysis == null) {
                        throw new IllegalArgumentException(
                                        "ProjectAnalysis must not be null");
                }

                List<ServiceInstance> instances = discoveryClient.getInstances("ai-agent-service");

                if (instances == null || instances.isEmpty()) {
                        throw new IllegalStateException(
                                        "No instances of ai-agent-service found in Eureka");
                }

                ServiceInstance instance = instances.get(0);

                URI endpoint = instance.getUri()
                                .resolve("/api/agents/code/plan");

                RestClient restClient = createRestClient();

                CodingResult response;
                try {
                        response = restClient.post()
                                        .uri(endpoint)
                                        .body(codingRequestBody(
                                                        analysis, projectPath, null, null, smokeTests))
                                        .retrieve()
                                        .body(CodingResult.class);
                } catch (HttpClientErrorException exception) {
                        throw new IllegalStateException(
                                        "Coding Agent rejected the request: "
                                                        + exception.getResponseBodyAsString(),
                                        exception);
                }

                if (response == null) {
                        throw new IllegalStateException(
                                        "Coding Agent returned an empty response");
                }

                return response;
        }

        public CodingResult planRepair(
                        ProjectAnalysis analysis,
                        TestingResult testingResult,
                        int repairAttempt) {
                return planRepair(analysis, testingResult, repairAttempt, null, null);
        }

        public CodingResult planRepair(
                        ProjectAnalysis analysis,
                        TestingResult testingResult,
                        int repairAttempt,
                        String projectPath) {
                return planRepair(
                                analysis, testingResult, repairAttempt, projectPath, null);
        }

        public CodingResult planRepair(
                        ProjectAnalysis analysis,
                        TestingResult testingResult,
                        int repairAttempt,
                        String projectPath,
                        List<TaskRequest.SmokeTestCase> smokeTests) {

                if (analysis == null) {
                        throw new IllegalArgumentException(
                                        "ProjectAnalysis must not be null");
                }

                if (testingResult == null) {
                        throw new IllegalArgumentException(
                                        "TestingResult must not be null");
                }

                List<ServiceInstance> instances = discoveryClient.getInstances("ai-agent-service");

                if (instances == null || instances.isEmpty()) {
                        throw new IllegalStateException(
                                        "No instances of ai-agent-service found in Eureka");
                }

                ServiceInstance instance = instances.get(0);

                URI endpoint = instance.getUri()
                                .resolve("/api/agents/code/plan");

                RestClient restClient = createRestClient();

                CodingResult response = restClient.post()
                                .uri(endpoint)
                                .body(codingRequestBody(
                                                analysis,
                                                projectPath,
                                                testingResult,
                                                repairAttempt,
                                                smokeTests))
                                .retrieve()
                                .body(CodingResult.class);

                if (response == null) {
                        throw new IllegalStateException(
                                        "Coding Agent returned an empty response");
                }

                return response;
        }

        private Map<String, Object> codingRequestBody(
                        ProjectAnalysis analysis,
                        String projectPath,
                        TestingResult repairDiagnostics,
                        Integer repairAttempt,
                        List<TaskRequest.SmokeTestCase> smokeTests) {
                Map<String, Object> body = new java.util.HashMap<>();
                body.put("projectAnalysis", analysis);
                if (projectPath != null && !projectPath.isBlank()) {
                        body.put("projectPath", projectPath);
                }
                if (smokeTests != null && !smokeTests.isEmpty()) {
                        body.put("smokeTests", smokeTests);
                }
                if (repairDiagnostics != null) {
                        body.put("repairDiagnostics", repairDiagnostics);
                        body.put("repairAttempt", repairAttempt);
                }
                return body;
        }

        public ApplyChangesResult applyCode(CodingResult proposal) {

                if (proposal == null) {
                        throw new IllegalArgumentException(
                                        "Coding proposal must not be null");
                }

                List<ServiceInstance> instances = discoveryClient.getInstances("ai-agent-service");

                if (instances == null || instances.isEmpty()) {
                        throw new IllegalStateException(
                                        "No instances of ai-agent-service found in Eureka");
                }

                ServiceInstance instance = instances.get(0);

                URI endpoint = instance.getUri()
                                .resolve("/api/agents/code/apply");

                RestClient restClient = createRestClient();

                ApplyChangesResult response;
                try {
                        response = restClient.post()
                                        .uri(endpoint)
                                        .body(Map.of(
                                                        "proposal", proposal,
                                                        "approved", true))
                                        .retrieve()
                                        .body(ApplyChangesResult.class);
                } catch (HttpClientErrorException exception) {
                        throw new IllegalStateException(
                                        "AI Agent rejected the proposal: "
                                                        + exception.getResponseBodyAsString(),
                                        exception);
                }

                if (response == null) {
                        throw new IllegalStateException(
                                        "Coding Agent returned an empty apply result");
                }

                return response;
        }

        public TestingResult testProject(
                        String taskDescription,
                        String projectPath,
                        List<TaskRequest.SmokeTestCase> smokeTests) {

                if (taskDescription == null || taskDescription.isBlank()) {
                        throw new IllegalArgumentException("Task description must not be blank");
                }

                if (projectPath == null || projectPath.isBlank()) {
                        throw new IllegalArgumentException("Project path must not be blank");
                }

                boolean mavenTest = smokeTests == null || smokeTests.isEmpty();

                List<ServiceInstance> instances = discoveryClient.getInstances("ai-agent-service");

                if (instances == null || instances.isEmpty()) {
                        throw new IllegalStateException(
                                        "No instances of ai-agent-service found in Eureka");
                }

                URI endpoint = instances.get(0).getUri()
                                .resolve("/api/agents/test");

                Map<String, Object> requestBody = new java.util.HashMap<>();
                requestBody.put("taskDescription", taskDescription);
                requestBody.put("projectPath", projectPath);
                requestBody.put("testType", mavenTest ? "MAVEN_TEST" : "JAVA_SMOKE");
                if (!mavenTest) {
                        List<Map<String, Object>> testCases = smokeTests.stream()
                                        .map(test -> Map.<String, Object>of(
                                                        "inputLines", test.getInputLines(),
                                                        "expectedOutput", test.getExpectedOutput()))
                                        .toList();
                        requestBody.put("smokeTests", testCases);
                }

                TestingResult response = createRestClient()
                                .post()
                                .uri(endpoint)
                                .body(requestBody)
                                .retrieve()
                                .body(TestingResult.class);

                if (response == null) {
                        throw new IllegalStateException(
                                        "Testing Agent returned an empty response");
                }

                return response;
        }

        private RestClient createRestClient() {
                return RestClient.builder()
                                .requestInterceptor((request, body, execution) -> {
                                        ServletRequestAttributes attributes =
                                                        (ServletRequestAttributes) RequestContextHolder
                                                                        .getRequestAttributes();
                                        if (attributes != null) {
                                                HttpServletRequest incomingRequest = attributes.getRequest();
                                                String authorization = incomingRequest
                                                                .getHeader(HttpHeaders.AUTHORIZATION);
                                                if (authorization != null
                                                                && authorization.startsWith("Bearer ")) {
                                                        request.getHeaders().set(
                                                                        HttpHeaders.AUTHORIZATION,
                                                                        authorization);
                                                }
                                        }
                                        return execution.execute(request, body);
                                })
                                .build();
        }

}

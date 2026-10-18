package com.devpilot.aiagent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CodingRequest {

    // The Orchestrator can pass the structured research result directly to the
    // Coding Agent. We don't need a second task description or another copy of the
    // requirements.
    @NotNull
    @Valid
    private ProjectAnalysis projectAnalysis;

    private TestingResult repairDiagnostics;

    private Integer repairAttempt;

    private String projectPath;

    @Valid
    private List<TestingRequest.SmokeTestCase> smokeTests = new ArrayList<>();

    public CodingRequest() {
    }

    public CodingRequest(ProjectAnalysis projectAnalysis) {
        this.projectAnalysis = projectAnalysis;
    }

    public ProjectAnalysis getProjectAnalysis() {
        return projectAnalysis;
    }

    public void setProjectAnalysis(ProjectAnalysis projectAnalysis) {
        this.projectAnalysis = projectAnalysis;
    }

    public TestingResult getRepairDiagnostics() {
        return repairDiagnostics;
    }

    public void setRepairDiagnostics(TestingResult repairDiagnostics) {
        this.repairDiagnostics = repairDiagnostics;
    }

    public Integer getRepairAttempt() {
        return repairAttempt;
    }

    public void setRepairAttempt(Integer repairAttempt) {
        this.repairAttempt = repairAttempt;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public List<TestingRequest.SmokeTestCase> getSmokeTests() {
        return smokeTests;
    }

    public void setSmokeTests(List<TestingRequest.SmokeTestCase> smokeTests) {
        this.smokeTests = smokeTests;
    }

}

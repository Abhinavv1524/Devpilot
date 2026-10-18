package com.devpilot.orchestrator.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class TaskRequest {

    @NotBlank(message = "Task description must not be blank")
    @Size(max = 2000, message = "Task description must not exceed 2000 characters")
    private String description;

    private String projectPath;

    @Valid
    private List<SmokeTestCase> smokeTests = new ArrayList<>();

    public TaskRequest() {
    }

    public TaskRequest(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public List<SmokeTestCase> getSmokeTests() {
        return smokeTests;
    }

    public void setSmokeTests(List<SmokeTestCase> smokeTests) {
        this.smokeTests = smokeTests;
    }

    public static class SmokeTestCase {

        @Valid
        private List<String> inputLines = new ArrayList<>();

        @NotBlank(message = "Expected output must not be blank")
        private String expectedOutput;

        public SmokeTestCase() {
        }

        public List<String> getInputLines() {
            return inputLines;
        }

        public void setInputLines(List<String> inputLines) {
            this.inputLines = inputLines;
        }

        public String getExpectedOutput() {
            return expectedOutput;
        }

        public void setExpectedOutput(String expectedOutput) {
            this.expectedOutput = expectedOutput;
        }
    }
}

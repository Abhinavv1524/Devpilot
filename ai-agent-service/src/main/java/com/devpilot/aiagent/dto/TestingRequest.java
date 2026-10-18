package com.devpilot.aiagent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// What these fields mean:
// - taskDescription: what you want to verify.
// - projectPath: a path relative to the configured workspace, such as AddTwoNumbers.
// - testType: selects a supported test procedure rather than providing a shell command.
// - smokeTests: input lines and expected output for supported console smoke checks.
// For JAVA_SMOKE, we'll require at least one smoke test case. For MAVEN_TEST, we'll validate the Maven project and run its predefined test goal. Those checks will be enforced by the agent and execution tool in the next steps.

public record TestingRequest(
        @NotBlank String taskDescription,
        @NotBlank String projectPath,
        @NotNull TestType testType,
        @Valid List<SmokeTestCase> smokeTests) {
    public enum TestType {
        JAVA_SMOKE,
        MAVEN_TEST
    }

    public record SmokeTestCase(
            @NotNull List<String> inputLines,
            @NotEmpty String expectedOutput) {
    }
}
package com.devpilot.aiagent.dto;

import java.util.List;

// A few design decisions:
// -exitCode is nullable because a process might be rejected before starting.-timedOut explicitly distinguishes timeouts from ordinary failures.-errors records validation and execution issues.-PASSED will only be returned when the actual execution and expected-output checks support that conclusion.

public record TestingResult(
        Status status,
        String summary,
        String testType,
        String testCommand,
        Integer exitCode,
        String standardOutput,
        String standardError,
        boolean timedOut,
        List<String> errors) {
    public enum Status {
        PASSED,
        FAILED,
        TIMED_OUT,
        REJECTED
    }
}
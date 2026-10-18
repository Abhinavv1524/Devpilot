package com.devpilot.aiagent.dto;

import java.util.List;

public record ApplyChangesResult(
        String status,
        String message,
        List<String> appliedFiles,
        List<String> errors) {
}
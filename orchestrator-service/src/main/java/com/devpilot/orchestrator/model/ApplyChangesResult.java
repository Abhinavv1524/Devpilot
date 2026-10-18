
package com.devpilot.orchestrator.model;

import java.util.List;

public record ApplyChangesResult(
        String status,
        String message,
        List<String> appliedFiles,
        List<String> errors) {
}

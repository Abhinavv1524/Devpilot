
package com.devpilot.aiagent.dto;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeSearchRequest(
        @NotBlank(message = "Question must not be empty") String question) {
}

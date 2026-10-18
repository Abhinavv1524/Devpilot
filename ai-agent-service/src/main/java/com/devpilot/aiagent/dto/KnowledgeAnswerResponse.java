package com.devpilot.aiagent.dto;

import java.util.List;
import java.util.List;

public record KnowledgeAnswerResponse(
        String answer,
        List<KnowledgeSource> sources) {
}

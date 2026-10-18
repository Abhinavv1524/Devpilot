
package com.devpilot.aiagent.controller;

import java.util.List;
import java.util.Map;

import com.devpilot.aiagent.dto.KnowledgeAnswerResponse;
import com.devpilot.aiagent.dto.KnowledgeSearchRequest;
import com.devpilot.aiagent.knowledge.KnowledgeQuestionAnswerService;
import com.devpilot.aiagent.knowledge.KnowledgeRetrievalService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/knowledge")
@SecurityRequirement(name = "bearerAuth")
public class KnowledgeController {

    private final KnowledgeRetrievalService retrievalService;
    private final KnowledgeQuestionAnswerService questionAnswerService;

    public KnowledgeController(
            KnowledgeRetrievalService retrievalService,
            KnowledgeQuestionAnswerService questionAnswerService) {
        this.retrievalService = retrievalService;
        this.questionAnswerService = questionAnswerService;
    }

    @PostMapping("/ask")
    public KnowledgeAnswerResponse askKnowledge(
            @Valid @RequestBody KnowledgeSearchRequest request) {
        return questionAnswerService.answer(request.question());
    }

    @PostMapping("/search")
    public List<Map<String, Object>> searchKnowledge(
            @Valid @RequestBody KnowledgeSearchRequest request) {
        return retrievalService.search(request.question());
    }
}

package com.devpilot.aiagent.knowledge;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeBaseIngestionRunner implements ApplicationRunner {

    private final KnowledgeBaseIngestionService ingestionService;

    public KnowledgeBaseIngestionRunner(
            KnowledgeBaseIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        ingestionService.ingestDocuments();
    }
}

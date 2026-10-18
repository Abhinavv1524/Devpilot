
package com.devpilot.aiagent.knowledge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeRetrievalService {

    private static final int MAX_RESULTS = 5;
    private static final double MIN_SIMILARITY = 0.65;

    private final VectorStore vectorStore;
    private final KnowledgeBaseIngestionService ingestionService;

    public KnowledgeRetrievalService(
            VectorStore vectorStore,
            KnowledgeBaseIngestionService ingestionService) {
        this.vectorStore = vectorStore;
        this.ingestionService = ingestionService;
    }

    public List<Map<String, Object>> search(String question) {
        List<Document> documents = similaritySearch(question, MIN_SIMILARITY);

        if (documents.isEmpty()) {
            ingestionService.ingestDocuments();
            documents = similaritySearch(question, 0.0);
        }

        List<Map<String, Object>> results = new ArrayList<>();

        for (Document document : documents) {
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> metadata = document.getMetadata();

            result.put("content", document.getText());
            result.put("source", metadata.get("source"));
            result.put("heading", metadata.get("heading"));
            result.put("section", metadata.get("section"));

            results.add(result);
        }

        return results;
    }

    private List<Document> similaritySearch(String question, double similarityThreshold) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(question)
                .topK(MAX_RESULTS);

        if (similarityThreshold > 0.0) {
            builder.similarityThreshold(similarityThreshold);
        }

        return vectorStore.similaritySearch(builder.build());
    }

    public String retrieveContext(String question) {
        List<Map<String, Object>> results = search(question);

        if (results.isEmpty()) {
            return "No relevant reference documents were found.";
        }

        StringBuilder context = new StringBuilder();

        for (Map<String, Object> result : results) {
            context.append("Source: ")
                    .append(result.get("source"))
                    .append("\nHeading: ")
                    .append(result.get("heading"))
                    .append("\nSection: ")
                    .append(result.get("section"))
                    .append("\nContent:\n")
                    .append(result.get("content"))
                    .append("\n\n");
        }

        return context.toString();
    }
}

package com.devpilot.aiagent.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class KnowledgeRetrievalServiceTest {

    @Test
    void searchUsesBoundedResultsAndRelevanceThreshold() {
        VectorStore vectorStore = org.mockito.Mockito.mock(VectorStore.class);
        Document document = Document.builder()
                .text("Use constructor injection.")
                .metadata(Map.of(
                        "source", "java-guidelines.pdf",
                        "heading", "Dependency injection",
                        "section", "Constructors"))
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(document));
        KnowledgeBaseIngestionService ingestionService =
                org.mockito.Mockito.mock(KnowledgeBaseIngestionService.class);
        KnowledgeRetrievalService service =
                new KnowledgeRetrievalService(vectorStore, ingestionService);

        List<Map<String, Object>> results = service.search("How do I inject dependencies?");

        ArgumentCaptor<SearchRequest> requestCaptor =
                ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        assertEquals(5, requestCaptor.getValue().getTopK());
        assertEquals(0.65, requestCaptor.getValue().getSimilarityThreshold());
        assertEquals(1, results.size());
        assertEquals("Use constructor injection.", results.get(0).get("content"));
        assertEquals("java-guidelines.pdf", results.get(0).get("source"));
    }

    @Test
    void searchTriggersIngestionAndRetriesWhenNoResultsAreFound() {
        VectorStore vectorStore = org.mockito.Mockito.mock(VectorStore.class);
        KnowledgeBaseIngestionService ingestionService =
                org.mockito.Mockito.mock(KnowledgeBaseIngestionService.class);
        Document document = Document.builder()
                .text("Use constructor injection.")
                .metadata(Map.of(
                        "source", "java-guidelines.pdf",
                        "heading", "Dependency injection",
                        "section", "Constructors"))
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of())
                .thenReturn(List.of(document));
        KnowledgeRetrievalService service =
                new KnowledgeRetrievalService(vectorStore, ingestionService);

        List<Map<String, Object>> results = service.search("How do I inject dependencies?");

        verify(ingestionService).ingestDocuments();
        assertEquals(1, results.size());
        assertEquals("Use constructor injection.", results.get(0).get("content"));
    }
}

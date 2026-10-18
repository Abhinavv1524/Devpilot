package com.devpilot.aiagent.knowledge;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeBaseIngestionService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseIngestionService.class);

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final SectionChunker sectionChunker;

    public KnowledgeBaseIngestionService(
            VectorStore vectorStore,
            JdbcTemplate jdbcTemplate,
            SectionChunker sectionChunker) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.sectionChunker = sectionChunker;
    }

    @Transactional
    public int ingestDocuments() {
        createManifestTable();

        int totalChunks = 0;

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

            Resource[] pdfFiles = resolver.getResources("classpath*:documents/*.pdf");

            for (Resource pdf : pdfFiles) {
                String source = pdf.getFilename();

                if (source == null) {
                    continue;
                }

                byte[] content;
                try (var input = pdf.getInputStream()) {
                    content = input.readAllBytes();
                }

                String checksum = sha256(content);

                List<String> savedChecksums = jdbcTemplate.query(
                        "SELECT checksum FROM devpilot_knowledge_documents "
                                + "WHERE source = ?",
                        (rs, rowNum) -> rs.getString("checksum"),
                        source);

                if (!savedChecksums.isEmpty()
                        && savedChecksums.get(0).equals(checksum)) {
                    log.info("Skipping unchanged PDF: {}", source);
                    continue;
                }

                // Remove the manifest entry first so a failed re-index
                // can be retried the next time ingestion is triggered.
                jdbcTemplate.update(
                        "DELETE FROM devpilot_knowledge_documents "
                                + "WHERE source = ?",
                        source);

                // Remove previously indexed chunks for this PDF.
                jdbcTemplate.update(
                        "DELETE FROM vector_store "
                                + "WHERE metadata ->> 'source' = ?",
                        source);

                ByteArrayResource pdfResource = new ByteArrayResource(content) {
                    @Override
                    public String getFilename() {
                        return source;
                    }
                };

                List<Document> pages = new PagePdfDocumentReader(pdfResource).get();

                List<Document> chunks = sectionChunker.chunk(pages, source);

                if (chunks.isEmpty()) {
                    log.warn("No text extracted from PDF: {}", source);
                    continue;
                }

                // Spring AI generates embeddings through the configured
                // EmbeddingModel and stores them in pgvector.
                vectorStore.add(chunks);

                jdbcTemplate.update(
                        "INSERT INTO devpilot_knowledge_documents "
                                + "(source, checksum) VALUES (?, ?)",
                        source, checksum);

                totalChunks += chunks.size();

                log.info("Indexed {} chunks from {}",
                        chunks.size(), source);
            }

            return totalChunks;

        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed to read knowledge-base PDFs", ex);
        }
    }

    private void createManifestTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS devpilot_knowledge_documents (
                    source VARCHAR(500) PRIMARY KEY,
                    checksum VARCHAR(64) NOT NULL,
                    indexed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

    private String sha256(byte[] content) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(content);

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable", ex);
        }
    }
}


package com.devpilot.aiagent.knowledge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
public class SectionChunker {

    public List<Document> chunk(List<Document> pages, String source) {
        List<Document> chunks = new ArrayList<>();

        String heading = "General";
        String section = "General";
        StringBuilder content = new StringBuilder();
        int chunkNumber = 0;

        for (Document page : pages) {
            for (String line : page.getText().split("\\R")) {
                String text = normalizeWhitespace(line);

                if (text.isEmpty()) {
                    continue;
                }

                boolean isHeading = text.startsWith("Heading: ");
                boolean isSection = text.startsWith("Section: ");

                if (isHeading || isSection) {
                    // Don't save a chunk containing only heading labels.
                    if (hasBodyContent(content)) {
                        chunks.add(createChunk(
                                content.toString(),
                                source,
                                heading,
                                section,
                                chunkNumber++));
                        content.setLength(0);
                    }

                    if (isHeading) {
                        heading = text.substring("Heading: ".length()).trim();
                        section = heading;
                    } else {
                        section = text.substring("Section: ".length()).trim();
                    }
                }

                if (content.length() > 0) {
                    content.append("\n");
                }
                content.append(text);
            }
        }

        if (hasBodyContent(content)) {
            chunks.add(createChunk(
                    content.toString(),
                    source,
                    heading,
                    section,
                    chunkNumber));
        }

        return chunks;
    }

    private boolean hasBodyContent(StringBuilder content) {
        return content.toString().lines()
                .map(String::trim)
                .anyMatch(line -> !line.isEmpty()
                        && !line.startsWith("Heading: ")
                        && !line.startsWith("Section: "));
    }

    private Document createChunk(
            String text,
            String source,
            String heading,
            String section,
            int chunkNumber) {

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", source);
        metadata.put("heading", heading);
        metadata.put("section", section);
        metadata.put("chunkNumber", chunkNumber);

        return Document.builder()
                .text(text.trim())
                .metadata(metadata)
                .build();
    }

    private String normalizeWhitespace(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
}

package com.devpilot.orchestrator.history;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devpilot.orchestrator.model.CodingResult;
import com.devpilot.orchestrator.model.ProjectAnalysis;
import com.devpilot.orchestrator.model.TaskState;
import com.devpilot.orchestrator.model.TestingResult;

@Service
public class TaskHistoryService {

    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9]+");
    private static final int MAX_MEMORY_RESULTS = 5;

    private final TaskHistoryRepository repository;

    public TaskHistoryService(TaskHistoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void saveTerminalTask(TaskState task) {
        if (task.getOwnerUsername() == null
                || (!"COMPLETED".equals(task.getStatus())
                        && !"FAILED".equals(task.getStatus())
                        && !"CANCELLED".equals(task.getStatus()))) {
            return;
        }

        repository.save(new TaskHistoryEntity(
                task.getTaskId(),
                task.getOwnerUsername(),
                task.getDescription(),
                task.getStatus(),
                researchSummary(task.getProjectAnalysis()),
                codingSummary(task.getCodingResult()),
                testingSummary(task.getTestingResult()),
                parseCreatedAt(task.getCreatedAt()),
                LocalDateTime.now()));
    }

    @Transactional(readOnly = true)
    public List<TaskHistoryResponse> findAll(String ownerUsername) {
        return repository.findTop20ByOwnerUsernameOrderByCreatedAtDesc(ownerUsername).stream()
                .map(TaskHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TaskHistoryResponse> findById(String taskId, String ownerUsername) {
        return repository.findByTaskIdAndOwnerUsername(taskId, ownerUsername)
                .map(TaskHistoryResponse::from);
    }

    @Transactional(readOnly = true)
    public List<TaskHistoryResponse> findRelevant(String ownerUsername, String prompt) {
        List<String> terms = terms(prompt);
        if (terms.isEmpty()) {
            return List.of();
        }

        return terms.stream()
                .flatMap(term -> repository.searchByTerm(ownerUsername, term).stream())
                .distinct()
                .limit(MAX_MEMORY_RESULTS)
                .map(TaskHistoryResponse::from)
                .toList();
    }

    public String formatContext(List<TaskHistoryResponse> tasks) {
        if (tasks.isEmpty()) {
            return "No related previous task history was found.";
        }

        return tasks.stream()
                .map(task -> """
                        Previous Task:
                        Prompt: %s
                        Status: %s
                        Research summary: %s
                        Coding summary: %s
                        Testing summary: %s
                        """.formatted(
                        task.userPrompt(),
                        task.taskStatus(),
                        value(task.researchSummary()),
                        value(task.codingSummary()),
                        value(task.testingSummary())))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private List<String> terms(String prompt) {
        if (prompt == null) {
            return List.of();
        }
        return java.util.Arrays.stream(NON_WORD.split(prompt.toLowerCase(Locale.ROOT)))
                .filter(term -> term.length() >= 3)
                .distinct()
                .toList();
    }

    private String researchSummary(ProjectAnalysis analysis) {
        return analysis == null ? null : analysis.getTask();
    }

    private String codingSummary(CodingResult result) {
        return result == null ? null : result.getSummary();
    }

    private String testingSummary(TestingResult result) {
        return result == null ? null : result.getSummary();
    }

    private LocalDateTime parseCreatedAt(String value) {
        return value == null ? LocalDateTime.now() : LocalDateTime.parse(value);
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "Not available" : value;
    }
}

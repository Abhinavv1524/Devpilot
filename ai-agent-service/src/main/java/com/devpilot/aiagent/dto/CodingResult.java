
package com.devpilot.aiagent.dto;

import java.util.ArrayList;
import java.util.List;

public class CodingResult {

    private String status;
    private String summary;

    private List<FileChangeProposal> proposedChanges = new ArrayList<>();

    private List<String> risks = new ArrayList<>();
    private List<String> openQuestions = new ArrayList<>();

    public CodingResult() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<FileChangeProposal> getProposedChanges() {
        return proposedChanges;
    }

    public void setProposedChanges(
            List<FileChangeProposal> proposedChanges) {
        this.proposedChanges = proposedChanges;
    }

    public List<String> getRisks() {
        return risks;
    }

    public void setRisks(List<String> risks) {
        this.risks = risks;
    }

    public List<String> getOpenQuestions() {
        return openQuestions;
    }

    public void setOpenQuestions(List<String> openQuestions) {
        this.openQuestions = openQuestions;
    }

    public static class FileChangeProposal {

        private String filePath;
        private String action;
        private String description;
        private String content;

        public FileChangeProposal() {
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}

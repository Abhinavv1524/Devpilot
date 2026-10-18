package com.devpilot.orchestrator.model;

import java.util.ArrayList;
import java.util.List;

public class ProjectAnalysis {

    private String task;
    private String projectType;

    private List<String> technologies = new ArrayList<>();
    private List<String> requirements = new ArrayList<>();
    private List<String> filesToModify = new ArrayList<>();
    private List<String> newFiles = new ArrayList<>();
    private List<String> dependencies = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();

    private List<String> assumptions = new ArrayList<>();
    private List<String> openQuestions = new ArrayList<>();
    private List<String> risks = new ArrayList<>();

    private boolean requiresProjectInspection;

    public ProjectAnalysis() {
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public List<String> getTechnologies() {
        return technologies;
    }

    public void setTechnologies(List<String> technologies) {
        this.technologies = technologies;
    }

    public List<String> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<String> requirements) {
        this.requirements = requirements;
    }

    public List<String> getFilesToModify() {
        return filesToModify;
    }

    public void setFilesToModify(List<String> filesToModify) {
        this.filesToModify = filesToModify;
    }

    public List<String> getNewFiles() {
        return newFiles;
    }

    public void setNewFiles(List<String> newFiles) {
        this.newFiles = newFiles;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public List<String> getAssumptions() {
        return assumptions;
    }

    public void setAssumptions(List<String> assumptions) {
        this.assumptions = assumptions;
    }

    public List<String> getOpenQuestions() {
        return openQuestions;
    }

    public void setOpenQuestions(List<String> openQuestions) {
        this.openQuestions = openQuestions;
    }

    public List<String> getRisks() {
        return risks;
    }

    public void setRisks(List<String> risks) {
        this.risks = risks;
    }

    public boolean isRequiresProjectInspection() {
        return requiresProjectInspection;
    }

    public void setRequiresProjectInspection(
            boolean requiresProjectInspection) {
        this.requiresProjectInspection = requiresProjectInspection;
    }
}
package com.devpilot.aiagent.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// ResearchRequest.java—defines what the Orchestrator sends to the Research Agent.

public class ResearchRequest {
    // This is the input contract. The Orchestrator will send the task description
    // and any context it already knows about the project.

    @NotBlank(message = "Task description must not be empty")
    @Size(max = 2000, message = "Task description must not exceed 2000 characters")
    private String taskDescription;

    private String projectType;

    private String projectPath;

    private String existingProjectContext;

    private List<String> desiredTechnologies = new ArrayList<>();

    // one boolean field, inspectWorkspace, so the Orchestrator can explicitly tell
    // the Research Agent whether it should inspect the workspace.
    private boolean inspectWorkspace;

    public ResearchRequest() {
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getExistingProjectContext() {
        return existingProjectContext;
    }

    public void setExistingProjectContext(String existingProjectContext) {
        this.existingProjectContext = existingProjectContext;
    }

    public List<String> getDesiredTechnologies() {
        return desiredTechnologies;
    }

    public void setDesiredTechnologies(List<String> desiredTechnologies) {
        this.desiredTechnologies = desiredTechnologies;
    }

    public boolean isInspectWorkspace() {
        return inspectWorkspace;
    }

    public void setInspectWorkspace(boolean inspectWorkspace) {
        this.inspectWorkspace = inspectWorkspace;
    }
}

/*
 * taskDescription: the feature or application the user wants.
 * projectType: for example, a REST API or web application.
 * existingProjectContext: relevant project details, if available.
 * desiredTechnologies: technologies the user wants to use.
 */

package com.devpilot.orchestrator.routing;

import java.util.List;

public class RoutingDecision {

    private TaskIntent intent;
    private List<AgentType> agents;
    private String reason;
    private boolean requiresProjectAccess;
    private boolean requiresTesting;
    private boolean requiresDocumentation;

    public RoutingDecision() {
    }

    public RoutingDecision(
            TaskIntent intent,
            List<AgentType> agents,
            String reason,
            boolean requiresProjectAccess,
            boolean requiresTesting,
            boolean requiresDocumentation) {
        this.intent = intent;
        this.agents = agents;
        this.reason = reason;
        this.requiresProjectAccess = requiresProjectAccess;
        this.requiresTesting = requiresTesting;
        this.requiresDocumentation = requiresDocumentation;
    }

    public TaskIntent getIntent() {
        return intent;
    }

    public void setIntent(TaskIntent intent) {
        this.intent = intent;
    }

    public List<AgentType> getAgents() {
        return agents;
    }

    public void setAgents(List<AgentType> agents) {
        this.agents = agents;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isRequiresProjectAccess() {
        return requiresProjectAccess;
    }

    public void setRequiresProjectAccess(boolean requiresProjectAccess) {
        this.requiresProjectAccess = requiresProjectAccess;
    }

    public boolean isRequiresTesting() {
        return requiresTesting;
    }

    public void setRequiresTesting(boolean requiresTesting) {
        this.requiresTesting = requiresTesting;
    }

    public boolean isRequiresDocumentation() {
        return requiresDocumentation;
    }

    public void setRequiresDocumentation(boolean requiresDocumentation) {
        this.requiresDocumentation = requiresDocumentation;
    }
}

/*
 * User request - Example routing decision
 * Generate a simple Java method - CODE_GENERATION → CODING
 * Explain a technical concept - KNOWLEDGE_RETRIEVAL → RESEARCH
 * Explain an existing project - PROJECT_ANALYSIS → RESEARCH
 * Fix a bug in a project - DEBUGGING → RESEARCH, CODING, possibly TESTING
 * Implement a complex feature - COMPLEX_IMPLEMENTATION → selected agents based
 * on
 * requirements
 */
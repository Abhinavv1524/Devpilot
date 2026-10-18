package com.devpilot.orchestrator.routing;

public enum AgentType {
    RESEARCH,
    CODING,
    TESTING,
    DOCUMENTATION
}

// Why? TaskIntent describes what the user wants. AgentType describes which
// agent can perform the work.
// For example,a simple Java code-generation request may need only CODING,while
// a complex feature may need RESEARCH,CODING,and TESTING.
package com.devpilot.orchestrator.routing;

public enum TaskIntent {

    GENERAL_QUESTION,
    CODE_GENERATION,
    KNOWLEDGE_RETRIEVAL,
    PROJECT_ANALYSIS,
    CODE_MODIFICATION,
    DEBUGGING,
    COMPLEX_IMPLEMENTATION

    /*
     * Intent Example
     * GENERAL_QUESTION “What is dependency injection?”
     * CODE_GENERATION “Write Java code to add two numbers.”
     * KNOWLEDGE_RETRIEVAL “Explain our Spring Boot coding standards.”
     * PROJECT_ANALYSIS “Explain how this project is structured.”
     * CODE_MODIFICATION “Add a field to this existing class.”
     * DEBUGGING “Find and fix this error.”
     * COMPLEX_IMPLEMENTATION “Add JWT authentication to my Expense API.”
     */
}

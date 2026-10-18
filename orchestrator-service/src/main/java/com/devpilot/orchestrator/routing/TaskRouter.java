package com.devpilot.orchestrator.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class TaskRouter {
    public RoutingDecision route(String description) {
        String task = description.toLowerCase(Locale.ROOT);
        List<AgentType> agents = new ArrayList<>();

        TaskIntent intent;
        String reason;
        boolean requiresProjectAccess = false;
        boolean requiresTesting = false;
        boolean requiresDocumentation = false;

        // 1. Bug fixing and debugging
        if (containsAny(task,
                "debug", "fix bug", "fix the bug", "bug in",
                "error in my code", "exception", "stack trace",
                "not working", "doesn't work", "does not work")) {

            intent = TaskIntent.DEBUGGING;
            agents.add(AgentType.RESEARCH);
            agents.add(AgentType.CODING);
            requiresTesting = true;
            reason = "The request appears to involve diagnosing or fixing a problem.";
        }

        // 2. Complex feature implementation
        else if (containsAny(task,
                "implement a feature", "build an application",
                "develop an application", "end-to-end",
                "complete application", "full application",
                "integrate", "multiple files", "full-stack")) {

            intent = TaskIntent.COMPLEX_IMPLEMENTATION;
            agents.add(AgentType.RESEARCH);
            agents.add(AgentType.CODING);
            agents.add(AgentType.TESTING);
            requiresTesting = true;
            reason = "The request appears to require coordinated implementation and testing.";
        }

        // 3. Requests to modify existing code
        else if (containsAny(task,
                "modify my code", "update my code", "change my code",
                "refactor", "add a feature to my project")) {

            intent = TaskIntent.CODE_MODIFICATION;
            agents.add(AgentType.RESEARCH);
            agents.add(AgentType.CODING);
            requiresProjectAccess = true;
            reason = "The request appears to require changes to existing code.";
        }

        // 4. Requests involving an existing project without a change request
        else if (containsAny(task,
                "in my project", "in this project", "existing project",
                "project files", "my codebase", "codebase")) {

            intent = TaskIntent.PROJECT_ANALYSIS;
            agents.add(AgentType.RESEARCH);
            requiresProjectAccess = true;
            reason = "The request appears to require understanding an existing project.";
        }

        // 5. Technical explanations and knowledge questions
        else if (containsAny(task,
                "explain", "what is", "what are", "how does",
                "how do", "difference between", "best practices",
                "coding standards", "why does")) {

            intent = TaskIntent.KNOWLEDGE_RETRIEVAL;
            agents.add(AgentType.RESEARCH);
            reason = "The request appears to be a technical knowledge question.";
        }

        // 6. Simple code generation
        else if (containsAny(task,
                "write code", "write a method", "write a function",
                "generate code", "create a method", "create a function",
                "java program", "python program", "code to")) {

            intent = TaskIntent.CODE_GENERATION;
            agents.add(AgentType.CODING);
            reason = "The request appears to require code generation.";
        }

        // 7. Default for requests not confidently classified
        else {
            intent = TaskIntent.GENERAL_QUESTION;
            agents.add(AgentType.RESEARCH);
            reason = "The request did not match a specific routing rule.";
        }

        return new RoutingDecision(
                intent,
                agents,
                reason,
                requiresProjectAccess,
                requiresTesting,
                requiresDocumentation);
    }

    private boolean containsAny(String text, String... phrases) {
        for (String phrase : phrases) {
            if (text.contains(phrase)) {
                return true;
            }
        }
        return false;
    }
}

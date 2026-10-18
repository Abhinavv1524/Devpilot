package com.devpilot.orchestrator.routing;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiTaskRouter {

  private final ChatClient chatClient;

  public AiTaskRouter(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public RoutingDecision route(String description) {
    String instructions = """
        You are the task router for DevPilot, a multi-agent
        software engineering assistant.

        Classify the user's request and select the agents needed
        to fulfil the complete request.

        Available intents:
        GENERAL_QUESTION, CODE_GENERATION, KNOWLEDGE_RETRIEVAL,
        PROJECT_ANALYSIS, CODE_MODIFICATION, DEBUGGING,
        COMPLEX_IMPLEMENTATION.

        Available agents:
        RESEARCH, CODING, TESTING, DOCUMENTATION.

        Rules:
        - A request to inspect, explain, or summarize an existing
          project without changing code is PROJECT_ANALYSIS and
          normally requires RESEARCH.
        - If the user asks to inspect a project and prepare a coding
          proposal, select RESEARCH and CODING.
        - If the user asks to modify, improve, refactor, implement,
          or fix existing project code, select RESEARCH and CODING.
        - If code changes are requested and testing is requested or
          clearly necessary, also select TESTING and set
          requiresTesting to true.
        - If TESTING is selected, set requiresTesting to true.
        - If a coding workflow requires project files, set
          requiresProjectAccess to true.
        - Simple standalone code generation usually needs CODING only.
        - Technical knowledge questions need RESEARCH.
        - Debugging that requires a fix needs RESEARCH and CODING.
        - Complex implementation may need RESEARCH, CODING and TESTING.
        - Include DOCUMENTATION only when requested or genuinely useful.
        - Never select TESTING as a substitute for CODING when code
          changes are requested.
        - Do not select CODING for research-only requests.
        - Do not claim that project files were inspected.
        - Return a concise reason for your decision.
        """;

    return chatClient.prompt()
        .system(instructions)
        .user(description)
        .call()
        .entity(RoutingDecision.class);
  }
}
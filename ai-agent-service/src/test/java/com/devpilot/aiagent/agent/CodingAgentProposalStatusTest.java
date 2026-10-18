package com.devpilot.aiagent.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.devpilot.aiagent.dto.CodingResult;

class CodingAgentProposalStatusTest {

    @Test
    void promotesCompleteInitialProposalAndKeepsQuestionsForReview() {
        CodingResult result = proposal("NEEDS_CLARIFICATION");
        result.setOpenQuestions(List.of("Should C++ replace the Java example?"));

        CodingAgent.promoteCompleteInitialProposal(result, false);

        assertEquals("PROPOSED", result.getStatus());
        assertTrue(result.getSummary().contains("Review the listed open questions"));
        assertEquals(
                List.of("Should C++ replace the Java example?"),
                result.getOpenQuestions());
    }

    @Test
    void doesNotPromoteIncompleteProposal() {
        CodingResult result = proposal("NEEDS_CLARIFICATION");
        result.getProposedChanges().get(0).setContent(" ");

        CodingAgent.promoteCompleteInitialProposal(result, false);

        assertEquals("NEEDS_CLARIFICATION", result.getStatus());
    }

    @Test
    void doesNotPromoteRepairClarification() {
        CodingResult result = proposal("NEEDS_CLARIFICATION");

        CodingAgent.promoteCompleteInitialProposal(result, true);

        assertEquals("NEEDS_CLARIFICATION", result.getStatus());
    }

    private CodingResult proposal(String status) {
        CodingResult result = new CodingResult();
        result.setStatus(status);
        CodingResult.FileChangeProposal change =
                new CodingResult.FileChangeProposal();
        change.setFilePath("project/src/main.cpp");
        change.setAction("CREATE");
        change.setContent("int main() { return 0; }");
        result.setProposedChanges(List.of(change));
        return result;
    }
}

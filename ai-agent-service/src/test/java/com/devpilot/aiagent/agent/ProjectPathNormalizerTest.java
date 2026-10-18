package com.devpilot.aiagent.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.devpilot.aiagent.dto.CodingResult;
import com.devpilot.aiagent.dto.ProjectAnalysis;

class ProjectPathNormalizerTest {

    @Test
    void resolvesProjectRelativePathsAgainstRequestedWorkspaceRoot() {
        assertEquals(
                "second phase testing/src/main/java/app/App.java",
                ProjectPathNormalizer.resolveWorkspacePath(
                        "src/main/java/app/App.java",
                        "second phase testing"));
    }

    @Test
    void doesNotPrefixPathsAlreadyRootedAtRequestedProject() {
        assertEquals(
                "second phase testing/src/main/java/app/App.java",
                ProjectPathNormalizer.resolveWorkspacePath(
                        "second phase testing/src/main/java/app/App.java",
                        "second phase testing"));
    }

    @Test
    void rejectsProjectRelativePathsThatEscapeTheRequestedRoot() {
        assertThrows(IllegalArgumentException.class,
                () -> ProjectPathNormalizer.resolveWorkspacePath(
                        "../MovieApi/pom.xml", "second phase testing"));
    }

    @Test
    void rejectsGlobPatternsAsFilePathsOnWindows() {
        assertThrows(IllegalArgumentException.class,
                () -> ProjectPathNormalizer.resolveWorkspacePath(
                        "src/main/java/**/controller/** (patient controller file, exact path unknown)",
                        "second phase testing"));
    }

    @Test
    void normalizesNestedProjectWhenProposalAlsoContainsRootLevelSource() {
        CodingResult result = proposal(
                "testing project\\MovieApi\\pom.xml",
                "testing project/MovieApi/src/main/java/com/example/movieapi/MovieApiApplication.java",
                "testing project/src/main/java/Main.java");

        ProjectPathNormalizer.normalize(
                result, "testing project", new ProjectAnalysis());

        assertEquals(List.of(
                "testing project/pom.xml",
                "testing project/src/main/java/com/example/movieapi/MovieApiApplication.java",
                "testing project/src/main/java/Main.java"),
                result.getProposedChanges().stream()
                        .map(CodingResult.FileChangeProposal::getFilePath)
                        .toList());
    }

    @Test
    void preservesFilesAlreadyAtTheRequestedProjectRoot() {
        CodingResult result = proposal(
                "testing project/pom.xml",
                "testing project/src/main/java/com/example/app/App.java",
                "testing project/src/test/java/com/example/app/AppTest.java");

        ProjectPathNormalizer.normalize(
                result, "testing project", new ProjectAnalysis());

        assertEquals(List.of(
                "testing project/pom.xml",
                "testing project/src/main/java/com/example/app/App.java",
                "testing project/src/test/java/com/example/app/AppTest.java"),
                result.getProposedChanges().stream()
                        .map(CodingResult.FileChangeProposal::getFilePath)
                        .toList());
    }

    private CodingResult proposal(String... paths) {
        CodingResult result = new CodingResult();
        result.setProposedChanges(java.util.Arrays.stream(paths)
                .map(path -> {
                    CodingResult.FileChangeProposal change =
                            new CodingResult.FileChangeProposal();
                    change.setFilePath(path);
                    return change;
                })
                .toList());
        return result;
    }
}

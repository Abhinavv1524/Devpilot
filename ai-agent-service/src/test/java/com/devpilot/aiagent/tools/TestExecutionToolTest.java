package com.devpilot.aiagent.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devpilot.aiagent.dto.TestingRequest;
import com.devpilot.aiagent.dto.TestingResult;

class TestExecutionToolTest {

    @TempDir
    Path workspaceRoot;

    @Test
    void mavenTestRunsProjectTests() throws IOException {
        Path project = Files.createDirectories(workspaceRoot.resolve("maven-project"));
        Files.writeString(project.resolve("pom.xml"), """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>test-project</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <packaging>pom</packaging>
                </project>
                """);

        TestingResult result = new TestExecutionTool(workspaceRoot.toString())
                .execute(new TestingRequest(
                        "Run Maven tests",
                        "maven-project",
                        TestingRequest.TestType.MAVEN_TEST,
                        List.of()));

        assertEquals(TestingResult.Status.PASSED, result.status());
        assertEquals("MAVEN_TEST", result.testType());
        assertEquals(0, result.exitCode());
    }

    @Test
    void mavenTestRejectsProjectWithoutPom() throws IOException {
        Files.createDirectories(workspaceRoot.resolve("not-maven"));

        TestingResult result = new TestExecutionTool(workspaceRoot.toString())
                .execute(new TestingRequest(
                        "Run Maven tests",
                        "not-maven",
                        TestingRequest.TestType.MAVEN_TEST,
                        List.of()));

        assertEquals(TestingResult.Status.REJECTED, result.status());
        assertEquals("Project validation failed: Expected a regular pom.xml at the project root.",
                result.summary());
    }

    @Test
    void javaSmokeStillRequiresAtLeastOneCase() {
        TestingResult result = new TestExecutionTool(workspaceRoot.toString())
                .execute(new TestingRequest(
                        "Run smoke tests",
                        "project",
                        TestingRequest.TestType.JAVA_SMOKE,
                        List.of()));

        assertEquals(TestingResult.Status.REJECTED, result.status());
        assertEquals("At least one smoke test case is required.",
                result.summary());
    }

    @Test
    void smokeTestsUseCppSourceWhenJavaMainIsAbsent() throws IOException {
        Path project = Files.createDirectories(workspaceRoot.resolve("cpp-project"));
        Files.writeString(project.resolve("Calculator.cpp"), """
                #include <iostream>
                int main() {
                    std::cout << "cpp smoke passed";
                    return 0;
                }
                """);

        TestingResult result = new TestExecutionTool(workspaceRoot.toString())
                .execute(new TestingRequest(
                        "Run C++ smoke test",
                        "cpp-project",
                        TestingRequest.TestType.JAVA_SMOKE,
                        List.of(new TestingRequest.SmokeTestCase(
                                List.of(), "cpp smoke passed"))));

        assertEquals("CPP_SMOKE", result.testType());
        assertTrue(result.status() == TestingResult.Status.PASSED
                        || result.status() == TestingResult.Status.FAILED
                        || result.status() == TestingResult.Status.REJECTED,
                result.summary());
        assertFalse(result.summary().startsWith("Project validation failed:"));
    }
}

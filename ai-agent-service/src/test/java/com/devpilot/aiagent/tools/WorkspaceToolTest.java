package com.devpilot.aiagent.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceToolTest {

    @TempDir
    Path workspaceRoot;

    @Test
    void projectContextOnlyIncludesFilesUnderRequestedRoot() throws IOException {
        Path movieSource = workspaceRoot.resolve(
                "MovieApi/src/main/java/com/example/movieapi/MovieController.java");
        Path requestedSource = workspaceRoot.resolve("second phase testing/src/TestController.java");
        Files.createDirectories(movieSource.getParent());
        Files.createDirectories(requestedSource.getParent());
        Files.writeString(movieSource, "package com.example.movieapi;");
        Files.writeString(requestedSource, "package com.example.secondphase;");

        String context = new WorkspaceTool(workspaceRoot.toString())
                .getProjectContext("second phase testing");

        assertTrue(context.contains("src\\TestController.java")
                || context.contains("src/TestController.java"));
        assertFalse(context.contains("MovieApi"));
        assertFalse(context.contains("com.example.movieapi"));
    }

    @Test
    void projectContextTreatsMissingRootAsNewProject() throws IOException {
        String context = new WorkspaceTool(workspaceRoot.toString())
                .getProjectContext("new project");

        assertTrue(context.contains("Treat this as a new project."));
        assertFalse(context.contains("MovieApi"));
    }

    @Test
    void projectContextRejectsPathsOutsideWorkspace() {
        WorkspaceTool workspaceTool = new WorkspaceTool(workspaceRoot.toString());

        assertThrows(IOException.class,
                () -> workspaceTool.getProjectContext("..\\outside"));
    }

    @Test
    void searchReturnsOnlyMatchesFromEligibleFilesUnderRequestedProject()
            throws IOException {
        Path source = workspaceRoot.resolve("project/src/Main.java");
        Path secret = workspaceRoot.resolve("project/src/.env");
        Path unrelated = workspaceRoot.resolve("other/src/Other.java");
        Files.createDirectories(source.getParent());
        Files.createDirectories(unrelated.getParent());
        Files.writeString(source, "class Main {\n  // needle occurs here\n}\n");
        Files.writeString(secret, "needle=secret");
        Files.writeString(unrelated, "// needle outside requested project");

        String results = new WorkspaceTool(workspaceRoot.toString())
                .searchProjectFiles("project", "needle");

        assertTrue(results.contains("src/Main.java:2:"));
        assertFalse(results.contains(".env"));
        assertFalse(results.contains("Other.java"));
    }

    @Test
    void contextAndNaturalLanguageSearchFindDeeplyNestedJavaSources()
            throws IOException {
        for (int index = 0; index < 20; index++) {
            Path unrelated = workspaceRoot.resolve(
                    "project/src/main/java/com/example/a%02d/Config.java"
                            .formatted(index));
            Files.createDirectories(unrelated.getParent());
            Files.writeString(unrelated, "class Config {}");
        }
        Path source = workspaceRoot.resolve(
                "project/src/main/java/com/example/patient/controller/PatientController.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                package com.example.patient.controller;
                class PatientController {
                    // GET endpoint for patient data
                }
                """);
        for (String relativePath : new String[] {
                "src/main/java/com/example/patient/service/PatientService.java",
                "src/main/java/com/example/patient/repository/PatientRepository.java",
                "src/main/java/com/example/patient/domain/Patient.java",
                "src/main/java/com/example/patient/api/dto/PatientResponse.java" }) {
            Path relatedSource = workspaceRoot.resolve("project").resolve(relativePath);
            Files.createDirectories(relatedSource.getParent());
            Files.writeString(relatedSource, "class "
                    + relatedSource.getFileName().toString().replace(".java", "")
                    + " {}");
        }

        WorkspaceTool workspaceTool = new WorkspaceTool(workspaceRoot.toString());
        String context = workspaceTool.getProjectContext(
                "project",
                "add a new endpoint for getting the patient data in the same project");
        String results = workspaceTool.searchProjectFiles(
                "project",
                "add a new endpoint for getting the patient data in the same project");

        assertTrue(context.contains("PatientController.java"));
        assertTrue(context.contains("class PatientController"));
        assertTrue(context.contains("PatientService.java"));
        assertTrue(context.contains("PatientRepository.java"));
        assertTrue(context.contains("Patient.java"));
        assertTrue(context.contains("PatientResponse.java"));
        assertTrue(results.contains("PatientController.java"));
        assertTrue(results.contains("GET endpoint for patient data"));
    }

    @Test
    void searchRejectsPathsOutsideWorkspace() {
        WorkspaceTool workspaceTool = new WorkspaceTool(workspaceRoot.toString());

        assertThrows(IOException.class,
                () -> workspaceTool.searchProjectFiles("..\\outside", "needle"));
    }

    @Test
    void approvedChangesCanCreateYamlConfigurationAndIgnoreFiles()
            throws IOException {
        WorkspaceTool workspaceTool = new WorkspaceTool(workspaceRoot.toString());
        String yamlPath = "foodMenuProject/src/main/resources/application.yml";
        String ignorePath = "foodMenuProject/.gitignore";

        workspaceTool.validateWriteTarget(yamlPath, "CREATE");
        workspaceTool.validateWriteTarget(ignorePath, "CREATE");
        workspaceTool.writeApprovedFile(yamlPath, "CREATE", "spring:\n  application:\n    name: food-menu\n");
        workspaceTool.writeApprovedFile(ignorePath, "CREATE", "target/\n");

        assertTrue(Files.readString(workspaceRoot.resolve(yamlPath))
                .contains("name: food-menu"));
        assertTrue(Files.readString(workspaceRoot.resolve(ignorePath))
                .contains("target/"));
    }

    @Test
    void approvedChangesCanCreateMavenWrapperScripts() throws IOException {
        WorkspaceTool workspaceTool = new WorkspaceTool(workspaceRoot.toString());
        String unixWrapper = "foodMenuProject/mvnw";
        String windowsWrapper = "foodMenuProject/mvnw.cmd";

        workspaceTool.validateWriteTarget(unixWrapper, "CREATE");
        workspaceTool.validateWriteTarget(windowsWrapper, "CREATE");
        workspaceTool.writeApprovedFile(unixWrapper, "CREATE", "#!/bin/sh\n");
        workspaceTool.writeApprovedFile(windowsWrapper, "CREATE", "@ECHO OFF\n");

        assertTrue(Files.exists(workspaceRoot.resolve(unixWrapper)));
        assertTrue(Files.exists(workspaceRoot.resolve(windowsWrapper)));
    }

    @Test
    void approvedChangesStillRejectSensitiveAndUnsupportedFiles() {
        WorkspaceTool workspaceTool = new WorkspaceTool(workspaceRoot.toString());

        assertThrows(IOException.class,
                () -> workspaceTool.validateWriteTarget("project/.env", "CREATE"));
        assertThrows(IOException.class,
                () -> workspaceTool.validateWriteTarget("project/image.png", "CREATE"));
    }

    @Test
    void approvedChangesRejectExcludedDirectoriesAndUnsafePathSegments() {
        WorkspaceTool workspaceTool = new WorkspaceTool(workspaceRoot.toString());

        assertThrows(IOException.class,
                () -> workspaceTool.validateWriteTarget("project/target/Build.java", "CREATE"));
        assertThrows(IOException.class,
                () -> workspaceTool.validateWriteTarget("project/../outside/Main.java", "CREATE"));
        assertThrows(IOException.class,
                () -> workspaceTool.validateWriteTarget("project/CON.txt", "CREATE"));
        assertThrows(IOException.class,
                () -> workspaceTool.validateWriteTarget("project/file.txt:stream", "CREATE"));
    }

    @Test
    void approvedChangesAllowCommonApplicationTextFormats() throws IOException {
        WorkspaceTool workspaceTool = new WorkspaceTool(workspaceRoot.toString());
        for (String path : new String[] {
                "project/src/Main.kt",
                "project/src/App.tsx",
                "project/config/settings.json",
                "project/config/settings.toml",
                "project/config/settings.ini",
                "project/.env.example",
                "project/README.rst",
                "project/schema.proto",
                "project/config/custom-format.devpilot" }) {
            workspaceTool.validateWriteTarget(path, "CREATE");
        }
        assertThrows(IOException.class,
                () -> workspaceTool.validateWriteTarget("project/data.bin", "CREATE"));
    }
}

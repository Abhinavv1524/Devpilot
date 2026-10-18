package com.devpilot.aiagent.tools;

import com.devpilot.aiagent.dto.TestingRequest;
import com.devpilot.aiagent.dto.TestingResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import org.springframework.beans.factory.annotation.Value;

@Component
public class TestExecutionTool {
    private static final Logger log = LoggerFactory.getLogger(TestExecutionTool.class);

    // This class is a placeholder for the Test Execution Tool. It will be
    // responsible for executing tests on the project and reporting the results
    // back to the Orchestrator.
    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration MAVEN_TEST_TIMEOUT = Duration.ofMinutes(3);
    private static final Duration CPP_COMPILATION_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_OUTPUT_BYTES = 64 * 1024;
    private static final int MAX_CPP_SOURCE_FILES = 50;
    private static final int MAX_PROJECT_DEPTH = 12;

    private final Path workspaceRoot;

    public TestExecutionTool(
            @Value("${devpilot.workspace.root}") String workspaceRoot) {
        this.workspaceRoot = Path.of(workspaceRoot)
                .toAbsolutePath()
                .normalize();
    }

    public TestingResult execute(TestingRequest request) {
        if (request == null || request.projectPath() == null
                || request.projectPath().isBlank()
                || request.testType() == null) {
            return rejected("A project path and supported test type are required.");
        }

        List<TestingRequest.SmokeTestCase> smokeTests = request.smokeTests();

        if (request.testType() == TestingRequest.TestType.JAVA_SMOKE
                && (smokeTests == null || smokeTests.isEmpty())) {
            return rejected("At least one smoke test case is required.");
        }

        Path project;
        List<Path> cppSources = List.of();

        try {
            project = resolveProject(request.projectPath());
            if (request.testType() == TestingRequest.TestType.MAVEN_TEST) {
                validateMavenProject(project);
                return runMavenTests(project);
            }
            if (!hasJavaMain(project)) {
                cppSources = findCppSources(project);
                if (!cppSources.isEmpty()) {
                    return runCppSmokeTests(project, cppSources, smokeTests);
                }
            }
            validateProject(project);
        } catch (IOException | RuntimeException ex) {
            return rejected("Project validation failed: " + safeMessage(ex));
        }

        Path source = project.resolve("src")
                .resolve("main")
                .resolve("java")
                .resolve("Main.java")
                .normalize();

        Path classes = project.resolve(".devpilot-test-classes").normalize();

        try {
            Files.createDirectories(classes);

            ProcessResult compilation = runProcess(
                    List.of(
                            javaExecutable("javac").toString(),
                            "-d",
                            classes.toString(),
                            source.toString()),
                    project,
                    "");

            if (compilation.timedOut()) {
                return timedOut("javac", compilation);
            }

            if (compilation.exitCode() != 0) {
                return result(
                        TestingResult.Status.FAILED,
                        "Java compilation failed.",
                        "JAVA_SMOKE",
                        "javac",
                        compilation.exitCode(),
                        compilation.stdout(),
                        compilation.stderr(),
                        false,
                        List.of("Compilation failed."));
            }

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            for (int i = 0; i < smokeTests.size(); i++) {
                TestingRequest.SmokeTestCase test = smokeTests.get(i);

                if (test == null || test.inputLines() == null
                        || test.expectedOutput() == null
                        || test.expectedOutput().isBlank()) {
                    return rejected(
                            "Smoke test cases require inputLines and expectedOutput.");
                }

                String input = String.join(
                        System.lineSeparator(), test.inputLines())
                        + System.lineSeparator();

                ProcessResult execution = runProcess(
                        List.of(
                                javaExecutable("java").toString(),
                                "-cp",
                                classes.toString(),
                                "Main"),
                        project,
                        input);

                stdout.append("--- Test ").append(i + 1).append(" ---\n")
                        .append(execution.stdout()).append('\n');

                stderr.append(execution.stderr());

                if (execution.timedOut()) {
                    return result(
                            TestingResult.Status.TIMED_OUT,
                            "Java smoke test timed out.",
                            "JAVA_SMOKE",
                            "java Main",
                            execution.exitCode(),
                            stdout.toString(),
                            stderr.toString(),
                            true,
                            List.of("Execution exceeded the permitted timeout."));
                }

                if (execution.exitCode() != 0) {
                    return result(
                            TestingResult.Status.FAILED,
                            "Java smoke test process failed.",
                            "JAVA_SMOKE",
                            "java Main",
                            execution.exitCode(),
                            stdout.toString(),
                            stderr.toString(),
                            false,
                            List.of("Test process returned a nonzero exit code."));
                }

                if (!normalizeOutput(execution.stdout())
                        .equals(normalizeOutput(test.expectedOutput()))) {
                    return result(
                            TestingResult.Status.FAILED,
                            "Smoke test output did not match expected output.",
                            "JAVA_SMOKE",
                            "javac + java Main",
                            0,
                            stdout.toString(),
                            stderr.toString(),
                            false,
                            List.of("Output mismatch in smoke test " + (i + 1)));
                }
            }

            return result(
                    TestingResult.Status.PASSED,
                    "Compilation succeeded and all smoke tests passed.",
                    "JAVA_SMOKE",
                    "javac + java Main",
                    0,
                    stdout.toString(),
                    stderr.toString(),
                    false,
                    List.of());

        } catch (IOException ex) {
            return result(
                    TestingResult.Status.FAILED,
                    "Unable to execute the Java smoke test.",
                    "JAVA_SMOKE",
                    "javac + java Main",
                    null,
                    "",
                    safeMessage(ex),
                    false,
                    List.of("Process execution or filesystem operation failed."));
        } finally {
            // Compiled classes are retained temporarily for diagnosis.
            // They are confined to the validated project directory.
        }
    }

    private boolean hasJavaMain(Path project) {
        Path source = project.resolve("src/main/java/Main.java").normalize();
        return source.startsWith(project)
                && !Files.isSymbolicLink(source)
                && Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS);
    }

    private List<Path> findCppSources(Path project) throws IOException {
        if (!Files.isDirectory(project, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(project)) {
            throw new IOException("Project directory does not exist.");
        }

        try (var paths = Files.walk(project, MAX_PROJECT_DEPTH)) {
            List<Path> sources = paths
                    .filter(path -> !Files.isSymbolicLink(path))
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.toAbsolutePath().normalize().startsWith(project))
                    .filter(TestExecutionTool::isCppSource)
                    .filter(path -> !isExcludedCppPath(project, path))
                    .sorted(Comparator.comparing(Path::toString))
                    .limit(MAX_CPP_SOURCE_FILES + 1L)
                    .toList();
            if (sources.size() > MAX_CPP_SOURCE_FILES) {
                throw new IOException(
                        "C++ project exceeds the permitted source-file limit.");
            }
            for (Path source : sources) {
                if (Files.size(source) > MAX_OUTPUT_BYTES) {
                    throw new IOException(
                            "C++ source file exceeds the permitted size: "
                                    + project.relativize(source));
                }
            }
            return sources;
        }
    }

    private static boolean isCppSource(Path path) {
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(".cpp") || name.endsWith(".cc") || name.endsWith(".cxx");
    }

    private static boolean isExcludedCppPath(Path project, Path path) {
        for (Path part : project.relativize(path)) {
            String name = part.toString().toLowerCase(java.util.Locale.ROOT);
            if (List.of(".git", "target", "build", "dist", "node_modules",
                    ".idea", ".gradle").contains(name)) {
                return true;
            }
        }
        return false;
    }

    private TestingResult runCppSmokeTests(
            Path project,
            List<Path> sources,
            List<TestingRequest.SmokeTestCase> smokeTests) {
        Path outputDirectory = null;
        String command = "g++ -std=c++17 <project source files>";
        try {
            outputDirectory = Files.createTempDirectory("devpilot-cpp-test-");
            Path executable = outputDirectory.resolve(
                    isWindows() ? "devpilot-smoke.exe" : "devpilot-smoke");
            List<String> compileCommand = new java.util.ArrayList<>(
                    List.of("g++", "-std=c++17", "-O0", "-o", executable.toString()));
            sources.forEach(source -> compileCommand.add(source.toString()));

            ProcessResult compilation = runProcess(
                    compileCommand, project, "", CPP_COMPILATION_TIMEOUT);
            if (compilation.timedOut()) {
                return result(
                        TestingResult.Status.TIMED_OUT,
                        "C++ compilation timed out.",
                        "CPP_SMOKE",
                        command,
                        compilation.exitCode(),
                        compilation.stdout(),
                        compilation.stderr(),
                        true,
                        List.of("Compilation exceeded the permitted timeout."));
            }
            if (compilation.exitCode() != 0) {
                return result(
                        TestingResult.Status.FAILED,
                        "C++ compilation failed.",
                        "CPP_SMOKE",
                        command,
                        compilation.exitCode(),
                        compilation.stdout(),
                        compilation.stderr(),
                        false,
                        List.of("C++ compiler returned a nonzero exit code."));
            }

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            for (int index = 0; index < smokeTests.size(); index++) {
                TestingRequest.SmokeTestCase test = smokeTests.get(index);
                if (test == null || test.inputLines() == null
                        || test.expectedOutput() == null
                        || test.expectedOutput().isBlank()) {
                    return rejected(
                            "Smoke test cases require inputLines and expectedOutput.");
                }

                String input = String.join(
                        System.lineSeparator(), test.inputLines())
                        + System.lineSeparator();
                ProcessResult execution = runProcess(
                        List.of(executable.toString()), project, input);
                stdout.append("--- Test ").append(index + 1).append(" ---\n")
                        .append(execution.stdout()).append('\n');
                stderr.append(execution.stderr());

                if (execution.timedOut()) {
                    return result(
                            TestingResult.Status.TIMED_OUT,
                            "C++ smoke test timed out.",
                            "CPP_SMOKE",
                            executable.getFileName().toString(),
                            execution.exitCode(),
                            stdout.toString(),
                            stderr.toString(),
                            true,
                            List.of("Execution exceeded the permitted timeout."));
                }
                if (execution.exitCode() != 0) {
                    return result(
                            TestingResult.Status.FAILED,
                            "C++ smoke test process failed.",
                            "CPP_SMOKE",
                            executable.getFileName().toString(),
                            execution.exitCode(),
                            stdout.toString(),
                            stderr.toString(),
                            false,
                            List.of("Test process returned a nonzero exit code."));
                }
                if (!normalizeOutput(execution.stdout())
                        .equals(normalizeOutput(test.expectedOutput()))) {
                    return result(
                            TestingResult.Status.FAILED,
                            "Smoke test output did not match expected output.",
                            "CPP_SMOKE",
                            command,
                            0,
                            stdout.toString(),
                            stderr.toString(),
                            false,
                            List.of("Output mismatch in smoke test " + (index + 1)));
                }
            }

            return result(
                    TestingResult.Status.PASSED,
                    "C++ compilation succeeded and all smoke tests passed.",
                    "CPP_SMOKE",
                    command,
                    0,
                    stdout.toString(),
                    stderr.toString(),
                    false,
                    List.of());
        } catch (IOException ex) {
            return result(
                    TestingResult.Status.REJECTED,
                    "Unable to execute C++ smoke tests. Ensure g++ is installed and available on PATH.",
                    "CPP_SMOKE",
                    command,
                    null,
                    "",
                    safeMessage(ex),
                    false,
                    List.of("C++ process execution or filesystem operation failed."));
        } finally {
            if (outputDirectory != null) {
                try {
                    try (var paths = Files.walk(outputDirectory)) {
                        for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                            Files.deleteIfExists(path);
                        }
                    }
                } catch (IOException ex) {
                    log.warn("Unable to clean up temporary C++ test artifacts at {}.",
                            outputDirectory, ex);
                }
            }
        }
    }

    private void validateMavenProject(Path project) throws IOException {
        if (!Files.isDirectory(project, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(project)) {
            throw new IOException("Project directory does not exist.");
        }

        Path pom = project.resolve("pom.xml");
        if (Files.isSymbolicLink(pom)
                || !Files.isRegularFile(pom, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Expected a regular pom.xml at the project root.");
        }
        if (Files.size(pom) > MAX_OUTPUT_BYTES) {
            throw new IOException("pom.xml exceeds the permitted size.");
        }
    }

    private TestingResult runMavenTests(Path project) {
        String command = isWindows()
                ? "mvn.cmd -B -ntp test"
                : "mvn -B -ntp test";
        try {
            ProcessResult execution = runProcess(
                    mavenCommand(),
                    project,
                    "",
                    MAVEN_TEST_TIMEOUT);

            if (execution.timedOut()) {
                return result(
                        TestingResult.Status.TIMED_OUT,
                        "Maven tests exceeded the permitted timeout.",
                        "MAVEN_TEST",
                        command,
                        execution.exitCode(),
                        execution.stdout(),
                        execution.stderr(),
                        true,
                        List.of("Maven tests exceeded the permitted timeout."));
            }

            if (execution.exitCode() != 0) {
                return result(
                        TestingResult.Status.FAILED,
                        "Maven tests failed.",
                        "MAVEN_TEST",
                        command,
                        execution.exitCode(),
                        execution.stdout(),
                        execution.stderr(),
                        false,
                        List.of("Maven test command returned a nonzero exit code."));
            }

            return result(
                    TestingResult.Status.PASSED,
                    "Maven tests passed.",
                    "MAVEN_TEST",
                    command,
                    execution.exitCode(),
                    execution.stdout(),
                    execution.stderr(),
                    false,
                    List.of());
        } catch (IOException ex) {
            return result(
                    TestingResult.Status.FAILED,
                    "Unable to execute Maven tests.",
                    "MAVEN_TEST",
                    command,
                    null,
                    "",
                    safeMessage(ex),
                    false,
                    List.of("Maven process execution failed."));
        }
    }

    private List<String> mavenCommand() {
        if (!isWindows()) {
            return List.of("mvn", "-B", "-ntp", "test");
        }

        String systemRoot = System.getenv("SystemRoot");
        Path commandProcessor = Path.of(
                systemRoot == null || systemRoot.isBlank()
                        ? "C:\\Windows"
                        : systemRoot,
                "System32",
                "cmd.exe");
        return List.of(
                commandProcessor.toString(),
                "/d",
                "/s",
                "/c",
                "mvn.cmd -B -ntp test");
    }

    private boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase(java.util.Locale.ROOT)
                .contains("win");
    }

    private String normalizeOutput(String output) {
        return output
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }

    private Path resolveProject(String relativePath) throws IOException {
        Path supplied = Path.of(relativePath);

        if (supplied.isAbsolute()) {
            throw new IOException("Absolute project paths are not permitted.");
        }

        if (!Files.isDirectory(workspaceRoot, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(workspaceRoot)) {
            throw new IOException("Configured workspace root is invalid.");
        }

        Path project = workspaceRoot.resolve(supplied).normalize();

        if (!project.startsWith(workspaceRoot)
                || project.equals(workspaceRoot)) {
            throw new IOException("Project path escapes the workspace.");
        }

        Path current = workspaceRoot;

        for (Path part : workspaceRoot.relativize(project)) {
            current = current.resolve(part);

            if (Files.isSymbolicLink(current)) {
                throw new IOException("Symbolic links are not permitted.");
            }
        }

        return project;
    }

    private void validateProject(Path project) throws IOException {
        if (!Files.isDirectory(project, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(project)) {
            throw new IOException("Project directory does not exist.");
        }

        Path source = project.resolve("src/main/java/Main.java").normalize();

        if (!source.startsWith(project)
                || Files.isSymbolicLink(source)
                || !Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "Expected src/main/java/Main.java was not found.");
        }

        Path current = project;

        for (Path part : project.relativize(source)) {
            current = current.resolve(part);

            if (Files.isSymbolicLink(current)) {
                throw new IOException("Symbolic links are not permitted.");
            }
        }

        if (Files.size(source) > MAX_OUTPUT_BYTES) {
            throw new IOException("Source file exceeds the permitted size.");
        }
    }

    private ProcessResult runProcess(
            List<String> command,
            Path directory,
            String stdin) throws IOException {
        return runProcess(command, directory, stdin, PROCESS_TIMEOUT);
    }

    private ProcessResult runProcess(
            List<String> command,
            Path directory,
            String stdin,
            Duration timeout) throws IOException {

        Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .start();

        try {
            // Drain both output streams concurrently to prevent pipe deadlocks.
            OutputCollector stdout = new OutputCollector(process.getInputStream());
            OutputCollector stderr = new OutputCollector(process.getErrorStream());

            Thread stdoutThread = Thread.ofPlatform().daemon()
                    .start(stdout::collect);
            Thread stderrThread = Thread.ofPlatform().daemon()
                    .start(stderr::collect);

            try (var output = process.getOutputStream()) {
                output.write(stdin.getBytes(StandardCharsets.UTF_8));
                output.flush();
            }

            boolean finished = process.waitFor(
                    timeout.toMillis(), TimeUnit.MILLISECONDS);

            if (!finished) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
            }

            stdoutThread.join(2000);
            stderrThread.join(2000);

            return new ProcessResult(
                    finished ? process.exitValue() : null,
                    stdout.text(),
                    stderr.text(),
                    !finished);

        } catch (InterruptedException ex) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Process execution was interrupted.", ex);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private Path javaExecutable(String executable) {
        String extension = System.getProperty("os.name")
                .toLowerCase()
                .contains("win") ? ".exe" : "";

        return Path.of(
                System.getProperty("java.home"),
                "bin",
                executable + extension);
    }

    private TestingResult rejected(String message) {
        return result(
                TestingResult.Status.REJECTED,
                message,
                null,
                null,
                null,
                "",
                "",
                false,
                List.of(message));
    }

    private TestingResult timedOut(
            String command,
            ProcessResult process) {
        return result(
                TestingResult.Status.TIMED_OUT,
                "Compilation timed out.",
                "JAVA_SMOKE",
                command,
                process.exitCode(),
                process.stdout(),
                process.stderr(),
                true,
                List.of("Process exceeded the permitted timeout."));
    }

    private TestingResult result(
            TestingResult.Status status,
            String summary,
            String testType,
            String command,
            Integer exitCode,
            String stdout,
            String stderr,
            boolean timedOut,
            List<String> errors) {
        return new TestingResult(
                status,
                summary,
                testType,
                command,
                exitCode,
                stdout,
                stderr,
                timedOut,
                errors);
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null ? ex.getClass().getSimpleName() : message;
    }

    private record ProcessResult(
            Integer exitCode,
            String stdout,
            String stderr,
            boolean timedOut) {
    }

    private static final class OutputCollector {
        private final InputStream input;
        private final ByteArrayOutputStream captured = new ByteArrayOutputStream();

        private OutputCollector(InputStream input) {
            this.input = input;
        }

        private void collect() {
            byte[] buffer = new byte[4096];

            try (input) {
                int count;

                while ((count = input.read(buffer)) != -1) {
                    int remaining = MAX_OUTPUT_BYTES - captured.size();

                    if (remaining > 0) {
                        captured.write(buffer, 0, Math.min(count, remaining));
                    }
                }
            } catch (IOException ignored) {
                // Process shutdown can close streams while they are being read.
            }
        }

        private String text() {
            return captured.toString(StandardCharsets.UTF_8);
        }

    }
}
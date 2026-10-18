package com.devpilot.aiagent.agent;

import java.util.LinkedHashSet;
import java.util.Set;

import com.devpilot.aiagent.dto.CodingResult;
import com.devpilot.aiagent.dto.ProjectAnalysis;

final class ProjectPathNormalizer {

    private ProjectPathNormalizer() {
    }

    static String resolveWorkspacePath(String candidatePath, String projectPath) {
        if (projectPath == null || projectPath.isBlank()
                || candidatePath == null || candidatePath.isBlank()) {
            return candidatePath;
        }

        java.nio.file.Path root = java.nio.file.Path
                .of(projectPath.replace('\\', '/')).normalize();
        java.nio.file.Path candidate = java.nio.file.Path
                .of(candidatePath.replace('\\', '/')).normalize();
        if (root.isAbsolute() || root.toString().isBlank()
                || root.startsWith("..")
                || candidate.isAbsolute()
                || candidate.startsWith("..")) {
            throw new IllegalArgumentException(
                    "Project file paths must remain within the requested project root.");
        }

        java.nio.file.Path resolved = candidate.startsWith(root)
                ? candidate
                : root.resolve(candidate).normalize();
        if (!resolved.startsWith(root) || resolved.equals(root)) {
            throw new IllegalArgumentException(
                    "Project file paths must remain within the requested project root.");
        }
        return resolved.toString().replace('\\', '/');
    }

    static void normalize(
            CodingResult result,
            String projectPath,
            ProjectAnalysis analysis) {
        if (projectPath == null || projectPath.isBlank()
                || analysis.getFilesToModify() != null
                        && !analysis.getFilesToModify().isEmpty()) {
            return;
        }

        String root = projectPath.replace('\\', '/').replaceAll("^/+|/+$", "");
        String prefix = root + "/";
        var changes = result.getProposedChanges();
        if (changes == null || changes.isEmpty()) {
            return;
        }

        Set<String> wrapperCandidates = new LinkedHashSet<>();
        for (var change : changes) {
            if (change == null || change.getFilePath() == null) {
                return;
            }
            String path = change.getFilePath().replace('\\', '/');
            if (!path.startsWith(prefix)) {
                return;
            }
            String remainder = path.substring(prefix.length());
            int separator = remainder.indexOf('/');
            if (separator <= 0) {
                continue;
            }
            String child = remainder.substring(0, separator);
            if (!isStandardProjectRootEntry(child)) {
                wrapperCandidates.add(child);
            }
        }

        if (wrapperCandidates.size() != 1) {
            return;
        }

        String wrapper = wrapperCandidates.iterator().next();
        String nestedPrefix = prefix + wrapper + "/";
        boolean hasSourceDirectory = false;
        boolean hasBuildFile = false;
        for (var change : changes) {
            String path = change.getFilePath().replace('\\', '/');
            if (!path.startsWith(nestedPrefix)) {
                continue;
            }
            String remainder = path.substring(nestedPrefix.length());
            hasSourceDirectory |= remainder.startsWith("src/");
            hasBuildFile |= isBuildFileAtProjectRoot(remainder);
        }
        if (!hasSourceDirectory || !hasBuildFile) {
            return;
        }

        for (var change : changes) {
            String path = change.getFilePath().replace('\\', '/');
            change.setFilePath(path.startsWith(nestedPrefix)
                    ? root + "/" + path.substring(nestedPrefix.length())
                    : path);
        }
    }

    private static boolean isStandardProjectRootEntry(String entry) {
        return "src".equals(entry)
                || "test".equals(entry)
                || "tests".equals(entry)
                || "docs".equals(entry)
                || "resources".equals(entry)
                || ".github".equals(entry)
                || ".vscode".equals(entry);
    }

    private static boolean isBuildFileAtProjectRoot(String path) {
        return "pom.xml".equals(path)
                || "build.gradle".equals(path)
                || "build.gradle.kts".equals(path)
                || "settings.gradle".equals(path)
                || "settings.gradle.kts".equals(path)
                || "package.json".equals(path)
                || "pyproject.toml".equals(path)
                || "go.mod".equals(path)
                || "Cargo.toml".equals(path);
    }
}

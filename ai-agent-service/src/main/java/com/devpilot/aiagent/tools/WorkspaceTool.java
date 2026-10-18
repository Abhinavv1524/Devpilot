
package com.devpilot.aiagent.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;

@Component
public class WorkspaceTool {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceTool.class);

    private static final int MAX_DEPTH = 12;
    private static final int MAX_FILES = 300;
    private static final int MAX_FILE_SIZE_BYTES = 64 * 1024;
    private static final int MAX_SEARCH_MATCHES = 50;
    private static final int MAX_SEARCH_OUTPUT_CHARS = 32_000;
    private static final int MAX_SEARCH_LINE_CHARS = 500;
    private static final Set<String> SEARCH_STOP_WORDS = Set.of(
            "add", "and", "are", "create", "delete", "for", "get", "getting",
            "implement", "in", "make", "new", "same", "the", "to", "update");

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git",
            "node_modules",
            "target",
            "build",
            "dist",
            ".idea",
            ".gradle",
            ".next",
            "coverage",
            ".venv",
            "venv",
            "__pycache__");

    private final Path workspaceRoot;

    public WorkspaceTool(
            @Value("${devpilot.workspace.root}") String workspaceRoot) {
        this.workspaceRoot = Path.of(workspaceRoot)
                .toAbsolutePath()
                .normalize();
    }

    /**
     * Returns a bounded inventory of files in the dedicated workspace.
     * This method does not read file contents or modify files.
     */
    public String getProjectStructure() throws IOException {

        if (!Files.isDirectory(workspaceRoot)) {
            throw new IllegalStateException(
                    "Configured DevPilot workspace does not exist "
                            + "or is not a directory.");
        }

        StringJoiner files = new StringJoiner("\n");
        int[] count = { 0 };
        boolean[] truncated = { false };

        Files.walkFileTree(
                workspaceRoot,
                java.util.EnumSet.noneOf(
                        java.nio.file.FileVisitOption.class),
                MAX_DEPTH,
                new SimpleFileVisitor<>() {

                    @Override
                    public FileVisitResult preVisitDirectory(
                            Path directory,
                            BasicFileAttributes attributes) {

                        if (directory.equals(workspaceRoot)) {
                            return FileVisitResult.CONTINUE;
                        }

                        Path relative = workspaceRoot.relativize(directory);

                        String directoryName = directory.getFileName().toString()
                                .toLowerCase(Locale.ROOT);

                        if (Files.isSymbolicLink(directory)
                                || IGNORED_DIRECTORIES.contains(
                                        directoryName)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        if (relative.getNameCount() > MAX_DEPTH) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attributes) {

                        if (Files.isSymbolicLink(file)
                                || !attributes.isRegularFile()
                                || isSensitiveFile(file)) {
                            return FileVisitResult.CONTINUE;
                        }

                        Path relative = workspaceRoot.relativize(file);

                        files.add(relative.toString().replace("\\", "/"));
                        count[0]++;

                        if (count[0] >= MAX_FILES) {
                            truncated[0] = true;
                            return FileVisitResult.TERMINATE;
                        }

                        return FileVisitResult.CONTINUE;
                    }
                });

        String result = files.toString();

        if (result.isBlank()) {
            result = "(No files found in the permitted workspace.)";
        }

        if (truncated[0]) {
            result += "\n[Output capped at "
                    + MAX_FILES + " files.]";
        }

        return result;
    }

    public String getProjectContext() throws java.io.IOException {
        return getProjectContext(null);
    }

    public String getProjectContext(String relativeProjectPath) throws java.io.IOException {
        return getProjectContext(relativeProjectPath, null);
    }

    public String getProjectContext(
            String relativeProjectPath,
            String query) throws java.io.IOException {
        Path projectRoot = resolveProjectRoot(relativeProjectPath);
        if (!Files.exists(projectRoot, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            return "No existing project files found under requested project root: "
                    + workspaceRoot.relativize(projectRoot)
                    + ". Treat this as a new project.";
        }
        if (!Files.isDirectory(projectRoot, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            throw new java.io.IOException("Workspace root does not exist.");
        }

        java.util.List<java.nio.file.Path> files;

        try (var paths = java.nio.file.Files.walk(projectRoot, MAX_DEPTH)) {
            files = paths
                    .filter(path -> !java.nio.file.Files.isSymbolicLink(path))
                    .filter(java.nio.file.Files::isRegularFile)
                    .filter(path -> path.toAbsolutePath().normalize()
                            .startsWith(projectRoot))
                    .filter(path -> !isExcludedPath(path))
                    .filter(path -> !isSensitiveFile(path))
                    .filter(WorkspaceTool::isReadableSourceFile)
                    .sorted(java.util.Comparator
                            .comparingInt((Path path) -> contextPathScore(path, query))
                            .reversed()
                            .thenComparing(Path::toString))
                    .limit(15)
                    .toList();
        }

        StringBuilder context = new StringBuilder();
        context.append("Observed source files:\n");

        for (var file : files) {
            context.append("- ")
                    .append(projectRoot.relativize(file))
                    .append('\n');
        }

        context.append("\nFile contents:\n");
        int remaining = 30_000;

        for (var file : files) {
            if (remaining <= 0) {
                context.append("\nOverall content limit reached.\n");
                break;
            }

            String content = java.nio.file.Files.readString(file);

            int limit = Math.min(6_000, remaining);

            context.append("\n--- ")
                    .append(projectRoot.relativize(file))
                    .append(" ---\n");

            int length = Math.min(content.length(), limit);
            context.append(content, 0, length).append('\n');

            remaining -= length;

            if (content.length() > length) {
                context.append("[File content truncated]\n");
            }
        }

        if (files.isEmpty()) {
            context.append("No eligible source files found.\n");
        }

        return context.toString();
    }

    private static int contextPathScore(Path path, String query) {
        if (query == null || query.isBlank()) {
            return 0;
        }

        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        String relativePath = path.toString().toLowerCase(Locale.ROOT);
        return searchTerms(query).stream()
                .mapToInt(term -> (fileName.contains(term) ? 4 : 0)
                        + (relativePath.contains(term) ? 2 : 0))
                .sum();
    }

    public String searchProjectFiles(String relativeProjectPath, String query)
            throws IOException {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("A non-blank search query is required.");
        }
        if (query.length() > 200) {
            throw new IllegalArgumentException("Search query exceeds 200 characters.");
        }

        Path projectRoot = resolveProjectRoot(relativeProjectPath);
        if (!Files.exists(projectRoot, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            return "(No matching source lines found.)";
        }
        if (!Files.isDirectory(projectRoot, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Requested project root is not a directory.");
        }

        java.util.List<Path> files;
        try (var paths = Files.walk(projectRoot, MAX_DEPTH)) {
            files = paths
                    .filter(path -> !Files.isSymbolicLink(path))
                    .filter(path -> Files.isRegularFile(
                            path, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.toAbsolutePath().normalize().startsWith(projectRoot))
                    .filter(path -> !isExcludedPath(path))
                    .filter(path -> !isSensitiveFile(path))
                    .filter(WorkspaceTool::isReadableSourceFile)
                    .sorted()
                    .limit(MAX_FILES)
                    .toList();
        }

        java.util.List<String> searchTerms = searchTerms(query);
        StringBuilder matches = new StringBuilder();
        int matchCount = 0;
        boolean truncated = false;

        for (Path file : files) {
            if (isExcludedPath(file) || isSensitiveFile(file)
                    || !isReadableSourceFile(file)
                    || Files.size(file) > MAX_FILE_SIZE_BYTES) {
                continue;
            }

            String relativePath = workspaceRoot.relativize(file).toString();
            String content = readSourceFile(relativePath);
            String[] lines = content.split("\\R", -1);

            for (int index = 0; index < lines.length; index++) {
                String line = lines[index];
                String normalizedLine = line.toLowerCase(Locale.ROOT);
                if (searchTerms.stream().noneMatch(normalizedLine::contains)) {
                    continue;
                }

                String displayedLine = line.length() > MAX_SEARCH_LINE_CHARS
                        ? line.substring(0, MAX_SEARCH_LINE_CHARS) + " [line truncated]"
                        : line;
                String match = "%s:%d: %s%n".formatted(
                        projectRoot.relativize(file).toString().replace("\\", "/"),
                        index + 1,
                        displayedLine);

                if (matchCount >= MAX_SEARCH_MATCHES
                        || matches.length() + match.length() > MAX_SEARCH_OUTPUT_CHARS) {
                    truncated = true;
                    break;
                }

                matches.append(match);
                matchCount++;
            }
            if (truncated) {
                break;
            }
        }

        if (matches.isEmpty()) {
            return "(No matching source lines found.)";
        }
        if (truncated) {
            matches.append("[Search output truncated.]");
        }
        return matches.toString();
    }

    private static java.util.List<String> searchTerms(String query) {
        java.util.List<String> terms = java.util.Arrays.stream(
                        query.toLowerCase(Locale.ROOT).split("[^\\p{Alnum}_]+"))
                .filter(term -> term.length() >= 3)
                .filter(term -> !SEARCH_STOP_WORDS.contains(term))
                .distinct()
                .limit(8)
                .toList();
        return terms.isEmpty() ? java.util.List.of(query.toLowerCase(Locale.ROOT)) : terms;
    }

    private Path resolveProjectRoot(String relativeProjectPath) throws IOException {
        if (relativeProjectPath == null || relativeProjectPath.isBlank()) {
            return workspaceRoot;
        }

        Path supplied = Path.of(relativeProjectPath);
        if (supplied.isAbsolute()) {
            throw new IOException("Absolute project paths are not permitted.");
        }

        Path projectRoot = workspaceRoot.resolve(supplied).normalize();
        if (!projectRoot.startsWith(workspaceRoot)) {
            throw new IOException("Project path escapes the permitted workspace.");
        }

        Path current = workspaceRoot;
        for (Path part : workspaceRoot.relativize(projectRoot)) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current)) {
                throw new IOException("Symbolic links are not permitted in project paths.");
            }
        }

        return projectRoot;
    }

    /**
     * Reads one eligible source file from the controlled workspace.
     */
    public String readSourceFile(String relativePath) throws IOException {

        Path file = resolveSafePath(relativePath);

        if (!Files.exists(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(file)
                || !Files.isRegularFile(file,
                        java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Requested source file is not a regular file.");
        }

        if (isSensitiveFile(file) || isExcludedPath(file)
                || !isReadableSourceFile(file)) {
            throw new IOException("Requested file is not eligible for reading.");
        }

        if (Files.size(file) > MAX_FILE_SIZE_BYTES) {
            throw new IOException("File exceeds the permitted size.");
        }

        return Files.readString(file);
    }

    public boolean isExistingRegularFile(String relativePath) throws IOException {
        Path file = resolveSafePath(relativePath);
        return Files.isRegularFile(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(file);
    }

    /**
     * Resolves a relative path and prevents access outside the workspace.
     */
    private Path resolveSafePath(String relativePath) throws IOException {

        if (relativePath == null || relativePath.isBlank()) {
            throw new IOException("A relative file path is required.");
        }

        String portablePath = relativePath.replace('\\', '/');
        if (portablePath.startsWith("/")
                || portablePath.indexOf(':') >= 0
                || portablePath.indexOf('\0') >= 0
                || portablePath.matches(".*[<>|?*\"].*")) {
            throw new IOException("Invalid characters or absolute path in target.");
        }
        for (String segment : portablePath.split("/", -1)) {
            if (segment.isBlank()
                    || segment.equals(".")
                    || segment.equals("..")
                    || segment.endsWith(".")
                    || segment.endsWith(" ")
                    || isReservedWindowsName(segment)) {
                throw new IOException("Invalid path segment in target: " + segment);
            }
        }

        Path supplied;
        try {
            supplied = Path.of(relativePath);
        } catch (java.nio.file.InvalidPathException exception) {
            throw new IOException("Invalid target path.", exception);
        }

        if (supplied.isAbsolute()) {
            throw new IOException("Absolute paths are not permitted.");
        }

        Path resolved = workspaceRoot.resolve(supplied).normalize();

        if (!resolved.startsWith(workspaceRoot)
                || resolved.equals(workspaceRoot)) {
            throw new IOException("Path escapes the permitted workspace.");
        }

        Path current = workspaceRoot;

        for (Path part : workspaceRoot.relativize(resolved)) {
            current = current.resolve(part);

            if (Files.isSymbolicLink(current)) {
                throw new IOException("Symbolic links are not permitted.");
            }
        }

        return resolved;
    }

    public void writeApprovedFile(
            String relativePath,
            String action,
            String content) throws IOException {

        if (!"CREATE".equals(action) && !"MODIFY".equals(action)) {
            throw new IOException("Only CREATE and MODIFY are permitted.");
        }

        Path file = resolveSafePath(relativePath);

        validateWritableTarget(file);

        Path parent = file.getParent();

        if (parent == null || !parent.startsWith(workspaceRoot)) {
            throw new IOException("Invalid target directory.");
        }

        // Check existing parent components before creating anything.
        Path current = workspaceRoot;

        for (Path part : workspaceRoot.relativize(parent)) {
            current = current.resolve(part);

            if (Files.isSymbolicLink(current)) {
                throw new IOException("Symbolic links are not permitted.");
            }

            if (Files.exists(current, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                    && !Files.isDirectory(
                            current, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException(
                        "A target directory component is not a directory.");
            }
        }

        boolean exists = Files.exists(
                file, java.nio.file.LinkOption.NOFOLLOW_LINKS);

        if (exists && (Files.isSymbolicLink(file)
                || !Files.isRegularFile(
                        file, java.nio.file.LinkOption.NOFOLLOW_LINKS))) {
            throw new IOException("Target must be a regular file.");
        }

        if ("CREATE".equals(action) && exists) {
            throw new IOException(
                    "CREATE rejected because the target already exists.");
        }

        if ("MODIFY".equals(action)) {
            if (!Files.isDirectory(parent, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException(
                        "MODIFY requires an existing target directory.");
            }

            if (!exists) {
                throw new IOException("MODIFY requires an existing file.");
            }

            Files.writeString(
                    file,
                    content,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    java.nio.file.StandardOpenOption.WRITE);

            return;
        }

        // CREATE: create missing parent directories, then create the new file.
        Files.createDirectories(parent);

        // Re-check the path after directory creation.
        current = workspaceRoot;

        for (Path part : workspaceRoot.relativize(parent)) {
            current = current.resolve(part);

            if (Files.isSymbolicLink(current)
                    || !Files.isDirectory(
                            current, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException(
                        "Target directory is invalid or contains a symbolic link.");
            }
        }

        if (Files.exists(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "CREATE rejected because the target already exists.");
        }

        Files.writeString(
                file,
                content,
                java.nio.file.StandardOpenOption.CREATE_NEW,
                java.nio.file.StandardOpenOption.WRITE);
    }

    public void validateWriteTarget(
            String relativePath,
            String action) throws IOException {

        if (!"CREATE".equals(action) && !"MODIFY".equals(action)) {
            throw new IOException("Only CREATE and MODIFY are permitted.");
        }

        Path file = resolveSafePath(relativePath);
        validateWritableTarget(file);

        Path parent = file.getParent();

        if (parent == null || !parent.startsWith(workspaceRoot)) {
            throw new IOException("Invalid target directory.");
        }

        // Every existing directory between the workspace root and
        // the target's parent must be a real directory, not a symlink.
        Path current = workspaceRoot;

        for (Path part : workspaceRoot.relativize(parent)) {
            current = current.resolve(part);

            if (Files.isSymbolicLink(current)) {
                throw new IOException("Symbolic links are not permitted.");
            }

            if (Files.exists(current, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                    && !Files.isDirectory(
                            current, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException(
                        "A target directory component is not a directory.");
            }
        }

        boolean exists = Files.exists(
                file, java.nio.file.LinkOption.NOFOLLOW_LINKS);

        if (exists && (Files.isSymbolicLink(file)
                || !Files.isRegularFile(
                        file, java.nio.file.LinkOption.NOFOLLOW_LINKS))) {
            throw new IOException("Target must be a regular file.");
        }

        if ("CREATE".equals(action) && exists) {
            throw new IOException(
                    "CREATE rejected because the target already exists.");
        }

        if ("MODIFY".equals(action)) {
            if (!Files.isDirectory(parent, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException(
                        "MODIFY requires an existing target directory.");
            }

            if (!exists) {
                throw new IOException("MODIFY requires an existing file.");
            }
        }
    }

    private void validateWritableTarget(Path file) throws IOException {
        if (isExcludedPath(file)) {
            throw new IOException("Target is inside an excluded directory.");
        }
        if (isSensitiveFile(file)) {
            throw new IOException("Target is a sensitive file.");
        }
        if (!isWritableProjectFile(file)) {
            throw new IOException(
                    "Unsupported project file type (binary files are not allowed): "
                            + file.getFileName());
        }
    }

    private static boolean isReadableSourceFile(
            java.nio.file.Path path) {

        String name = path.getFileName()
                .toString()
                .toLowerCase(java.util.Locale.ROOT);

        return name.endsWith(".java")
                || name.endsWith(".md")
                || name.endsWith(".xml")
                || name.endsWith(".properties")
                || name.endsWith(".gradle")
                || name.endsWith(".sql")
                || name.endsWith(".py")
                || name.endsWith(".js")
                || name.endsWith(".ts")
                || name.endsWith(".html")
                || name.endsWith(".css")
                || name.endsWith(".kt")
                || name.endsWith(".kts")
                || name.endsWith(".go")
                || name.endsWith(".rs")
                || name.endsWith(".c")
                || name.endsWith(".h")
                || name.endsWith(".cc")
                || name.endsWith(".cpp")
                || name.endsWith(".hpp")
                || name.endsWith(".cs")
                || name.endsWith(".rb")
                || name.endsWith(".php")
                || name.endsWith(".swift")
                || name.endsWith(".dart")
                || name.endsWith(".vue")
                || name.endsWith(".jsx")
                || name.endsWith(".tsx")
                || name.endsWith(".scss")
                || name.endsWith(".sass")
                || name.endsWith(".less")
                || name.endsWith(".graphql")
                || name.endsWith(".proto")
                || name.endsWith(".tf")
                || name.endsWith(".hcl")
                || name.endsWith(".txt")
                || name.endsWith(".rst")
                || name.endsWith(".adoc")
                || name.endsWith(".lock");
    }

    private static boolean isWritableProjectFile(Path path) {
        String name = path.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT);

        if (Set.of(
                        ".gitignore",
                        ".dockerignore",
                        ".editorconfig",
                        ".gitattributes",
                        ".env.example",
                        ".env.sample",
                        ".env.template",
                        "dockerfile",
                        "makefile",
                        "mvnw",
                        "gradlew")
                .contains(name)) {
            return true;
        }
        if (Set.of(".gitmodules", ".gitconfig").contains(name)) {
            return false;
        }

        int extensionSeparator = name.lastIndexOf('.');
        String extension = extensionSeparator < 0
                ? ""
                : name.substring(extensionSeparator + 1);
        return !Set.of(
                "7z", "a", "avi", "bin", "bmp", "class", "dll",
                "doc", "docx", "dylib", "ear", "eot", "exe", "flac",
                "gif", "gz", "ico", "jar", "jpeg", "jpg", "m4a", "m4v",
                "mov", "mp3", "mp4", "mpeg", "mpg", "o", "obj", "otf",
                "pdf", "png", "ppt", "pptx", "psd", "pyc", "pyd", "pyo",
                "rar", "so", "sqlite", "sqlite3", "tar", "tif", "tiff",
                "ttf", "wav", "webm", "webp", "woff", "woff2", "xls",
                "xlsx", "zip")
                .contains(extension);
    }

    private boolean isExcludedPath(java.nio.file.Path path) {
        java.util.Set<String> excluded = java.util.Set.of(
                ".git", "node_modules", "target", "build",
                "dist", ".idea", ".gradle", ".next",
                "coverage", ".venv", "venv", "__pycache__");

        for (var part : workspaceRoot.relativize(path)) {
            if (excluded.contains(part.toString().toLowerCase(
                    java.util.Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }

    private boolean isSensitiveFile(Path file) {
        String name = file.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT);

        boolean safeEnvironmentTemplate = Set.of(
                ".env.example",
                ".env.sample",
                ".env.template").contains(name);

        return name.equals(".env")
                || name.startsWith(".env.") && !safeEnvironmentTemplate
                || name.contains("secret")
                || name.contains("credential")
                || name.endsWith(".pem")
                || name.endsWith(".key")
                || name.endsWith(".p12")
                || name.endsWith(".jks")
                || name.endsWith(".keystore")
                || name.equals("id_rsa")
                || name.equals(".npmrc")
                || name.equals(".pypirc");
    }

    private static boolean isReservedWindowsName(String segment) {
        String name = segment.toLowerCase(Locale.ROOT);
        int extensionSeparator = name.indexOf('.');
        if (extensionSeparator >= 0) {
            name = name.substring(0, extensionSeparator);
        }
        return Set.of(
                "con", "prn", "aux", "nul",
                "com1", "com2", "com3", "com4", "com5",
                "com6", "com7", "com8", "com9",
                "lpt1", "lpt2", "lpt3", "lpt4", "lpt5",
                "lpt6", "lpt7", "lpt8", "lpt9").contains(name);
    }
}

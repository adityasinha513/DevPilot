package devPilot.backend.indexing;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class RepositoryPathFilter {

    private static final Set<String> IGNORED_DIR_NAMES = Set.of(
            ".git",
            "node_modules",
            "dist",
            "build",
            "target",
            "out",
            ".next",
            ".nuxt",
            ".output",
            "coverage",
            "__pycache__",
            ".pytest_cache",
            ".mypy_cache",
            ".gradle",
            ".idea",
            ".vscode",
            "vendor",
            "bin",
            "obj",
            ".terraform",
            "venv",
            ".venv",
            "site-packages"
    );

    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "ico", "bmp", "svg",
            "pdf", "zip", "gz", "tar", "rar", "7z", "jar", "war", "ear",
            "exe", "dll", "so", "dylib", "class", "o", "a", "lib",
            "woff", "woff2", "ttf", "eot", "otf",
            "mp3", "mp4", "avi", "mov", "wav", "flac",
            "sqlite", "db", "bin", "dat", "lock"
    );

    public boolean shouldSkipPath(String path) {
        if (path == null || path.isBlank()) {
            return true;
        }
        String normalized = path.replace('\\', '/');
        String[] segments = normalized.split("/");
        for (String segment : segments) {
            if (segment.isBlank()) {
                continue;
            }
            String lower = segment.toLowerCase(Locale.ROOT);
            if (IGNORED_DIR_NAMES.contains(lower)) {
                return true;
            }
            if (lower.startsWith(".") && !isAllowedDotFile(lower)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBinaryExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return false;
        }
        return BINARY_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
    }

    private boolean isAllowedDotFile(String segment) {
        return segment.equals(".env.example")
                || segment.equals(".gitignore")
                || segment.equals(".gitattributes")
                || segment.equals(".editorconfig")
                || segment.equals(".dockerignore");
    }
}

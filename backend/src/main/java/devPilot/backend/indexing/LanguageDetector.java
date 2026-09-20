package devPilot.backend.indexing;

import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class LanguageDetector {

    private static final Map<String, String> EXTENSION_TO_LANGUAGE = Map.ofEntries(
            Map.entry("java", "java"),
            Map.entry("kt", "kotlin"),
            Map.entry("kts", "kotlin"),
            Map.entry("js", "javascript"),
            Map.entry("jsx", "javascript"),
            Map.entry("ts", "typescript"),
            Map.entry("tsx", "typescript"),
            Map.entry("py", "python"),
            Map.entry("go", "go"),
            Map.entry("rs", "rust"),
            Map.entry("rb", "ruby"),
            Map.entry("php", "php"),
            Map.entry("cs", "csharp"),
            Map.entry("cpp", "cpp"),
            Map.entry("cc", "cpp"),
            Map.entry("c", "c"),
            Map.entry("h", "c"),
            Map.entry("hpp", "cpp"),
            Map.entry("swift", "swift"),
            Map.entry("scala", "scala"),
            Map.entry("sql", "sql"),
            Map.entry("md", "markdown"),
            Map.entry("yaml", "yaml"),
            Map.entry("yml", "yaml"),
            Map.entry("json", "json"),
            Map.entry("xml", "xml"),
            Map.entry("html", "html"),
            Map.entry("css", "css"),
            Map.entry("scss", "scss"),
            Map.entry("sh", "shell"),
            Map.entry("bash", "shell"),
            Map.entry("dockerfile", "dockerfile"),
            Map.entry("properties", "properties"),
            Map.entry("gradle", "gradle")
    );

    public String detect(String path, String extension) {
        if (extension != null && !extension.isBlank()) {
            String fromExt = EXTENSION_TO_LANGUAGE.get(extension.toLowerCase(Locale.ROOT));
            if (fromExt != null) {
                return fromExt;
            }
        }
        if (path != null) {
            String fileName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
            if ("Dockerfile".equalsIgnoreCase(fileName)) {
                return "dockerfile";
            }
            if ("Makefile".equalsIgnoreCase(fileName)) {
                return "makefile";
            }
        }
        return extension != null && !extension.isBlank() ? extension.toLowerCase(Locale.ROOT) : "unknown";
    }

    public static String extensionFromPath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String fileName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static String fileNameFromPath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        return path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
    }
}

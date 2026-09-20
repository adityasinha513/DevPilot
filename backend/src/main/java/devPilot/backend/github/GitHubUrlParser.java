package devPilot.backend.github;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import devPilot.backend.exceptions.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class GitHubUrlParser {

    private static final Pattern SSH_PATTERN = Pattern.compile("^git@github\\.com:([^/]+)/(.+?)(?:\\.git)?$");
    private static final Pattern HTTPS_PATTERN = Pattern.compile("^https?://github\\.com/([^/]+)/(.+?)(?:\\.git)?/?$");

    public GitHubRepoRef parse(String input) {
        if (input == null || input.isBlank()) {
            throw new BadRequestException("Repository reference is required");
        }

        String trimmed = input.trim();

        if (trimmed.contains("://") && !trimmed.contains("github.com")) {
            throw new BadRequestException("Only github.com repository URLs are supported");
        }

        Matcher ssh = SSH_PATTERN.matcher(trimmed);
        if (ssh.matches()) {
            return new GitHubRepoRef(ssh.group(1), stripGitSuffix(ssh.group(2)));
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            Matcher https = HTTPS_PATTERN.matcher(trimmed);
            if (https.matches()) {
                return new GitHubRepoRef(https.group(1), stripGitSuffix(https.group(2)));
            }
            try {
                URI uri = URI.create(trimmed);
                if ("github.com".equalsIgnoreCase(uri.getHost())) {
                    String path = uri.getPath();
                    if (path != null && path.length() > 1) {
                        String[] parts = path.replaceFirst("^/", "").split("/");
                        if (parts.length >= 2) {
                            return new GitHubRepoRef(parts[0], stripGitSuffix(parts[1]));
                        }
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
            throw new BadRequestException("Invalid GitHub repository URL");
        }

        if (trimmed.contains("/") && !trimmed.contains(" ")) {
            String[] parts = trimmed.split("/", 2);
            if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                return new GitHubRepoRef(parts[0].trim(), stripGitSuffix(parts[1].trim()));
            }
        }

        throw new BadRequestException("Provide owner/name or a valid GitHub repository URL");
    }

    private static String stripGitSuffix(String name) {
        return name.endsWith(".git") ? name.substring(0, name.length() - 4) : name;
    }
}

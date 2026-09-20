package devPilot.backend.github;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class GitHubApiModels {

    private GitHubApiModels() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubRepositoryJson(
            Long id,
            String name,
            @JsonProperty("full_name") String fullName,
            GitHubOwnerJson owner,
            @JsonProperty("private") boolean isPrivate,
            @JsonProperty("html_url") String htmlUrl,
            String description,
            @JsonProperty("default_branch") String defaultBranch
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubOwnerJson(String login) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubRefJson(GitHubRefObjectJson object) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubRefObjectJson(String sha, String type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubCommitJson(GitHubCommitTreeJson tree) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubCommitTreeJson(String sha) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubTreeJson(
            String sha,
            List<GitHubTreeEntryJson> tree,
            Boolean truncated
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubTreeEntryJson(
            String path,
            String mode,
            String type,
            String sha,
            Long size
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubBlobJson(
            String sha,
            Long size,
            String content,
            String encoding
    ) {
    }
}

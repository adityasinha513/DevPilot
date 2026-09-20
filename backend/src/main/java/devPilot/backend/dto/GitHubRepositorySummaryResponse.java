package devPilot.backend.dto;

public record GitHubRepositorySummaryResponse(
        Long githubRepoId,
        String owner,
        String name,
        String fullName,
        String defaultBranch,
        boolean isPrivate,
        String htmlUrl,
        String description
) {
}

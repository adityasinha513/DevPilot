package devPilot.backend.dto;

import java.time.Instant;
import java.util.UUID;

import devPilot.backend.entity.GitRepository.ProcessingStatus;

public record StoredRepositoryResponse(
        UUID id,
        Long githubRepoId,
        String owner,
        String name,
        String fullName,
        String defaultBranch,
        boolean isPrivate,
        String htmlUrl,
        String description,
        ProcessingStatus processingStatus,
        String processingError,
        String indexedCommitSha,
        String indexedBranch,
        int filesDiscovered,
        int filesProcessed,
        int filesSkipped,
        int filesFailed,
        int progressPercent,
        Instant createdAt,
        Instant updatedAt
) {
}

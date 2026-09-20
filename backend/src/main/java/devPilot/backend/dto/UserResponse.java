package devPilot.backend.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String githubUsername,
        String displayName,
        String avatarUrl
) {
}
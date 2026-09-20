package devPilot.backend.dto;
import java.time.Instant; import java.util.UUID;
public record ConversationResponse(UUID id, UUID repositoryId, String title, Instant createdAt, Instant updatedAt) {}

package devPilot.backend.dto;
import java.time.Instant; import java.util.*; import devPilot.backend.entity.ChatMessage.Role;
public record MessageResponse(UUID id, Role role, String content, List<CitationResponse> citations, Instant createdAt) {}

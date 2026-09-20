package devPilot.backend.dto;
import jakarta.validation.constraints.NotBlank; public record CreateConversationRequest(@NotBlank String title) {}

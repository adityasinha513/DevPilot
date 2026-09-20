package devPilot.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ConnectRepositoryRequest(
        @NotBlank String reference
) {
}

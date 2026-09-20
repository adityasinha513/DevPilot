package devPilot.backend.dto;
import java.util.UUID;
public record CitationResponse(UUID chunkId, String path, int startLine, int endLine, String url) {}

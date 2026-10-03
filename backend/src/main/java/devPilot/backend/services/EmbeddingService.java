package devPilot.backend.services;

import java.util.List;
import java.util.UUID;

import devPilot.backend.entity.CodeChunk;
import devPilot.backend.exceptions.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final VectorEmbeddingStore store;

    @Value("${app.ai.max-retries:3}")
    private int maxRetries;

    @Value("${app.ai.retry-backoff-ms:750}")
    private long retryBackoffMs;

    public void embedAll(UUID repositoryId, String commit, List<CodeChunk> chunks) {
        for (CodeChunk chunk : chunks) {
            store.save(chunk.getId(), embed(chunk.getContent()));
        }
    }

    public float[] embedQuery(String query) {
        return embed(query);
    }

    public void deleteStale(UUID repositoryId, String commitSha) {
        store.deleteForCommit(repositoryId, commitSha);
    }

    private float[] embed(String value) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= Math.max(1, maxRetries); attempt++) {
            try {
                return embeddingModel.embed(value);
            } catch (RuntimeException exception) {
                last = exception;
                log.warn("Embedding attempt {} failed: {}", attempt, exception.getMessage());
                if (isPermanentError(exception)) {
                    throw new ExternalServiceException("Embedding provider rejected request", exception);
                }
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryBackoffMs * attempt);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new ExternalServiceException("Embedding provider is unavailable", last);
    }

    private boolean isPermanentError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof HttpClientErrorException clientError) {
                if (clientError.getStatusCode().value() != 429) {
                    return true;
                }
            }
            if (current.getClass().getSimpleName().contains("NonTransient")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

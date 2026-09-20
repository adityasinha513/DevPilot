package devPilot.backend.services;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IngestionJobRunner {

    private final RepositoryIngestionService repositoryIngestionService;

    @Async("ingestionExecutor")
    public void runJobAsync(UUID jobId) {
        repositoryIngestionService.executeJob(jobId);
    }
}

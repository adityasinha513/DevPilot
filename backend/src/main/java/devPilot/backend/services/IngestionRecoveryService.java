package devPilot.backend.services;

import devPilot.backend.entity.GitRepository;
import devPilot.backend.repository.GitRepositoryRepository;
import devPilot.backend.repository.RepositoryProcessingJobRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** In-process jobs cannot survive a restart; make interrupted work explicitly retryable. */
@Component
@RequiredArgsConstructor
@Slf4j
public class IngestionRecoveryService {
    private final GitRepositoryRepository repositories;
    private final RepositoryProcessingJobRepository jobs;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recoverInterruptedJobs() {
        List<GitRepository> interrupted = repositories.findByProcessingStatusIn(List.of(
                GitRepository.ProcessingStatus.QUEUED,
                GitRepository.ProcessingStatus.INGESTING,
                GitRepository.ProcessingStatus.CHUNKING,
                GitRepository.ProcessingStatus.EMBEDDING));
        for (GitRepository repository : interrupted) {
            repository.setProcessingStatus(GitRepository.ProcessingStatus.FAILED_INGESTION);
            repository.setProcessingError("Analysis was interrupted by a service restart. Retry to continue.");
            jobs.findFirstByRepositoryIdOrderByCreatedAtDesc(repository.getId()).ifPresent(job -> {
                if (job.getStatus() == GitRepository.ProcessingStatus.QUEUED
                        || job.getStatus() == GitRepository.ProcessingStatus.INGESTING
                        || job.getStatus() == GitRepository.ProcessingStatus.CHUNKING
                        || job.getStatus() == GitRepository.ProcessingStatus.EMBEDDING) {
                    job.setStatus(GitRepository.ProcessingStatus.FAILED_INGESTION);
                    job.setErrorMessage("Analysis was interrupted by a service restart. Retry to continue.");
                    job.setCompletedAt(Instant.now());
                }
            });
        }
        if (!interrupted.isEmpty()) log.warn("Marked {} interrupted repository indexing job(s) retryable at {}", interrupted.size(), Instant.now());
    }
}

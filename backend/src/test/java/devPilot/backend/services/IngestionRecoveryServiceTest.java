package devPilot.backend.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import devPilot.backend.entity.GitRepository;
import devPilot.backend.entity.RepositoryProcessingJob;
import devPilot.backend.repository.GitRepositoryRepository;
import devPilot.backend.repository.RepositoryProcessingJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestionRecoveryServiceTest {

    @Mock
    private GitRepositoryRepository repositories;

    @Mock
    private RepositoryProcessingJobRepository jobs;

    @InjectMocks
    private IngestionRecoveryService recoveryService;

    @Test
    void testRecoversInterruptedJobsAndRepositories() {
        UUID repoId = UUID.randomUUID();
        GitRepository repository = GitRepository.builder()
                .id(repoId)
                .processingStatus(GitRepository.ProcessingStatus.INGESTING)
                .build();

        RepositoryProcessingJob job = RepositoryProcessingJob.builder()
                .id(UUID.randomUUID())
                .status(GitRepository.ProcessingStatus.INGESTING)
                .build();

        when(repositories.findByProcessingStatusIn(any())).thenReturn(List.of(repository));
        when(jobs.findFirstByRepositoryIdOrderByCreatedAtDesc(repoId)).thenReturn(Optional.of(job));

        recoveryService.recoverInterruptedJobs();

        assertEquals(GitRepository.ProcessingStatus.FAILED_INGESTION, repository.getProcessingStatus());
        assertNotNull(repository.getProcessingError());

        assertEquals(GitRepository.ProcessingStatus.FAILED_INGESTION, job.getStatus());
        assertNotNull(job.getErrorMessage());
        assertNotNull(job.getCompletedAt());
    }
}

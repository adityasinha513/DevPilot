package devPilot.backend.repository;

import java.util.Optional;
import java.util.UUID;

import devPilot.backend.entity.RepositoryProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryProcessingJobRepository extends JpaRepository<RepositoryProcessingJob, UUID> {

    Optional<RepositoryProcessingJob> findFirstByRepositoryIdOrderByCreatedAtDesc(UUID repositoryId);
}

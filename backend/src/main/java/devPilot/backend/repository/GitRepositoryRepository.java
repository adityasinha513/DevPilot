package devPilot.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.util.UUID;

import devPilot.backend.entity.GitRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GitRepositoryRepository extends JpaRepository<GitRepository, UUID> {

    List<GitRepository> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<GitRepository> findByUserIdAndOwnerAndName(UUID userId, String owner, String name);

    List<GitRepository> findByProcessingStatusIn(Collection<GitRepository.ProcessingStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from GitRepository r where r.id = :id")
    Optional<GitRepository> findByIdForUpdate(@Param("id") UUID id);
}

package devPilot.backend.repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import devPilot.backend.entity.RepositoryFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositoryFileRepository extends JpaRepository<RepositoryFile, UUID> {

    Optional<RepositoryFile> findByRepositoryIdAndCommitShaAndPath(
            UUID repositoryId, String commitSha, String path);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RepositoryFile f where f.repository.id = :repositoryId and f.commitSha <> :commitSha")
    int deleteByRepositoryIdAndCommitShaNot(@Param("repositoryId") UUID repositoryId, @Param("commitSha") String commitSha);

    long countByRepositoryIdAndCommitShaAndIngestStatus(
            UUID repositoryId, String commitSha, RepositoryFile.IngestStatus status);

    List<RepositoryFile> findByRepositoryIdAndCommitShaAndIngestStatus(
            UUID repositoryId, String commitSha, RepositoryFile.IngestStatus status);
}

package devPilot.backend.repository;
import java.util.*; import devPilot.backend.entity.CodeChunk; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface CodeChunkRepository extends JpaRepository<CodeChunk, UUID> {
 List<CodeChunk> findByRepositoryFileIdOrderByChunkIndex(UUID fileId);
 List<CodeChunk> findByRepositoryIdAndCommitSha(UUID repositoryId, String commitSha);
 @Modifying @Query("delete from CodeChunk c where c.repository.id=:repositoryId and c.commitSha<>:commitSha") int deleteStale(@Param("repositoryId") UUID repositoryId,@Param("commitSha") String commitSha);
}

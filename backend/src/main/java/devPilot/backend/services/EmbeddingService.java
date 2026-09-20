package devPilot.backend.services;
import java.util.*; import devPilot.backend.entity.CodeChunk; import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j; import org.springframework.ai.embedding.EmbeddingModel; import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor @Slf4j
public class EmbeddingService {
 private final EmbeddingModel embeddingModel; private final VectorEmbeddingStore store;
 public void embedAll(UUID repositoryId,String commit,List<CodeChunk> chunks){ for(CodeChunk chunk:chunks){try{store.save(chunk.getId(),embeddingModel.embed(chunk.getContent()));}catch(RuntimeException ex){log.warn("Embedding failed for chunk {}",chunk.getId(),ex); throw ex;}} }
 public float[] embedQuery(String query){return embeddingModel.embed(query);}
 public void deleteStale(UUID repositoryId, String commitSha) { store.deleteForCommit(repositoryId, commitSha); }
}

package devPilot.backend.services;
import java.util.*; import devPilot.backend.entity.GitRepository; import devPilot.backend.exceptions.BadRequestException; import lombok.RequiredArgsConstructor; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
public class RepositoryRetrievalService {
 private final EmbeddingService embeddingService; private final VectorEmbeddingStore store;
 @Value("${app.rag.retrieval-top-k:8}") private int topK;
 public List<VectorEmbeddingStore.SearchHit> retrieve(GitRepository repository,String question){
  if(repository.getProcessingStatus()!=GitRepository.ProcessingStatus.READY || repository.getIndexedCommitSha()==null) throw new BadRequestException("Repository analysis is not ready yet");
  int safeTopK = Math.min(Math.max(topK, 1), 20);
  return store.search(repository.getId(),repository.getIndexedCommitSha(),embeddingService.embedQuery(question),safeTopK);
 }
}

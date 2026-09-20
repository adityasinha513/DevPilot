package devPilot.backend.services;
import java.util.*; import devPilot.backend.entity.*; import devPilot.backend.indexing.CodeChunkingService; import devPilot.backend.repository.*; import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class RepositoryChunkingService {
 private final RepositoryFileRepository files; private final CodeChunkRepository chunks; private final CodeChunkingService chunker;
 @Transactional public List<CodeChunk> createChunks(UUID repositoryId,String commitSha){
  List<CodeChunk> created=new ArrayList<>();
  for(RepositoryFile file: files.findByRepositoryIdAndCommitShaAndIngestStatus(repositoryId,commitSha,RepositoryFile.IngestStatus.STORED)){
   if(!chunks.findByRepositoryFileIdOrderByChunkIndex(file.getId()).isEmpty()) continue;
   List<CodeChunkingService.ChunkDraft> drafts=chunker.chunk(file.getContent(),file.getLanguage()); int i=0;
   for(var d:drafts) created.add(CodeChunk.builder().repository(file.getRepository()).repositoryFile(file).commitSha(commitSha).path(file.getPath()).language(file.getLanguage()).chunkIndex(i++).content(d.content()).startLine(d.startLine()).endLine(d.endLine()).symbolName(d.symbolName()).chunkType(d.symbolName()==null?"text":"symbol").importsJson(String.join("\n",d.imports())).metadataJson("{\"filename\":\""+file.getFileName().replace("\"", "")+"\"}").build());
  }
  chunks.saveAll(created); return chunks.findByRepositoryIdAndCommitSha(repositoryId, commitSha);
 }
 @Transactional public void deleteStale(UUID repositoryId, String commitSha) { chunks.deleteStale(repositoryId, commitSha); }
}

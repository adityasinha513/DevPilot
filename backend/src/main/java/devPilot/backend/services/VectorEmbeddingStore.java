package devPilot.backend.services;
import java.util.*; import javax.sql.DataSource; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.stereotype.Service;
/** pgvector access kept outside JPA so embeddings are never serialized into API DTOs. */
@Service
public class VectorEmbeddingStore {
 private final JdbcTemplate jdbc;
 public VectorEmbeddingStore(DataSource dataSource){this.jdbc=new JdbcTemplate(dataSource);}
 public void save(UUID chunkId,float[] embedding){ jdbc.update("insert into code_chunk_embeddings (chunk_id, embedding) values (?, cast(? as vector)) on conflict (chunk_id) do update set embedding=excluded.embedding",chunkId,vector(embedding)); }
 public void deleteForCommit(UUID repositoryId,String commit){ jdbc.update("delete from code_chunk_embeddings e using code_chunks c where e.chunk_id=c.id and c.repository_id=? and c.commit_sha<>?",repositoryId,commit); }
 public List<SearchHit> search(UUID repositoryId,String commit,float[] embedding,int limit){ return jdbc.query("select c.id, c.path, c.start_line, c.end_line, c.content, c.symbol_name, 1-(e.embedding <=> cast(? as vector)) score from code_chunk_embeddings e join code_chunks c on c.id=e.chunk_id where c.repository_id=? and c.commit_sha=? order by e.embedding <=> cast(? as vector) limit ?",(rs,row)->new SearchHit(UUID.fromString(rs.getString("id")),rs.getString("path"),rs.getInt("start_line"),rs.getInt("end_line"),rs.getString("content"),rs.getString("symbol_name"),rs.getDouble("score")),vector(embedding),repositoryId,commit,vector(embedding),limit); }
 private String vector(float[] values){StringBuilder b=new StringBuilder("["); for(int i=0;i<values.length;i++){if(i>0)b.append(',');b.append(values[i]);}return b.append(']').toString();}
 public record SearchHit(UUID chunkId,String path,int startLine,int endLine,String content,String symbolName,double score){}
}

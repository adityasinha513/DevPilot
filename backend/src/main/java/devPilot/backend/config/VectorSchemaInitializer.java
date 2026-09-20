package devPilot.backend.config;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** The vector table references a Hibernate-owned table, so it is created after JPA schema creation. */
@Component
@RequiredArgsConstructor
public class VectorSchemaInitializer {
    private final DataSource dataSource;
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        new JdbcTemplate(dataSource).execute("CREATE TABLE IF NOT EXISTS code_chunk_embeddings ("
            + "chunk_id UUID PRIMARY KEY REFERENCES code_chunks(id) ON DELETE CASCADE, embedding vector(1536) NOT NULL)");
        new JdbcTemplate(dataSource).execute("ALTER TABLE code_chunk_embeddings "
            + "ALTER COLUMN embedding TYPE vector(1536) USING embedding::vector(1536)");
        new JdbcTemplate(dataSource).execute("CREATE INDEX IF NOT EXISTS idx_chunk_embeddings_cosine "
                + "ON code_chunk_embeddings USING hnsw (embedding vector_cosine_ops)");
    }
}

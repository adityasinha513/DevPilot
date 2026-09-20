package devPilot.backend.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

/** A deterministic, commit-scoped piece of a repository file. */
@Entity
@Table(name = "code_chunks", uniqueConstraints = @UniqueConstraint(columnNames = {"repository_file_id", "chunk_index"}), indexes = {
        @Index(name = "idx_chunks_repository_commit", columnList = "repository_id,commit_sha"),
        @Index(name = "idx_chunks_path", columnList = "repository_id,path") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodeChunk {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "repository_id") private GitRepository repository;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "repository_file_id") private RepositoryFile repositoryFile;
    @Column(name = "commit_sha", nullable = false, length = 40) private String commitSha;
    @Column(nullable = false, length = 2048) private String path;
    @Column(length = 64) private String language;
    @Column(name = "chunk_index", nullable = false) private int chunkIndex;
    @Column(columnDefinition = "TEXT", nullable = false) private String content;
    @Column(name = "start_line", nullable = false) private int startLine;
    @Column(name = "end_line", nullable = false) private int endLine;
    @Column(name = "symbol_name", length = 512) private String symbolName;
    @Column(name = "chunk_type", length = 64) private String chunkType;
    @Column(name = "imports_json", columnDefinition = "TEXT") private String importsJson;
    @Column(name = "metadata_json", columnDefinition = "TEXT") private String metadataJson;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void created() { if (createdAt == null) createdAt = Instant.now(); }
}

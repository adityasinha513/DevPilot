package devPilot.backend.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "repository_files",
        uniqueConstraints = @UniqueConstraint(columnNames = { "repository_id", "commit_sha", "path" })
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositoryFile {

    public enum IngestStatus {
        STORED,
        SKIPPED_FILTERED,
        SKIPPED_TOO_LARGE,
        SKIPPED_BINARY,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private GitRepository repository;

    @Column(name = "commit_sha", nullable = false, length = 40)
    private String commitSha;

    @Column(name = "branch_name", nullable = false, length = 255)
    private String branchName;

    @Column(nullable = false, length = 2048)
    private String path;

    @Column(name = "file_name", nullable = false, length = 512)
    private String fileName;

    @Column(length = 32)
    private String extension;

    @Column(length = 64)
    private String language;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "blob_sha", nullable = false, length = 40)
    private String blobSha;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingest_status", nullable = false, length = 32)
    private IngestStatus ingestStatus;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

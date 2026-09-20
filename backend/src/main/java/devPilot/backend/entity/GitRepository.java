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
        name = "git_repositories",
        uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "owner", "name" })
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitRepository {

    public enum ProcessingStatus {
        NOT_STARTED,
        QUEUED,
        INGESTING, CHUNKING, EMBEDDING, READY,
        FAILED_INGESTION, FAILED_CHUNKING, FAILED_EMBEDDING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "github_repo_id")
    private Long githubRepoId;

    @Column(nullable = false, length = 100)
    private String owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "default_branch", length = 255)
    private String defaultBranch;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate;

    @Column(name = "html_url", length = 512)
    private String htmlUrl;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 32)
    private ProcessingStatus processingStatus;

    @Column(name = "processing_error", length = 2000)
    private String processingError;

    @Column(name = "indexed_commit_sha", length = 40)
    private String indexedCommitSha;

    @Column(name = "indexed_branch", length = 255)
    private String indexedBranch;

    @Column(name = "files_discovered", nullable = false)
    private int filesDiscovered;

    @Column(name = "files_processed", nullable = false)
    private int filesProcessed;

    @Column(name = "files_skipped", nullable = false)
    private int filesSkipped;

    @Column(name = "files_failed", nullable = false)
    private int filesFailed;

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
        if (processingStatus == null) {
            processingStatus = ProcessingStatus.NOT_STARTED;
        }
        // counters default to 0 via primitive int
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

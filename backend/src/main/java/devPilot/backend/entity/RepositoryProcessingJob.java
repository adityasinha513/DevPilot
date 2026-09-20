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
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "repository_processing_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositoryProcessingJob {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GitRepository.ProcessingStatus status;

    @Column(name = "files_discovered", nullable = false)
    private int filesDiscovered;

    @Column(name = "files_processed", nullable = false)
    private int filesProcessed;

    @Column(name = "files_skipped", nullable = false)
    private int filesSkipped;

    @Column(name = "files_failed", nullable = false)
    private int filesFailed;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

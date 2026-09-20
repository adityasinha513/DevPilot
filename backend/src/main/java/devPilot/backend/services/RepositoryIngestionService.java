package devPilot.backend.services;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import devPilot.backend.entity.GitRepository;
import devPilot.backend.entity.RepositoryFile;
import devPilot.backend.entity.RepositoryProcessingJob;
import devPilot.backend.entity.User;
import devPilot.backend.exceptions.BadRequestException;
import devPilot.backend.exceptions.NotFoundException;
import devPilot.backend.github.GitHubApiModels.GitHubBlobJson;
import devPilot.backend.github.GitHubApiService;
import devPilot.backend.github.GitHubTreeBlob;
import devPilot.backend.indexing.LanguageDetector;
import devPilot.backend.indexing.RepositoryPathFilter;
import devPilot.backend.repository.GitRepositoryRepository;
import devPilot.backend.repository.RepositoryFileRepository;
import devPilot.backend.repository.RepositoryProcessingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryIngestionService {

    private final GitRepositoryRepository gitRepositoryRepository;
    private final RepositoryProcessingJobRepository jobRepository;
    private final RepositoryFileRepository repositoryFileRepository;
    private final GitHubApiService gitHubApiService;
    private final RepositoryPathFilter repositoryPathFilter;
    private final LanguageDetector languageDetector;
    private final RepositoryChunkingService repositoryChunkingService;
    private final EmbeddingService embeddingService;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.indexing.max-file-bytes:102400}")
    private long maxFileBytes;

    @Value("${app.indexing.progress-update-interval:20}")
    private int progressUpdateInterval;

    @Transactional
    public UUID enqueueIngestion(UUID repositoryId) {
        return enqueueIngestion(repositoryId, false);
    }

    @Transactional
    public UUID retryIngestion(UUID repositoryId, UUID userId) {
        GitRepository repository = gitRepositoryRepository.findById(repositoryId)
                .filter(repo -> repo.getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Repository not found"));
        if (repository.getProcessingStatus() == GitRepository.ProcessingStatus.QUEUED
                || repository.getProcessingStatus() == GitRepository.ProcessingStatus.INGESTING
                || repository.getProcessingStatus() == GitRepository.ProcessingStatus.CHUNKING
                || repository.getProcessingStatus() == GitRepository.ProcessingStatus.EMBEDDING) {
            throw new BadRequestException("Repository ingestion is already in progress");
        }
        return enqueueIngestion(repositoryId, true);
    }

    private UUID enqueueIngestion(UUID repositoryId, boolean force) {
        GitRepository repository = gitRepositoryRepository.findByIdForUpdate(repositoryId)
                .orElseThrow(() -> new NotFoundException("Repository not found"));

        if (repository.getProcessingStatus() == GitRepository.ProcessingStatus.QUEUED
                || repository.getProcessingStatus() == GitRepository.ProcessingStatus.INGESTING
                || repository.getProcessingStatus() == GitRepository.ProcessingStatus.CHUNKING
                || repository.getProcessingStatus() == GitRepository.ProcessingStatus.EMBEDDING) {
            return null;
        }

        User user = repository.getUser();
        String branch = repository.getDefaultBranch() != null && !repository.getDefaultBranch().isBlank()
                ? repository.getDefaultBranch()
                : "main";
        String commitSha = gitHubApiService.resolveCommitSha(user, repository.getOwner(), repository.getName(), branch);

        if (!force
                && commitSha.equals(repository.getIndexedCommitSha())
                && repository.getProcessingStatus() == GitRepository.ProcessingStatus.READY) {
            return null;
        }

        RepositoryProcessingJob job = RepositoryProcessingJob.builder()
                .repository(repository)
                .commitSha(commitSha)
                .branchName(branch)
                .status(GitRepository.ProcessingStatus.QUEUED)
                .build();
        job = jobRepository.save(job);

        repository.setProcessingStatus(GitRepository.ProcessingStatus.QUEUED);
        repository.setProcessingError(null);
        repository.setIndexedBranch(branch);
        repository.setFilesDiscovered(0);
        repository.setFilesProcessed(0);
        repository.setFilesSkipped(0);
        repository.setFilesFailed(0);
        gitRepositoryRepository.save(repository);

        return job.getId();
    }

    public void executeJob(UUID jobId) {
        JobRunContext context = transactionTemplate.execute(status -> {
            RepositoryProcessingJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new NotFoundException("Ingestion job not found"));
            GitRepository repository = job.getRepository();
            User user = repository.getUser();
            user.getAccessToken();
            return new JobRunContext(
                    jobId,
                    repository.getId(),
                    user,
                    repository.getOwner(),
                    repository.getName(),
                    job.getCommitSha(),
                    job.getBranchName(),
                    repository);
        });

        if (context == null) {
            return;
        }

        UUID repositoryId = context.repositoryId();
        User user = context.user();
        String owner = context.owner();
        String repoName = context.repoName();
        String commitSha = context.commitSha();
        String branch = context.branch();
        GitRepository repository = context.repository();

        try {
            markJobRunning(jobId, repositoryId);

            String rootTreeSha = gitHubApiService.resolveRootTreeSha(user, owner, repoName, commitSha);
            List<GitHubTreeBlob> allBlobs = gitHubApiService.listRepositoryBlobs(user, owner, repoName, rootTreeSha);

            List<GitHubTreeBlob> candidates = new ArrayList<>();
            for (GitHubTreeBlob blob : allBlobs) {
                if (repositoryPathFilter.shouldSkipPath(blob.path())) {
                    continue;
                }
                String extension = LanguageDetector.extensionFromPath(blob.path());
                if (repositoryPathFilter.isBinaryExtension(extension)) {
                    continue;
                }
                candidates.add(blob);
            }

            updateDiscoveredCounts(jobId, repositoryId, candidates.size());
            int processed = 0;
            int skipped = 0;
            int failed = 0;

            for (int index = 0; index < candidates.size(); index++) {
                GitHubTreeBlob blob = candidates.get(index);
                IngestOutcome outcome = ingestSingleFile(
                        user, repository, owner, repoName, branch, commitSha, blob);
                switch (outcome) {
                    case STORED -> processed++;
                    case SKIPPED -> skipped++;
                    case FAILED -> failed++;
                    default -> {
                    }
                }

                if ((index + 1) % progressUpdateInterval == 0 || index + 1 == candidates.size()) {
                    updateProgress(jobId, repositoryId, processed, skipped, failed);
                }
            }

            markStage(jobId, repositoryId, GitRepository.ProcessingStatus.CHUNKING);
            List<devPilot.backend.entity.CodeChunk> chunks = repositoryChunkingService.createChunks(repositoryId, commitSha);
            markStage(jobId, repositoryId, GitRepository.ProcessingStatus.EMBEDDING);
            embeddingService.embedAll(repositoryId, commitSha, chunks);
            removeStaleIndexData(repositoryId, commitSha);
            finalizeSuccess(jobId, repositoryId, commitSha, branch, processed, skipped, failed);
        } catch (Exception exception) {
            log.error("Repository ingestion failed for job {}", jobId, exception);
            finalizeFailure(jobId, repositoryId, exception.getMessage());
        }
    }

    private IngestOutcome ingestSingleFile(
            User user,
            GitRepository repository,
            String owner,
            String repoName,
            String branch,
            String commitSha,
            GitHubTreeBlob blob) {
        String path = blob.path();
        String extension = LanguageDetector.extensionFromPath(path);
        String fileName = LanguageDetector.fileNameFromPath(path);
        String language = languageDetector.detect(path, extension);
        long size = blob.size() != null ? blob.size() : 0L;

        if (size > maxFileBytes) {
            persistFileRecord(
                    repository, commitSha, branch, path, fileName, extension, language,
                    size, blob.sha(), RepositoryFile.IngestStatus.SKIPPED_TOO_LARGE, null, null);
            return IngestOutcome.SKIPPED;
        }

        try {
            GitHubBlobJson blobJson = gitHubApiService.fetchBlob(user, owner, repoName, blob.sha());
            if (blobJson == null) {
                persistFileRecord(
                        repository, commitSha, branch, path, fileName, extension, language,
                        size, blob.sha(), RepositoryFile.IngestStatus.FAILED, "Empty blob response", null);
                return IngestOutcome.FAILED;
            }

            long blobSize = blobJson.size() != null ? blobJson.size() : size;
            if (blobSize > maxFileBytes) {
                persistFileRecord(
                        repository, commitSha, branch, path, fileName, extension, language,
                        blobSize, blob.sha(), RepositoryFile.IngestStatus.SKIPPED_TOO_LARGE, null, null);
                return IngestOutcome.SKIPPED;
            }

            String content = decodeBlobContent(blobJson);
            if (content == null) {
                persistFileRecord(
                        repository, commitSha, branch, path, fileName, extension, language,
                        blobSize, blob.sha(), RepositoryFile.IngestStatus.SKIPPED_BINARY, null, null);
                return IngestOutcome.SKIPPED;
            }

            persistFileRecord(
                    repository, commitSha, branch, path, fileName, extension, language,
                    blobSize, blob.sha(), RepositoryFile.IngestStatus.STORED, null, content);
            return IngestOutcome.STORED;
        } catch (Exception exception) {
            persistFileRecord(
                    repository, commitSha, branch, path, fileName, extension, language,
                    size, blob.sha(), RepositoryFile.IngestStatus.FAILED,
                    truncate(exception.getMessage(), 1000), null);
            return IngestOutcome.FAILED;
        }
    }

    private void persistFileRecord(
            GitRepository repository,
            String commitSha,
            String branch,
            String path,
            String fileName,
            String extension,
            String language,
            long sizeBytes,
            String blobSha,
            RepositoryFile.IngestStatus status,
            String errorMessage,
            String content) {
        transactionTemplate.executeWithoutResult(tx -> {
            RepositoryFile file = repositoryFileRepository
                    .findByRepositoryIdAndCommitShaAndPath(repository.getId(), commitSha, path)
                    .orElseGet(RepositoryFile::new);
            file.setRepository(repository);
            file.setCommitSha(commitSha);
            file.setBranchName(branch);
            file.setPath(path);
            file.setFileName(fileName);
            file.setExtension(extension);
            file.setLanguage(language);
            file.setSizeBytes(sizeBytes);
            file.setBlobSha(blobSha);
            file.setIngestStatus(status);
            file.setErrorMessage(errorMessage);
            file.setContent(content);
            repositoryFileRepository.save(file);
        });
    }

    private String decodeBlobContent(GitHubBlobJson blobJson) {
        if (blobJson.content() == null || blobJson.content().isBlank()) {
            return "";
        }
        if (!"base64".equalsIgnoreCase(blobJson.encoding())) {
            return null;
        }
        byte[] decoded = Base64.getDecoder().decode(blobJson.content().replaceAll("\\s+", ""));
        if (containsNullByte(decoded)) {
            return null;
        }
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private boolean containsNullByte(byte[] bytes) {
        for (byte value : bytes) {
            if (value == 0) {
                return true;
            }
        }
        return false;
    }

    private void markJobRunning(UUID jobId, UUID repositoryId) {
        transactionTemplate.executeWithoutResult(status -> {
            RepositoryProcessingJob job = jobRepository.findById(jobId).orElseThrow();
            GitRepository repository = gitRepositoryRepository.findByIdForUpdate(repositoryId).orElseThrow();
            job.setStatus(GitRepository.ProcessingStatus.INGESTING);
            job.setStartedAt(Instant.now());
            repository.setProcessingStatus(GitRepository.ProcessingStatus.INGESTING);
            jobRepository.save(job);
            gitRepositoryRepository.save(repository);
        });
    }

    private void updateDiscoveredCounts(UUID jobId, UUID repositoryId, int discovered) {
        transactionTemplate.executeWithoutResult(status -> {
            RepositoryProcessingJob job = jobRepository.findById(jobId).orElseThrow();
            GitRepository repository = gitRepositoryRepository.findById(repositoryId).orElseThrow();
            job.setFilesDiscovered(discovered);
            repository.setFilesDiscovered(discovered);
            jobRepository.save(job);
            gitRepositoryRepository.save(repository);
        });
    }

    private void updateProgress(UUID jobId, UUID repositoryId, int processed, int skipped, int failed) {
        transactionTemplate.executeWithoutResult(status -> {
            RepositoryProcessingJob job = jobRepository.findById(jobId).orElseThrow();
            GitRepository repository = gitRepositoryRepository.findById(repositoryId).orElseThrow();
            job.setFilesProcessed(processed);
            job.setFilesSkipped(skipped);
            job.setFilesFailed(failed);
            repository.setFilesProcessed(processed);
            repository.setFilesSkipped(skipped);
            repository.setFilesFailed(failed);
            jobRepository.save(job);
            gitRepositoryRepository.save(repository);
        });
    }

    private void finalizeSuccess(
            UUID jobId,
            UUID repositoryId,
            String commitSha,
            String branch,
            int processed,
            int skipped,
            int failed) {
        transactionTemplate.executeWithoutResult(status -> {
            RepositoryProcessingJob job = jobRepository.findById(jobId).orElseThrow();
            GitRepository repository = gitRepositoryRepository.findByIdForUpdate(repositoryId).orElseThrow();

            job.setFilesProcessed(processed);
            job.setFilesSkipped(skipped);
            job.setFilesFailed(failed);
            job.setCompletedAt(Instant.now());

            repository.setFilesProcessed(processed);
            repository.setFilesSkipped(skipped);
            repository.setFilesFailed(failed);
            repository.setIndexedCommitSha(commitSha);
            repository.setIndexedBranch(branch);

            if (failed > 0 && processed == 0) {
                job.setStatus(GitRepository.ProcessingStatus.FAILED_INGESTION);
                repository.setProcessingStatus(GitRepository.ProcessingStatus.FAILED_INGESTION);
                String message = "Ingestion failed for all candidate files (" + failed + " failures)";
                job.setErrorMessage(message);
                repository.setProcessingError(message);
            } else {
                job.setStatus(GitRepository.ProcessingStatus.READY);
                repository.setProcessingStatus(GitRepository.ProcessingStatus.READY);
                if (failed > 0) {
                    String message = "Completed with " + failed + " file failures";
                    job.setErrorMessage(message);
                    repository.setProcessingError(message);
                } else {
                    job.setErrorMessage(null);
                    repository.setProcessingError(null);
                }
            }

            jobRepository.save(job);
            gitRepositoryRepository.save(repository);
        });
    }

    private void finalizeFailure(UUID jobId, UUID repositoryId, String message) {
        transactionTemplate.executeWithoutResult(status -> {
            RepositoryProcessingJob job = jobRepository.findById(jobId).orElseThrow();
            GitRepository repository = gitRepositoryRepository.findByIdForUpdate(repositoryId).orElseThrow();
            String safeMessage = truncate(message != null ? message : "Repository ingestion failed", 2000);
            GitRepository.ProcessingStatus failedStatus = switch (repository.getProcessingStatus()) {
                case CHUNKING -> GitRepository.ProcessingStatus.FAILED_CHUNKING;
                case EMBEDDING -> GitRepository.ProcessingStatus.FAILED_EMBEDDING;
                default -> GitRepository.ProcessingStatus.FAILED_INGESTION;
            };
            job.setStatus(failedStatus);
            job.setErrorMessage(safeMessage);
            job.setCompletedAt(Instant.now());
            repository.setProcessingStatus(failedStatus);
            repository.setProcessingError(safeMessage);
            jobRepository.save(job);
            gitRepositoryRepository.save(repository);
        });
    }

    private void markStage(UUID jobId, UUID repositoryId, GitRepository.ProcessingStatus stage) {
        transactionTemplate.executeWithoutResult(status -> {
            RepositoryProcessingJob job = jobRepository.findById(jobId).orElseThrow();
            GitRepository repository = gitRepositoryRepository.findByIdForUpdate(repositoryId).orElseThrow();
            job.setStatus(stage);
            repository.setProcessingStatus(stage);
            jobRepository.save(job);
            gitRepositoryRepository.save(repository);
        });
    }

    /** Keeps a successful prior index available until its replacement is fully embedded. */
    private void removeStaleIndexData(UUID repositoryId, String commitSha) {
        transactionTemplate.executeWithoutResult(status -> {
            embeddingService.deleteStale(repositoryId, commitSha);
            repositoryChunkingService.deleteStale(repositoryId, commitSha);
            repositoryFileRepository.deleteByRepositoryIdAndCommitShaNot(repositoryId, commitSha);
        });
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private enum IngestOutcome {
        STORED,
        SKIPPED,
        FAILED
    }

    private record JobRunContext(
            UUID jobId,
            UUID repositoryId,
            User user,
            String owner,
            String repoName,
            String commitSha,
            String branch,
            GitRepository repository) {
    }
}

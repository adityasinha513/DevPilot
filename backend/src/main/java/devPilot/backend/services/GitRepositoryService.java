package devPilot.backend.services;

import java.util.List;
import java.util.UUID;

import devPilot.backend.dto.GitHubRepositorySummaryResponse;
import devPilot.backend.dto.StoredRepositoryResponse;
import devPilot.backend.entity.GitRepository;
import devPilot.backend.entity.User;
import devPilot.backend.exceptions.NotFoundException;
import devPilot.backend.github.GitHubApiModels.GitHubRepositoryJson;
import devPilot.backend.github.GitHubApiService;
import devPilot.backend.github.GitHubRepoRef;
import devPilot.backend.github.GitHubUrlParser;
import devPilot.backend.repository.GitRepositoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class GitRepositoryService {

    private final GitRepositoryRepository gitRepositoryRepository;
    private final GitHubApiService gitHubApiService;
    private final GitHubUrlParser gitHubUrlParser;
    private final RepositoryIngestionService repositoryIngestionService;
    private final IngestionJobRunner ingestionJobRunner;
    private final TransactionTemplate transactionTemplate;

    @Transactional(readOnly = true)
    public List<StoredRepositoryResponse> listStoredRepositories(UUID userId) {
        return gitRepositoryRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoredRepositoryResponse getStoredRepository(UUID userId, UUID repositoryId) {
        GitRepository repository = gitRepositoryRepository.findById(repositoryId)
                .filter(repo -> repo.getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Repository not found"));
        return toResponse(repository);
    }

    @Transactional(readOnly = true)
    public List<GitHubRepositorySummaryResponse> listGitHubCatalog(User user, int page, int perPage) {
        return gitHubApiService.listAccessibleRepositories(user, page, perPage).stream()
                .map(this::toSummary)
                .toList();
    }

    public StoredRepositoryResponse connectRepository(User user, String reference) {
        GitRepository saved = transactionTemplate.execute(status -> persistConnection(user, reference));
        if (saved == null) {
            throw new NotFoundException("Could not connect repository");
        }
        startIngestion(saved.getId());
        return getStoredRepository(user.getId(), saved.getId());
    }

    public StoredRepositoryResponse retryIngestion(UUID userId, UUID repositoryId) {
        UUID jobId = repositoryIngestionService.retryIngestion(repositoryId, userId);
        if (jobId != null) {
            ingestionJobRunner.runJobAsync(jobId);
        }
        return getStoredRepository(userId, repositoryId);
    }

    private GitRepository persistConnection(User user, String reference) {
        GitHubRepoRef ref = gitHubUrlParser.parse(reference);
        GitHubRepositoryJson githubRepo = gitHubApiService.requireRepository(user, ref);

        GitRepository entity = gitRepositoryRepository
                .findByUserIdAndOwnerAndName(user.getId(), ref.owner(), ref.name())
                .orElseGet(GitRepository::new);

        entity.setUser(user);
        entity.setGithubRepoId(githubRepo.id());
        entity.setOwner(ref.owner());
        entity.setName(ref.name());
        entity.setFullName(githubRepo.fullName() != null ? githubRepo.fullName() : ref.fullName());
        entity.setDefaultBranch(githubRepo.defaultBranch());
        entity.setPrivate(githubRepo.isPrivate());
        entity.setHtmlUrl(githubRepo.htmlUrl());
        entity.setDescription(githubRepo.description());
        if (entity.getProcessingStatus() == null) {
            entity.setProcessingStatus(GitRepository.ProcessingStatus.NOT_STARTED);
        }

        return gitRepositoryRepository.save(entity);
    }

    private void startIngestion(UUID repositoryId) {
        UUID jobId = repositoryIngestionService.enqueueIngestion(repositoryId);
        if (jobId != null) {
            ingestionJobRunner.runJobAsync(jobId);
        }
    }

    private StoredRepositoryResponse toResponse(GitRepository repository) {
        return new StoredRepositoryResponse(
                repository.getId(),
                repository.getGithubRepoId(),
                repository.getOwner(),
                repository.getName(),
                repository.getFullName(),
                repository.getDefaultBranch(),
                repository.isPrivate(),
                repository.getHtmlUrl(),
                repository.getDescription(),
                repository.getProcessingStatus(),
                repository.getProcessingError(),
                repository.getIndexedCommitSha(),
                repository.getIndexedBranch(),
                repository.getFilesDiscovered(),
                repository.getFilesProcessed(),
                repository.getFilesSkipped(),
                repository.getFilesFailed(),
                progressPercent(repository),
                repository.getCreatedAt(),
                repository.getUpdatedAt()
        );
    }

    private static int progressPercent(GitRepository repository) {
        if (repository.getFilesDiscovered() <= 0) {
            return repository.getProcessingStatus() == GitRepository.ProcessingStatus.READY ? 100 : 0;
        }
        int handled = repository.getFilesProcessed()
                + repository.getFilesSkipped()
                + repository.getFilesFailed();
        return Math.min(100, (int) Math.round((handled * 100.0) / repository.getFilesDiscovered()));
    }

    private GitHubRepositorySummaryResponse toSummary(GitHubRepositoryJson repo) {
        String owner = repo.owner() != null ? repo.owner().login() : "";
        return new GitHubRepositorySummaryResponse(
                repo.id(),
                owner,
                repo.name(),
                repo.fullName(),
                repo.defaultBranch(),
                repo.isPrivate(),
                repo.htmlUrl(),
                repo.description()
        );
    }
}

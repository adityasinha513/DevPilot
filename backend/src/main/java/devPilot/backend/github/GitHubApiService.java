package devPilot.backend.github;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;
import java.time.Duration;

import devPilot.backend.entity.User;
import devPilot.backend.exceptions.BadRequestException;
import devPilot.backend.exceptions.NotFoundException;
import devPilot.backend.exceptions.UnauthorizedException;
import devPilot.backend.exceptions.ExternalServiceException;
import devPilot.backend.github.GitHubApiModels.GitHubBlobJson;
import devPilot.backend.github.GitHubApiModels.GitHubCommitJson;
import devPilot.backend.github.GitHubApiModels.GitHubRefJson;
import devPilot.backend.github.GitHubApiModels.GitHubRepositoryJson;
import devPilot.backend.github.GitHubApiModels.GitHubTreeEntryJson;
import devPilot.backend.github.GitHubApiModels.GitHubTreeJson;
import devPilot.backend.services.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Service
public class GitHubApiService {

    private final RestClient restClient;
    private final UserService userService;
    private final long apiDelayMs;
    private final int maxRetries;
    private final long retryBackoffMs;

    public GitHubApiService(
            UserService userService,
            @Value("${app.github.api-delay-ms:50}") long apiDelayMs,
            @Value("${app.github.max-retries:3}") int maxRetries,
            @Value("${app.github.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.github.connect-timeout-seconds:10}") long connectTimeoutSeconds,
            @Value("${app.github.read-timeout-seconds:30}") long readTimeoutSeconds) {
        this.userService = userService;
        this.apiDelayMs = apiDelayMs;
        this.maxRetries = Math.max(1, maxRetries);
        this.retryBackoffMs = Math.max(0, retryBackoffMs);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    public String resolveCommitSha(User user, String owner, String repo, String branch) {
        String token = userService.decryptAccessToken(user);
        try {
            pauseIfNeeded();
            GitHubRefJson ref = withRetries(() -> restClient.get()
                    .uri("/repos/{owner}/{repo}/git/ref/heads/{branch}", owner, repo, branch)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(GitHubRefJson.class));
            if (ref == null || ref.object() == null || ref.object().sha() == null) {
                throw new NotFoundException("Could not resolve branch: " + branch);
            }
            return ref.object().sha();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new NotFoundException("Branch not found: " + branch);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new UnauthorizedException("GitHub access token is invalid or lacks repository access");
        }
    }

    public String resolveRootTreeSha(User user, String owner, String repo, String commitSha) {
        String token = userService.decryptAccessToken(user);
        try {
            pauseIfNeeded();
            GitHubCommitJson commit = withRetries(() -> restClient.get()
                    .uri("/repos/{owner}/{repo}/git/commits/{commitSha}", owner, repo, commitSha)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(GitHubCommitJson.class));
            if (commit == null || commit.tree() == null || commit.tree().sha() == null) {
                throw new BadRequestException("Could not resolve commit tree for " + commitSha);
            }
            return commit.tree().sha();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new NotFoundException("Commit not found: " + commitSha);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new UnauthorizedException("GitHub access token is invalid or lacks repository access");
        }
    }

    public List<GitHubTreeBlob> listRepositoryBlobs(User user, String owner, String repo, String rootTreeSha) {
        String token = userService.decryptAccessToken(user);
        GitHubTreeJson recursiveTree = fetchTree(token, owner, repo, rootTreeSha, true);
        if (recursiveTree != null
                && recursiveTree.tree() != null
                && !Boolean.TRUE.equals(recursiveTree.truncated())) {
            return recursiveTree.tree().stream()
                    .filter(entry -> "blob".equals(entry.type()))
                    .map(entry -> new GitHubTreeBlob(entry.path(), entry.sha(), entry.size()))
                    .toList();
        }
        return listRepositoryBlobsBreadthFirst(token, owner, repo, rootTreeSha);
    }

    public GitHubBlobJson fetchBlob(User user, String owner, String repo, String blobSha) {
        String token = userService.decryptAccessToken(user);
        try {
            pauseIfNeeded();
            return withRetries(() -> restClient.get()
                    .uri("/repos/{owner}/{repo}/git/blobs/{sha}", owner, repo, blobSha)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(GitHubBlobJson.class));
        } catch (HttpClientErrorException.NotFound ex) {
            throw new NotFoundException("Blob not found: " + blobSha);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new UnauthorizedException("GitHub access token is invalid or lacks repository access");
        }
    }

    public List<GitHubRepositoryJson> listAccessibleRepositories(User user, int page, int perPage) {
        String token = userService.decryptAccessToken(user);
        int safePage = Math.max(page, 1);
        int safePerPage = Math.min(Math.max(perPage, 1), 100);

        try {
            pauseIfNeeded();
            GitHubRepositoryJson[] repos = withRetries(() -> restClient.get()
                    .uri(uri -> uri
                            .path("/user/repos")
                            .queryParam("sort", "updated")
                            .queryParam("per_page", safePerPage)
                            .queryParam("page", safePage)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(GitHubRepositoryJson[].class));

            if (repos == null) {
                return List.of();
            }
            return List.of(repos);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new UnauthorizedException("GitHub access token is invalid or lacks required permissions");
        }
    }

    public GitHubRepositoryJson getRepository(User user, GitHubRepoRef ref) {
        String token = userService.decryptAccessToken(user);
        try {
            pauseIfNeeded();
            return withRetries(() -> restClient.get()
                    .uri("/repos/{owner}/{repo}", ref.owner(), ref.name())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(GitHubRepositoryJson.class));
        } catch (HttpClientErrorException.NotFound ex) {
            throw new NotFoundException("Repository not found or you do not have access: " + ref.fullName());
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new UnauthorizedException("GitHub access token is invalid or lacks access to this repository");
        }
    }

    public GitHubRepositoryJson requireRepository(User user, GitHubRepoRef ref) {
        GitHubRepositoryJson repo = getRepository(user, ref);
        if (repo == null) {
            throw new NotFoundException("Repository not found: " + ref.fullName());
        }
        return repo;
    }

    public List<GitHubRepositoryJson> listAllAccessibleRepositories(User user, int maxPages) {
        List<GitHubRepositoryJson> all = new ArrayList<>();
        int pages = Math.min(Math.max(maxPages, 1), 10);
        for (int page = 1; page <= pages; page++) {
            List<GitHubRepositoryJson> batch = listAccessibleRepositories(user, page, 100);
            all.addAll(batch);
            if (batch.size() < 100) {
                break;
            }
        }
        return all;
    }

    private List<GitHubTreeBlob> listRepositoryBlobsBreadthFirst(
            String token, String owner, String repo, String rootTreeSha) {
        List<GitHubTreeBlob> blobs = new ArrayList<>();
        Deque<QueuedTree> queue = new ArrayDeque<>();
        queue.add(new QueuedTree(rootTreeSha, ""));

        while (!queue.isEmpty()) {
            QueuedTree current = queue.removeFirst();
            GitHubTreeJson tree = fetchTree(token, owner, repo, current.sha(), false);
            if (tree == null || tree.tree() == null) {
                continue;
            }
            for (GitHubTreeEntryJson entry : tree.tree()) {
                if (entry.sha() == null || entry.path() == null) {
                    continue;
                }
                String fullPath = current.prefix().isEmpty()
                        ? entry.path()
                        : current.prefix() + "/" + entry.path();
                if ("blob".equals(entry.type())) {
                    blobs.add(new GitHubTreeBlob(fullPath, entry.sha(), entry.size()));
                } else if ("tree".equals(entry.type())) {
                    queue.addLast(new QueuedTree(entry.sha(), fullPath));
                }
            }
        }
        return blobs;
    }

    private GitHubTreeJson fetchTree(String token, String owner, String repo, String treeSha, boolean recursive) {
        try {
            pauseIfNeeded();
            return withRetries(() -> restClient.get()
                    .uri("/repos/{owner}/{repo}/git/trees/{treeSha}?recursive={recursive}",
                            owner, repo, treeSha, recursive ? "1" : "0")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(GitHubTreeJson.class));
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new UnauthorizedException("GitHub access token is invalid or lacks repository access");
        }
    }

    private record QueuedTree(String sha, String prefix) {
    }

    private <T> T withRetries(Supplier<T> request) {
        ResourceAccessException lastFailure = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return request.get();
            } catch (ResourceAccessException exception) {
                lastFailure = exception;
                if (attempt < maxRetries) sleep(retryBackoffMs * attempt);
            }
        }
        throw new ExternalServiceException("GitHub is unavailable", lastFailure);
    }

    private void pauseIfNeeded() {
        if (apiDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(apiDelayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("GitHub API request interrupted");
        }
    }

    private void sleep(long delayMs) {
        if (delayMs <= 0) return;
        try { Thread.sleep(delayMs); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new ExternalServiceException("GitHub request interrupted", exception); }
    }
}

package devPilot.backend.controllers;

import java.util.List;
import java.util.UUID;

import devPilot.backend.dto.ConnectRepositoryRequest;
import devPilot.backend.dto.GitHubRepositorySummaryResponse;
import devPilot.backend.dto.StoredRepositoryResponse;
import devPilot.backend.security.AppUserPrincipal;
import devPilot.backend.security.CurrentUser;
import devPilot.backend.services.GitRepositoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
public class RepositoryController {

    private final CurrentUser currentUser;
    private final GitRepositoryService gitRepositoryService;

    @GetMapping
    public List<StoredRepositoryResponse> listStored() {
        AppUserPrincipal principal = currentUser.require();
        return gitRepositoryService.listStoredRepositories(principal.getId());
    }

    @GetMapping("/catalog")
    public List<GitHubRepositorySummaryResponse> listGitHubCatalog(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int perPage) {
        AppUserPrincipal principal = currentUser.require();
        return gitRepositoryService.listGitHubCatalog(principal.getUser(), page, perPage);
    }

    @GetMapping("/{repositoryId}")
    public StoredRepositoryResponse getStored(@PathVariable UUID repositoryId) {
        AppUserPrincipal principal = currentUser.require();
        return gitRepositoryService.getStoredRepository(principal.getId(), repositoryId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoredRepositoryResponse connect(@Valid @RequestBody ConnectRepositoryRequest request) {
        AppUserPrincipal principal = currentUser.require();
        return gitRepositoryService.connectRepository(principal.getUser(), request.reference());
    }

    @PostMapping("/{repositoryId}/retry-ingestion")
    public StoredRepositoryResponse retryIngestion(@PathVariable UUID repositoryId) {
        AppUserPrincipal principal = currentUser.require();
        return gitRepositoryService.retryIngestion(principal.getId(), repositoryId);
    }
}

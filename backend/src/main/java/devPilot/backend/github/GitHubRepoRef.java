package devPilot.backend.github;

public record GitHubRepoRef(String owner, String name) {

    public String fullName() {
        return owner + "/" + name;
    }
}

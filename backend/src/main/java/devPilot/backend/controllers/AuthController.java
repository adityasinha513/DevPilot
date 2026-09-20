package devPilot.backend.controllers;

import java.util.Map;

import devPilot.backend.dto.UserResponse;
import devPilot.backend.security.AppUserPrincipal;
import devPilot.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final CurrentUser currentUser;

    @GetMapping("/login-url")
    public Map<String, String> loginUrl() {
        String githubLoginUrl = "/oauth2/authorization/github";
        return Map.of("loginUrl", githubLoginUrl);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        AppUserPrincipal userPrincipal = currentUser.require();
        UserResponse userResponse = new UserResponse(
                userPrincipal.getId(),
                userPrincipal.getUser().getGithubUsername(),
                userPrincipal.getUser().getDisplayName(),
                userPrincipal.getUser().getAvatarUrl()
        );
        return ResponseEntity.ok(userResponse);
    }
}
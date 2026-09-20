package devPilot.backend.security;

import devPilot.backend.exceptions.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public AppUserPrincipal require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new UnauthorizedException("No authenticated user found");
        }
        return principal;
    }
}
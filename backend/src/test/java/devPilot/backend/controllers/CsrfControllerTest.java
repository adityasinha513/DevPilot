package devPilot.backend.controllers;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CsrfControllerTest {

    private final CsrfController controller = new CsrfController();

    @Test
    void testCsrfTokenReturned() {
        DefaultCsrfToken token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token-value");
        Map<String, String> response = controller.csrf(token);
        assertNotNull(response);
        assertEquals("X-CSRF-TOKEN", response.get("headerName"));
        assertEquals("test-token-value", response.get("token"));
    }

    @Test
    void testNullCsrfTokenSafeFallback() {
        Map<String, String> response = controller.csrf(null);
        assertNotNull(response);
        assertEquals("X-CSRF-TOKEN", response.get("headerName"));
        assertEquals("", response.get("token"));
    }
}

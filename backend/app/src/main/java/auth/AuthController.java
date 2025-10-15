package auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    @org.springframework.beans.factory.annotation.Value("${app.cookies.secure:false}")
    private boolean cookieSecure;
    @org.springframework.beans.factory.annotation.Value("${app.cookies.sameSite:Lax}")
    private String cookieSameSite;
    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:${app.cors.allowedOrigin:http://localhost:3000}}")
    private String frontendBase;
    @org.springframework.beans.factory.annotation.Value("${app.auth.devEndpoints:false}")
    private boolean devEndpoints;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req) {
        return authService.signup(req);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletResponse res) {
        log.debug("Login attempt for {}", req.email());
        AuthService.LoginResult result = authService.login(req);
        setRefreshCookie(res, result.refreshToken(), result.refreshTtlSeconds());
        log.debug("Login success for {}", req.email());
        return ResponseEntity.ok(java.util.Map.of(
            "accessToken", result.accessToken(),
            "role", result.role()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                     HttpServletResponse res) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", "Missing refresh token"));
        }
        AuthService.LoginResult result = authService.refresh(refreshToken);
        setRefreshCookie(res, result.refreshToken(), result.refreshTtlSeconds());
        return ResponseEntity.ok(java.util.Map.of(
            "accessToken", result.accessToken(),
            "role", result.role()
        ));
    }

    // Optional: support redirect-based refresh from backend directly
    @GetMapping("/refresh")
    public void refreshRedirect(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                @RequestParam(name = "returnTo", defaultValue = "/") String returnTo,
                                HttpServletResponse res) throws java.io.IOException {
        if (refreshToken == null || refreshToken.isBlank()) {
            // Redirect to frontend login when no refresh token
            res.setStatus(303);
            res.setHeader("Location", frontendBase + "/login");
            return;
        }
        try {
            AuthService.LoginResult result = authService.refresh(refreshToken);
            setRefreshCookie(res, result.refreshToken(), result.refreshTtlSeconds());
            // Keep headers small; do not set extra cookies on redirect
            res.setStatus(303);
            res.setHeader("Location", returnTo);
        } catch (AuthService.Unauthorized ex) {
            // Redirect to frontend login on unauthorized session
            res.setStatus(303);
            res.setHeader("Location", frontendBase + "/login");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                    HttpServletResponse res) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
    // Clear cookie with matching attributes
    String clear = "refresh_token=; Path=/; HttpOnly; Max-Age=0" +
        (cookieSameSite != null && !cookieSameSite.isBlank() ? "; SameSite=" + cookieSameSite : "") +
        (cookieSecure ? "; Secure" : "");
    res.addHeader("Set-Cookie", clear);
        return ResponseEntity.ok(java.util.Map.of("message", "Logged out"));
    }

    // Graceful GET support for browser navigations: clear cookie and redirect to login
    @GetMapping("/logout")
    public void logoutGet(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                          @RequestParam(name = "returnTo", defaultValue = "/login") String returnTo,
                          HttpServletResponse res) throws java.io.IOException {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        String clear = "refresh_token=; Path=/; HttpOnly; Max-Age=0" +
            (cookieSameSite != null && !cookieSameSite.isBlank() ? "; SameSite=" + cookieSameSite : "") +
            (cookieSecure ? "; Secure" : "");
        res.addHeader("Set-Cookie", clear);
        res.setStatus(303);
        res.setHeader("Location", frontendBase + returnTo);
    }

    // --- Dev-only helpers to align password hashes quickly ---
    @PostMapping("/dev/hash")
    public ResponseEntity<?> devHash(@RequestBody java.util.Map<String, String> body) {
        if (!devEndpoints) return ResponseEntity.status(404).build();
        if (body == null) return ResponseEntity.badRequest().body(java.util.Map.of("message","JSON body required"));
        String pwd = body.get("password");
        if (pwd == null || pwd.isBlank()) return ResponseEntity.badRequest().body(java.util.Map.of("message","password required"));
        return ResponseEntity.ok(java.util.Map.of("hash", authService.devHash(pwd)));
    }

    @PostMapping("/dev/reset")
    public ResponseEntity<?> devReset(@RequestBody java.util.Map<String, String> body) {
        if (!devEndpoints) return ResponseEntity.status(404).build();
        if (body == null) return ResponseEntity.badRequest().body(java.util.Map.of("message","JSON body required"));
        String email = body.get("email");
        String pwd = body.get("password");
        if (email == null || pwd == null) return ResponseEntity.badRequest().body(java.util.Map.of("message","email and password required"));
        int updated = authService.devResetPassword(email, pwd);
        return ResponseEntity.ok(java.util.Map.of("updated", updated));
    }

    @PostMapping("/dev/verify")
    public ResponseEntity<?> devVerify(@RequestBody java.util.Map<String, String> body) {
        if (!devEndpoints) return ResponseEntity.status(404).build();
        if (body == null) return ResponseEntity.badRequest().body(java.util.Map.of("message","JSON body required"));
        String email = body.get("email");
        String pwd = body.get("password");
        if (email == null || pwd == null) return ResponseEntity.badRequest().body(java.util.Map.of("message","email and password required"));
        var result = authService.devVerify(email, pwd);
        return ResponseEntity.ok(result);
    }

    private void setRefreshCookie(HttpServletResponse res, String value, int ttlSeconds) {
        // Use header to attach SameSite / Secure attributes for cross-site auth
        StringBuilder sb = new StringBuilder();
        sb.append("refresh_token=").append(value)
          .append("; Path=/; HttpOnly; Max-Age=").append(ttlSeconds);
        if (cookieSameSite != null && !cookieSameSite.isBlank()) {
            sb.append("; SameSite=").append(cookieSameSite);
        }
        if (cookieSecure) {
            sb.append("; Secure");
        }
        res.addHeader("Set-Cookie", sb.toString());
    }

    // Using simple Map bodies above; nested records not required.

    // --- Profile Endpoints ---
    @GetMapping("/me")
    public ResponseEntity<?> me(@org.springframework.web.bind.annotation.RequestHeader(name="Authorization", required = false) String authHeader) {
        try {
            var parsed = authService.parseToken(authHeader);
            try {
                var profile = authService.getProfile(parsed.userId());
                return ResponseEntity.ok(profile);
            } catch (BadSqlGrammarException e) {
                // Schema/table not available; fall back to token-derived minimal profile
                return ResponseEntity.ok(java.util.Map.of(
                    "userId", parsed.userId(),
                    "email", null,
                    "name", null,
                    "role", parsed.role(),
                    "source", "token"
                ));
            } catch (EmptyResultDataAccessException e) {
                return ResponseEntity.status(404).body(java.util.Map.of("message", "User not found"));
            }
        } catch (AuthService.Unauthorized ex) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", ex.getMessage()));
        } catch (DataAccessException ex) {
            // Unexpected data access error; avoid leaking stack as 500 HTML
            return ResponseEntity.status(502).body(java.util.Map.of("message", "Profile store unavailable"));
        }
    }

    public record UpdateProfileRequest(String name, String email) {}
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
        @org.springframework.web.bind.annotation.RequestHeader(name="Authorization", required = false) String authHeader,
        @RequestBody UpdateProfileRequest req) {
        try {
            var parsed = authService.parseToken(authHeader);
            try {
                var result = authService.updateProfile(parsed.userId(), req != null ? req.name() : null, req != null ? req.email() : null);
                return ResponseEntity.ok(result);
            } catch (BadSqlGrammarException ex) {
                // Profile persistence not available in this deployment
                return ResponseEntity.status(501).body(java.util.Map.of("message", "Profile update not supported by backend"));
            }
        } catch (AuthService.Unauthorized ex) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", ex.getMessage()));
        } catch (DataAccessException ex) {
            return ResponseEntity.status(502).body(java.util.Map.of("message", "Profile store unavailable"));
        }
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword) {}
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
        @org.springframework.web.bind.annotation.RequestHeader(name="Authorization", required = false) String authHeader,
        @RequestBody ChangePasswordRequest req) {
        try {
            var parsed = authService.parseToken(authHeader);
            var result = authService.changePassword(parsed.userId(), req != null ? req.currentPassword() : null, req != null ? req.newPassword() : null);
            return ResponseEntity.ok(result);
        } catch (AuthService.Unauthorized ex) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", ex.getMessage()));
        }
    }
}

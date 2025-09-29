package auth;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final JdbcTemplate jdbc;
    private final Argon2PasswordEncoder argon2 = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Value("${jwt.secret:change-me}")
    private String jwtSecret;
    @Value("${jwt.issuer:tariff}")
    private String jwtIssuer;
    @Value("${jwt.audience:tariff-web}")
    private String jwtAudience;
    @Value("${jwt.accessTtlSeconds:900}")
    private int accessTtlSeconds;
    @Value("${sessions.refreshTtlSeconds:604800}")
    private int refreshTtlSeconds;
    @Value("${app.auth.allowPlaintext:false}")
    private boolean allowPlaintext;

    public AuthService(@org.springframework.beans.factory.annotation.Qualifier("authJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ResponseEntity<?> signup(SignupRequest req) {
        if (req.name() == null || req.email() == null || req.country() == null || req.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Missing required fields"));
        }

        Integer exists = jdbc.queryForObject("SELECT COUNT(1) FROM users WHERE LOWER(email) = LOWER(?)", Integer.class, req.email());
        if (exists != null && exists > 0) {
            return ResponseEntity.status(409).body(Map.of("message", "Email already registered"));
        }
        String hash = argon2.encode(req.password());
        String userId = UUID.randomUUID().toString();

    boolean insertedUser = false;
        // Try default expected schema
        try {
            jdbc.update("INSERT INTO users (id, email, name, country_code, role) VALUES (?, ?, ?, ?, ?)",
                    userId, req.email(), req.name(), req.country(), "user");
            insertedUser = true;
        } catch (BadSqlGrammarException e) {
            // Try variants
            try {
                // Without country_code
                jdbc.update("INSERT INTO users (id, email, name, role) VALUES (?, ?, ?, ?)",
                        userId, req.email(), req.name(), "user");
                insertedUser = true;
            } catch (BadSqlGrammarException e2) {
                try {
                    // Minimal columns
                    jdbc.update("INSERT INTO users (id, email) VALUES (?, ?)", userId, req.email());
                    insertedUser = true;
                } catch (BadSqlGrammarException e3) {
                    try {
                        // Some schemas store password on users.hashed_password
                        jdbc.update("INSERT INTO users (id, email, name, role, hashed_password) VALUES (?, ?, ?, ?, ?)",
                                userId, req.email(), req.name(), "user", hash);
                        insertedUser = true;
                    } catch (BadSqlGrammarException e4) {
                        try {
                            // Or users.password_hash
                            jdbc.update("INSERT INTO users (id, email, name, role, password_hash) VALUES (?, ?, ?, ?, ?)",
                                    userId, req.email(), req.name(), "user", hash);
                            insertedUser = true;
                        } catch (BadSqlGrammarException e5) {
                            log.warn("Signup: none of the users table variants matched the schema");
                        }
                    }
                }
            }
        }

        if (!insertedUser) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Signup failed"));
        }

        // After inserting basic user row, attempt to persist password hash on users table if columns exist
        // Try common variants in order and ignore grammar errors if column doesn't exist
        try {
            jdbc.update("UPDATE users SET hashed_password = ? WHERE id = ?", hash, userId);
        } catch (BadSqlGrammarException ignore) {
            try {
                jdbc.update("UPDATE users SET password_hash = ? WHERE id = ?", hash, userId);
            } catch (BadSqlGrammarException ignore2) {
                try {
                    // NextAuth/Prisma default column name
                    jdbc.update("UPDATE users SET hashedPassword = ? WHERE id = ?", hash, userId);
                } catch (BadSqlGrammarException ignore3) {
                    log.debug("Signup: users password hash column not found (tried hashed_password, password_hash, hashedPassword)");
                }
            }
        }

        // Try inserting into accounts table for credentials provider if present
        try {
            jdbc.update("INSERT INTO accounts (id, user_id, provider, provider_account_id, password_hash, password_algorithm) VALUES (UUID(), ?, 'credentials', ?, ?, 'argon2id')",
                    userId, req.email(), hash);
        } catch (BadSqlGrammarException e) {
            // accounts table may not exist in this schema; ignore
            log.debug("Signup: skipping accounts insert (table/columns not present)");
        }

        return ResponseEntity.ok(Map.of("message", "Signup successful"));
    }

    public LoginResult login(LoginRequest req) {
        if (req.email() == null || req.password() == null) {
            throw new Unauthorized("Missing credentials");
        }
        final String inputEmail = req.email().trim();
        try {
            CredRow row = fetchCredentials(inputEmail);
            String passwordHash = row.passwordHash();
            String algorithm = row.algorithm();
            if (passwordHash == null || passwordHash.isBlank()) {
                log.info("Login failed for email {}: empty password hash", inputEmail);
                throw new Unauthorized("Invalid email or password");
            }

            String algo = algorithm != null ? algorithm.toLowerCase() : null;
            try {
                if ((algo != null && algo.contains("argon2")) || passwordHash.startsWith("$argon2")) {
                    boolean ok = argon2.matches(req.password(), passwordHash);
                    if (!ok) {
                        log.info("Login credential mismatch for {} using argon2", inputEmail);
                        throw new Unauthorized("Invalid email or password");
                    }
                } else if ((algo != null && algo.contains("bcrypt")) || passwordHash.startsWith("$2a$") || passwordHash.startsWith("$2b$") || passwordHash.startsWith("$2y$")) {
                    boolean ok = bcrypt.matches(req.password(), passwordHash);
                    if (!ok) {
                        log.info("Login credential mismatch for {} using bcrypt", inputEmail);
                        throw new Unauthorized("Invalid email or password");
                    }
                } else {
                    // Unknown algorithm — try both common verifiers, then optionally allow plaintext in dev
                    log.warn("Unknown password algorithm for user email {}: algorithm={}, hashPrefix={}, length={}",
                            inputEmail, algorithm, passwordHash.substring(0, Math.min(10, passwordHash.length())), passwordHash.length());
                    boolean ok = false;
                    try { ok = argon2.matches(req.password(), passwordHash); } catch (IllegalArgumentException ignore) {}
                    if (!ok) {
                        try { ok = bcrypt.matches(req.password(), passwordHash); } catch (IllegalArgumentException ignore) {}
                    }
                    if (!ok && allowPlaintext) {
                        ok = req.password().equals(passwordHash);
                        if (ok) {
                            log.error("SECURITY WARNING: accepting PLAINTEXT password match for {} due to app.auth.allowPlaintext=true", inputEmail);
                        }
                    }
                    if (!ok) {
                        log.info("Login credential mismatch for {} using unknown algorithm fallback(s)", inputEmail);
                        throw new Unauthorized("Invalid email or password");
                    }
                }
            } catch (IllegalArgumentException e) {
                // Happens if hash is truncated or malformed
                log.warn("Password hash parsing error for email {}: {}", inputEmail, e.toString());
                throw new Unauthorized("Invalid email or password");
            }
            String userId = row.userId();
            String role = row.role() != null ? row.role() : "user";
            return issueTokens(userId, role, null);
        } catch (EmptyResultDataAccessException ex) {
            log.debug("No credentials record found for email {}", inputEmail);
            throw new Unauthorized("Invalid email or password");
        }
    }

    private CredRow fetchCredentials(String email) {
        // Variant A: users + accounts (credentials)
        try {
            var m = jdbc.queryForMap(
                "SELECT u.id as user_id, u.role as role, a.password_hash as password_hash, a.password_algorithm as password_algorithm " +
                "FROM users u JOIN accounts a ON u.id = a.user_id WHERE LOWER(u.email) = LOWER(?) AND a.provider = 'credentials'",
                email);
            log.debug("Auth: credentials via accounts table");
            return new CredRow((String)m.get("user_id"), (String)m.get("role"), (String)m.get("password_hash"), (String)m.get("password_algorithm"));
        } catch (BadSqlGrammarException | EmptyResultDataAccessException ignore) {}
        // Variant A2: accounts.hashed_password
        try {
            var m = jdbc.queryForMap(
                "SELECT u.id as user_id, u.role as role, a.hashed_password as password_hash, a.password_algorithm as password_algorithm " +
                "FROM users u JOIN accounts a ON u.id = a.user_id WHERE LOWER(u.email) = LOWER(?) AND a.provider = 'credentials'",
                email);
            log.debug("Auth: credentials via accounts.hashed_password");
            return new CredRow((String)m.get("user_id"), (String)m.get("role"), (String)m.get("password_hash"), (String)m.get("password_algorithm"));
        } catch (BadSqlGrammarException | EmptyResultDataAccessException ignore) {}
        // Variant A3: accounts.hashedPassword (camelCase) and passwordAlgorithm
        try {
            var m = jdbc.queryForMap(
                "SELECT u.id as user_id, u.role as role, a.hashedPassword as password_hash, a.passwordAlgorithm as password_algorithm " +
                "FROM users u JOIN accounts a ON u.id = a.user_id WHERE LOWER(u.email) = LOWER(?) AND a.provider = 'credentials'",
                email);
            log.debug("Auth: credentials via accounts.hashedPassword/passwordAlgorithm");
            return new CredRow((String)m.get("user_id"), (String)m.get("role"), (String)m.get("password_hash"), (String)m.get("password_algorithm"));
        } catch (BadSqlGrammarException | EmptyResultDataAccessException ignore) {}
        // Variant A4: accounts.password (plaintext or unknown)
        try {
            var m = jdbc.queryForMap(
                "SELECT u.id as user_id, u.role as role, a.password as password_hash, NULL as password_algorithm " +
                "FROM users u JOIN accounts a ON u.id = a.user_id WHERE LOWER(u.email) = LOWER(?) AND a.provider = 'credentials'",
                email);
            log.debug("Auth: credentials via accounts.password");
            return new CredRow((String)m.get("user_id"), (String)m.get("role"), (String)m.get("password_hash"), null);
        } catch (BadSqlGrammarException | EmptyResultDataAccessException ignore) {}
        // Variant B: users.hashed_password
        try {
            var m = jdbc.queryForMap(
                "SELECT u.id as user_id, u.role as role, u.hashed_password as password_hash, NULL as password_algorithm FROM users u WHERE LOWER(u.email) = LOWER(?)",
                email);
            log.debug("Auth: credentials via users.hashed_password");
            return new CredRow((String)m.get("user_id"), (String)m.get("role"), (String)m.get("password_hash"), null);
        } catch (BadSqlGrammarException | EmptyResultDataAccessException ignore) {}
        // Variant C: users.password_hash
        try {
            var m = jdbc.queryForMap(
                "SELECT u.id as user_id, u.role as role, u.password_hash as password_hash, NULL as password_algorithm FROM users u WHERE LOWER(u.email) = LOWER(?)",
                email);
            log.debug("Auth: credentials via users.password_hash");
            return new CredRow((String)m.get("user_id"), (String)m.get("role"), (String)m.get("password_hash"), null);
        } catch (BadSqlGrammarException | EmptyResultDataAccessException ignore) {}
        // Variant C2: users.hashedPassword (camelCase)
        try {
            var m = jdbc.queryForMap(
                "SELECT u.id as user_id, u.role as role, u.hashedPassword as password_hash, NULL as password_algorithm FROM users u WHERE LOWER(u.email) = LOWER(?)",
                email);
            log.debug("Auth: credentials via users.hashedPassword");
            return new CredRow((String)m.get("user_id"), (String)m.get("role"), (String)m.get("password_hash"), null);
        } catch (BadSqlGrammarException | EmptyResultDataAccessException ignore) {}
        // Variant D: users.password
        try {
            var m = jdbc.queryForMap(
                "SELECT u.id as user_id, u.role as role, u.password as password_hash, NULL as password_algorithm FROM users u WHERE LOWER(u.email) = LOWER(?)",
                email);
            log.debug("Auth: credentials via users.password");
            return new CredRow((String)m.get("user_id"), (String)m.get("role"), (String)m.get("password_hash"), null);
        } catch (BadSqlGrammarException | EmptyResultDataAccessException ignore) {}
        throw new EmptyResultDataAccessException(1);
    }

    public LoginResult refresh(String refreshToken) {
        try {
            var row = jdbc.queryForMap(
                    "SELECT s.user_id, u.role, s.expires_at FROM sessions s JOIN users u ON u.id = s.user_id WHERE s.id = ?",
                    refreshToken);
            var expiresAt = (java.sql.Timestamp) row.get("expires_at");
            if (expiresAt.toInstant().isBefore(Instant.now())) {
                jdbc.update("DELETE FROM sessions WHERE id = ?", refreshToken);
                throw new Unauthorized("Session expired");
            }
            String userId = (String) row.get("user_id");
            String role = (String) row.get("role");
            return issueTokens(userId, role, refreshToken);
        } catch (EmptyResultDataAccessException ex) {
            throw new Unauthorized("Session not found");
        }
    }

    public void logout(String refreshToken) {
        jdbc.update("DELETE FROM sessions WHERE id = ?", refreshToken);
    }

    // --- Dev helpers (guard usage at controller level) ---
    public String devHash(String password) {
        return argon2.encode(password);
    }

    public int devResetPassword(String email, String newPassword) {
        String hash = argon2.encode(newPassword);
        // Prefer accounts table when present
        try {
            int n = jdbc.update(
                "UPDATE accounts a JOIN users u ON a.user_id = u.id SET a.password_hash = ?, a.password_algorithm = 'argon2id' " +
                "WHERE a.provider = 'credentials' AND LOWER(u.email) = LOWER(?)",
                hash, email);
            if (n > 0) return n;
        } catch (BadSqlGrammarException ignore) {}
        // Fallback to users table variants
        int total = 0;
        try { total += jdbc.update("UPDATE users SET hashed_password = ? WHERE LOWER(email) = LOWER(?)", hash, email); } catch (BadSqlGrammarException ignore) {}
        try { total += jdbc.update("UPDATE users SET password_hash = ? WHERE LOWER(email) = LOWER(?)", hash, email); } catch (BadSqlGrammarException ignore) {}
        try { total += jdbc.update("UPDATE users SET hashedPassword = ? WHERE LOWER(email) = LOWER(?)", hash, email); } catch (BadSqlGrammarException ignore) {}
        return total;
    }

    public java.util.Map<String, Object> devVerify(String email, String password) {
        try {
            String e = email != null ? email.trim() : null;
            CredRow row = fetchCredentials(e);
            String hash = row.passwordHash();
            String algorithm = row.algorithm();
            String mode = "unknown";
            boolean matched = false;
            String prefix = hash != null ? hash.substring(0, Math.min(12, hash.length())) : null;
            String algo = algorithm != null ? algorithm.toLowerCase() : null;
            if (hash == null || hash.isBlank()) {
                return java.util.Map.of(
                    "matched", false,
                    "mode", mode,
                    "algorithm", algorithm,
                    "hashPrefix", prefix,
                    "hashLength", 0
                );
            }
            if ((algo != null && algo.contains("argon2")) || hash.startsWith("$argon2")) {
                mode = "argon2";
                try { matched = argon2.matches(password, hash); } catch (IllegalArgumentException ignore) {}
            } else if ((algo != null && algo.contains("bcrypt")) || hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$")) {
                mode = "bcrypt";
                try { matched = bcrypt.matches(password, hash); } catch (IllegalArgumentException ignore) {}
            } else {
                mode = "unknown";
                try { matched = argon2.matches(password, hash); } catch (IllegalArgumentException ignore) {}
                if (!matched) {
                    try { matched = bcrypt.matches(password, hash); } catch (IllegalArgumentException ignore) {}
                }
                if (!matched && allowPlaintext) {
                    matched = password.equals(hash);
                }
            }
            return java.util.Map.of(
                "matched", matched,
                "mode", mode,
                "algorithm", algorithm,
                "hashPrefix", prefix,
                "hashLength", hash.length()
            );
        } catch (org.springframework.dao.EmptyResultDataAccessException notFound) {
            return java.util.Map.of(
                "matched", false,
                "notFound", true,
                "message", "No credentials found for email"
            );
        } catch (Exception ex) {
            log.warn("devVerify unexpected error for {}: {}", email, ex.toString());
            return java.util.Map.of(
                "matched", false,
                "error", ex.getClass().getSimpleName(),
                "message", ex.getMessage()
            );
        }
    }

    private LoginResult issueTokens(String userId, String role, String oldRefreshToken) {
        // rotate refresh
        String newSessionId = UUID.randomUUID().toString();
        Instant newExp = Instant.now().plusSeconds(refreshTtlSeconds);
        if (oldRefreshToken != null) {
            jdbc.update("DELETE FROM sessions WHERE id = ?", oldRefreshToken);
        }
        jdbc.update("INSERT INTO sessions (id, user_id, expires_at) VALUES (?, ?, ?)",
                newSessionId, userId, java.sql.Timestamp.from(newExp));

        Algorithm alg = Algorithm.HMAC256(jwtSecret);
        Instant now = Instant.now();
        String accessToken = JWT.create()
                .withIssuer(jwtIssuer)
                .withAudience(jwtAudience)
                .withIssuedAt(java.util.Date.from(now))
                .withExpiresAt(java.util.Date.from(now.plusSeconds(accessTtlSeconds)))
                .withClaim("userId", userId)
                .withClaim("role", role)
                .sign(alg);
        return new LoginResult(accessToken, newSessionId, refreshTtlSeconds, role);
    }

    public record LoginResult(String accessToken, String refreshToken, int refreshTtlSeconds, String role) {}
    private record CredRow(String userId, String role, String passwordHash, String algorithm) {}

    public static class Unauthorized extends RuntimeException {
        public Unauthorized(String msg) { super(msg); }
    }
}

package auth;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

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

    @Value("${google.oauth.clientId:}")
    private String googleClientId;

    public AuthService(@Qualifier("authJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record ParsedToken(String userId, String role) {}

    public ParsedToken parseToken(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) throw new Unauthorized("Missing token");
        String token = bearerToken.startsWith("Bearer ") ? bearerToken.substring(7) : bearerToken;
        Algorithm alg = Algorithm.HMAC256(jwtSecret);
        JWTVerifier verifier = JWT.require(alg)
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .build();
        DecodedJWT jwt = verifier.verify(token);
        String userId = jwt.getClaim("userId").asString();
        String role = jwt.getClaim("role").asString();
        if (userId == null || userId.isBlank()) throw new Unauthorized("Invalid token");
        return new ParsedToken(userId, role != null ? role : "user");
    }

    /**
     * Verify a Google ID token (from Google Identity Services), upsert the user and
     * issue application tokens. Uses Google's tokeninfo endpoint via OkHttp for
     * verification to avoid an extra dependency.
     */
    public LoginResult loginWithGoogleIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) throw new Unauthorized("Missing idToken");
        // Call Google's tokeninfo endpoint
        var client = new okhttp3.OkHttpClient();
        var parsed = okhttp3.HttpUrl.parse("https://oauth2.googleapis.com/tokeninfo");
        if (parsed == null) throw new Unauthorized("Google URL error");
        var url = parsed.newBuilder().addQueryParameter("id_token", idToken).build();
        var req = new okhttp3.Request.Builder().url(url).get().build();
        try (var resp = client.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new Unauthorized("Invalid Google token");
            }
            var rb = resp.body();
            if (rb == null) throw new Unauthorized("Invalid Google response");
            var bodyStr = rb.string();
            if (bodyStr == null || bodyStr.isBlank()) throw new Unauthorized("Invalid Google response");
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            var data = (java.util.Map<String, Object>) mapper.readValue(bodyStr, java.util.Map.class);

            String aud = stringVal(data.get("aud"));
            String iss = stringVal(data.get("iss"));
            String sub = stringVal(data.get("sub"));
            String email = stringVal(data.get("email"));
            String emailVerified = stringVal(data.get("email_verified"));
            String name = stringVal(data.get("name"));

            if (sub == null || sub.isBlank()) throw new Unauthorized("Invalid Google token (no sub)");
            if (googleClientId != null && !googleClientId.isBlank()) {
                if (!googleClientId.equals(aud)) throw new Unauthorized("Google token audience mismatch");
            }
            if (iss != null && !(iss.equals("accounts.google.com") || iss.equals("https://accounts.google.com"))) {
                throw new Unauthorized("Google token issuer invalid");
            }
            if (emailVerified != null && !(emailVerified.equals("true") || emailVerified.equals("1"))) {
                // allow unverified email but warn
                log.info("Google login for sub {} with unverified email {}", sub, email);
            }

            // Find or create user
            String userId = resolveOrCreateGoogleUser(sub, email, name);
            // Determine role
            String role = "user";
            try {
                var m = jdbc.queryForMap("SELECT role FROM accounts.users WHERE id = ?", userId);
                role = (String) m.getOrDefault("role", "user");
            } catch (EmptyResultDataAccessException | org.springframework.jdbc.BadSqlGrammarException ignore) {
                // If table/column not available, default to user
            }
            return issueTokens(userId, role, null);
        } catch (java.io.IOException e) {
            throw new Unauthorized("Google verification error");
        }
    }

    private static String stringVal(Object o) {
        return o != null ? String.valueOf(o) : null;
    }

    private String resolveOrCreateGoogleUser(String sub, String email, String name) {
        // Try mapping table first
        try {
            var m = jdbc.queryForMap(
                "SELECT user_id FROM accounts.accounts WHERE provider = 'google' AND provider_account_id = ?",
                sub);
            return (String) m.get("user_id");
        } catch (EmptyResultDataAccessException | BadSqlGrammarException ignore) {}

        String userId = null;
        // If email exists, try to attach to existing user by email
        if (email != null && !email.isBlank()) {
            try {
                userId = jdbc.queryForObject(
                    "SELECT id FROM accounts.users WHERE LOWER(TRIM(email)) = LOWER(?) LIMIT 1",
                    String.class, email);
            } catch (EmptyResultDataAccessException nf) {
                userId = null;
            } catch (BadSqlGrammarException ignore) {
                userId = null;
            }
        }

        // Create user if none
        if (userId == null) {
            userId = java.util.UUID.randomUUID().toString();
            boolean inserted = false;
            try {
                jdbc.update("INSERT INTO accounts.users (id, email, name, role) VALUES (?, ?, ?, 'user')",
                    userId, email, name);
                inserted = true;
            } catch (BadSqlGrammarException e1) {
                try {
                    jdbc.update("INSERT INTO accounts.users (id, email, role) VALUES (?, ?, 'user')",
                        userId, email);
                    inserted = true;
                } catch (BadSqlGrammarException e2) {
                    try {
                        jdbc.update("INSERT INTO accounts.Users (id, email) VALUES (?, ?)", userId, email);
                        inserted = true;
                    } catch (BadSqlGrammarException e3) {
                        // give up, will throw below
                    }
                }
            }
            if (!inserted) throw new Unauthorized("Failed to create user");
        }

        // Ensure provider mapping exists
        try {
            ensureAccountsTable();
            jdbc.update(
                "INSERT INTO accounts.accounts (user_id, provider, provider_account_id) VALUES (?, 'google', ?)",
                userId, sub);
        } catch (BadSqlGrammarException ignore) {
            // If table or columns differ, ignore mapping; auth will still work via users table
        }

        return userId;
    }

    private void ensureAccountsTable() {
        try {
            ensureAccountsSchema();
            jdbc.execute("CREATE TABLE IF NOT EXISTS accounts.accounts (\n" +
                "  provider VARCHAR(32) NOT NULL,\n" +
                "  provider_account_id VARCHAR(191) NOT NULL,\n" +
                "  user_id VARCHAR(64) NOT NULL,\n" +
                "  password_hash TEXT NULL,\n" +
                "  password_algorithm VARCHAR(32) NULL,\n" +
                "  PRIMARY KEY (provider, provider_account_id)\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        } catch (BadSqlGrammarException ignore) {}
    }

    public Map<String, Object> getProfile(String userId) {
        try {
            var m = jdbc.queryForMap("SELECT id, email, name, role FROM accounts.users WHERE id = ?", userId);
            return Map.of(
                "userId", (String)m.get("id"),
                "email", (String)m.get("email"),
                "name", (String)m.getOrDefault("name", null),
                "role", (String)m.getOrDefault("role", "user")
            );
        } catch (EmptyResultDataAccessException e) {
            throw new Unauthorized("User not found");
        }
    }

    public Map<String, Object> updateProfile(String userId, String name, String email) {
        int updated = 0;
        if (email != null && !email.isBlank()) {
            // ensure uniqueness
            Integer exists = jdbc.queryForObject("SELECT COUNT(1) FROM accounts.users WHERE LOWER(email)=LOWER(?) AND id<>?", Integer.class, email, userId);
            if (exists != null && exists > 0) {
                return Map.of("updated", 0, "message", "Email already in use");
            }
            updated += jdbc.update("UPDATE accounts.users SET email = ? WHERE id = ?", email, userId);
        }
        if (name != null && !name.isBlank()) {
            try {
                updated += jdbc.update("UPDATE accounts.users SET name = ? WHERE id = ?", name, userId);
            } catch (BadSqlGrammarException ignore) {
                // name column may not exist in some schemas
            }
        }
        var profile = getProfile(userId);
        return Map.of("updated", updated, "profile", profile);
    }

    public Map<String, Object> changePassword(String userId, String currentPassword, String newPassword) {
        if (currentPassword == null || newPassword == null || newPassword.isBlank()) {
            return Map.of("updated", 0, "message", "Missing password(s)");
        }
        // Fetch hash by user id
        String passwordHash = null;
        String algorithm = null;
        try {
            var m = jdbc.queryForMap("SELECT password_hash, algorithm FROM accounts.user_passwords WHERE user_id = ?", userId);
            passwordHash = (String)m.get("password_hash");
            algorithm = (String)m.get("algorithm");
        } catch (EmptyResultDataAccessException nf) {
            // try legacy
            try {
                var m = jdbc.queryForMap("SELECT password_hash, password_algorithm FROM accounts.accounts WHERE user_id = ? AND provider='credentials'", userId);
                passwordHash = (String)m.get("password_hash");
                algorithm = (String)m.get("password_algorithm");
            } catch (EmptyResultDataAccessException nf2) {
                return Map.of("updated", 0, "message", "No existing password found");
            }
        }
        boolean matched = false;
        String algo = algorithm != null ? algorithm.toLowerCase() : null;
        if (passwordHash != null) {
            try {
                if ((algo != null && algo.contains("argon2")) || passwordHash.startsWith("$argon2")) matched = argon2.matches(currentPassword, passwordHash);
                else if ((algo != null && algo.contains("bcrypt")) || passwordHash.startsWith("$2a$") || passwordHash.startsWith("$2b$") || passwordHash.startsWith("$2y$")) matched = bcrypt.matches(currentPassword, passwordHash);
                else {
                    try { matched = argon2.matches(currentPassword, passwordHash); } catch (IllegalArgumentException ignore) {}
                    if (!matched) { try { matched = bcrypt.matches(currentPassword, passwordHash); } catch (IllegalArgumentException ignore) {} }
                }
            } catch (IllegalArgumentException ignore) { matched = false; }
        }
        if (!matched) return Map.of("updated", 0, "message", "Current password is incorrect");
        String newHash = argon2.encode(newPassword);
        // upsert
        int n = jdbc.update("UPDATE accounts.user_passwords SET password_hash = ?, algorithm='argon2id' WHERE user_id = ?", newHash, userId);
        if (n == 0) {
            jdbc.update("INSERT INTO accounts.user_passwords (user_id, password_hash, algorithm) VALUES (?, ?, 'argon2id')", userId, newHash);
        }
        return Map.of("updated", 1);
    }

    public ResponseEntity<?> signup(SignupRequest req) {
        if (req.name() == null || req.email() == null || req.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Missing required fields"));
        }
        // Check existing in accounts.users
        Integer exists = jdbc.queryForObject("SELECT COUNT(1) FROM accounts.users WHERE LOWER(email) = LOWER(?)", Integer.class, req.email());
        if (exists != null && exists > 0) {
            return ResponseEntity.status(409).body(Map.of("message", "Email already registered"));
        }
        String hash = argon2.encode(req.password());
        String userId = UUID.randomUUID().toString();
        boolean insertedUser = false;
        try {
            jdbc.update("INSERT INTO accounts.users (id, email, name, role) VALUES (?, ?, ?, ?)",
                    userId, req.email(), req.name(), "user");
            insertedUser = true;
        } catch (BadSqlGrammarException e1) {
            try {
                jdbc.update("INSERT INTO accounts.users (id, email, role) VALUES (?, ?, ?)",
                        userId, req.email(), "user");
                insertedUser = true;
            } catch (BadSqlGrammarException e2) {
                log.warn("Signup: failed to insert user into accounts.users");
            }
        }
        if (!insertedUser) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Signup failed"));
        }
        // Ensure user_passwords exists and set hash
        try {
            jdbc.execute("CREATE TABLE IF NOT EXISTS accounts.user_passwords (\n" +
                        "  user_id VARCHAR(64) NOT NULL PRIMARY KEY,\n" +
                        "  password_hash TEXT NOT NULL,\n" +
                        "  algorithm VARCHAR(32) NOT NULL\n" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            int n = jdbc.update(
                "UPDATE accounts.user_passwords SET password_hash = ?, algorithm = 'argon2id' WHERE user_id = ?",
                hash, userId);
            if (n == 0) {
                jdbc.update(
                    "INSERT INTO accounts.user_passwords (user_id, password_hash, algorithm) VALUES (?, ?, 'argon2id')",
                    userId, hash);
            }
        } catch (BadSqlGrammarException ignore) {}
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
        // Primary: accounts.users + accounts.user_passwords by email
        try {
            var m = jdbc.queryForMap(
                "SELECT u.id as user_id, u.role as role, p.password_hash as password_hash, p.algorithm as password_algorithm " +
                "FROM accounts.users u JOIN accounts.user_passwords p ON p.user_id = u.id WHERE LOWER(TRIM(u.email)) = LOWER(?)",
                email);
            log.debug("Auth: credentials via accounts.users + user_passwords");
            return new CredRow((String)m.get("user_id"), (String)m.get("role"), (String)m.get("password_hash"), (String)m.get("password_algorithm"));
        } catch (BadSqlGrammarException | EmptyResultDataAccessException ignore) {}
        // Secondary: accounts.accounts + accounts.user_passwords by provider_account_id
        try {
            var m = jdbc.queryForMap(
                "SELECT a.user_id as user_id, u.role as role, p.password_hash as password_hash, p.algorithm as password_algorithm " +
                "FROM accounts.accounts a JOIN accounts.user_passwords p ON p.user_id = a.user_id " +
                "JOIN accounts.users u ON u.id = a.user_id " +
                "WHERE LOWER(TRIM(a.provider_account_id)) = LOWER(?) AND a.provider = 'credentials'",
                email);
            log.debug("Auth: credentials via accounts.accounts + user_passwords");
            return new CredRow((String)m.get("user_id"), (String)m.get("role"), (String)m.get("password_hash"), (String)m.get("password_algorithm"));
        } catch (BadSqlGrammarException | EmptyResultDataAccessException ignore) {}
        // Legacy fallback: accounts.accounts password_hash directly
        try {
            var m = jdbc.queryForMap(
                "SELECT a.user_id as user_id, u.role as role, a.password_hash as password_hash, a.password_algorithm as password_algorithm " +
                "FROM accounts.accounts a JOIN accounts.users u ON u.id = a.user_id " +
                "WHERE LOWER(TRIM(a.provider_account_id)) = LOWER(?) AND a.provider = 'credentials'",
                email);
            log.debug("Auth: credentials via accounts.accounts (legacy)");
            return new CredRow((String)m.get("user_id"), (String)m.get("role"), (String)m.get("password_hash"), (String)m.get("password_algorithm"));
        } catch (BadSqlGrammarException | EmptyResultDataAccessException ignore) {}
        throw new EmptyResultDataAccessException(1);
    }

    public LoginResult refresh(String refreshToken) {
        try {
        var row = jdbc.queryForMap(
            "SELECT s.user_id, u.role, s.expires_at FROM accounts.sessions s JOIN accounts.users u ON u.id = s.user_id WHERE s.id = ?",
                    refreshToken);
            var expiresAt = (java.sql.Timestamp) row.get("expires_at");
            if (expiresAt.toInstant().isBefore(Instant.now())) {
                jdbc.update("DELETE FROM accounts.sessions WHERE id = ?", refreshToken);
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
    jdbc.update("DELETE FROM accounts.sessions WHERE id = ?", refreshToken);
    }

    // --- Dev helpers (guard usage at controller level) ---
    public String devHash(String password) {
        return argon2.encode(password);
    }

    public int devResetPassword(String email, String newPassword) {
        String hash = argon2.encode(newPassword);
        int total = 0;
        // Resolve user id from users table in accounts schema
        String userId = null;
        try {
            userId = jdbc.queryForObject(
                "SELECT id FROM accounts.users WHERE LOWER(TRIM(email)) = LOWER(?) LIMIT 1",
                String.class, email);
        } catch (EmptyResultDataAccessException nf) {
            userId = null;
        }
        // Try resolve via accounts mapping if not found in users
        if (userId == null) {
            try {
                userId = jdbc.queryForObject(
                    "SELECT user_id FROM accounts.accounts WHERE LOWER(TRIM(provider_account_id)) = LOWER(?) AND provider = 'credentials' LIMIT 1",
                    String.class, email);
            } catch (EmptyResultDataAccessException nf) {
                userId = null;
            } catch (BadSqlGrammarException ignore) {
                userId = null;
            }
        }
        // If still not found, create a minimal users row
        if (userId == null) {
            String newId = UUID.randomUUID().toString();
            boolean inserted = false;
            try {
                jdbc.update("INSERT INTO accounts.users (id, email) VALUES (?, ?)", newId, email);
                inserted = true;
            } catch (BadSqlGrammarException e1) {
                try {
                    // Try with name column if required by schema
                    String name = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
                    jdbc.update("INSERT INTO accounts.users (id, email, name) VALUES (?, ?, ?)", newId, email, name);
                    inserted = true;
                } catch (BadSqlGrammarException e2) {
                    try {
                        // Capitalized Users table variant
                        jdbc.update("INSERT INTO accounts.Users (id, email) VALUES (?, ?)", newId, email);
                        inserted = true;
                    } catch (BadSqlGrammarException e3) {
                        // give up creating user row
                    }
                }
            }
            if (inserted) {
                userId = newId;
                log.info("devResetPassword: created users row for {} with id {}", email, userId);
            }
        }
        if (userId == null) return 0;
        // Ensure user_passwords exists and upsert
        try {
            jdbc.execute("CREATE TABLE IF NOT EXISTS accounts.user_passwords (\n" +
                        "  user_id VARCHAR(64) NOT NULL PRIMARY KEY,\n" +
                        "  password_hash TEXT NOT NULL,\n" +
                        "  algorithm VARCHAR(32) NOT NULL\n" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            int n = jdbc.update(
                "UPDATE accounts.user_passwords SET password_hash = ?, algorithm = 'argon2id' WHERE user_id = ?",
                hash, userId);
            total += n;
            if (n == 0) {
                total += jdbc.update(
                    "INSERT INTO accounts.user_passwords (user_id, password_hash, algorithm) VALUES (?, ?, 'argon2id')",
                    userId, hash);
            }
        } catch (BadSqlGrammarException ignore) {}
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
            jdbc.update("DELETE FROM accounts.sessions WHERE id = ?", oldRefreshToken);
        }
        try {
            jdbc.update("INSERT INTO accounts.sessions (id, user_id, expires_at) VALUES (?, ?, ?)",
                    newSessionId, userId, java.sql.Timestamp.from(newExp));
        } catch (org.springframework.jdbc.BadSqlGrammarException e) {
            // Ensure sessions table exists then retry once
            ensureSessionsTable();
            jdbc.update("INSERT INTO accounts.sessions (id, user_id, expires_at) VALUES (?, ?, ?)",
                    newSessionId, userId, java.sql.Timestamp.from(newExp));
        }

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

    private void ensureSessionsTable() {
        try {
            ensureAccountsSchema();
            jdbc.execute("CREATE TABLE IF NOT EXISTS accounts.sessions (\n" +
                "  id VARCHAR(64) NOT NULL PRIMARY KEY,\n" +
                "  user_id VARCHAR(64) NOT NULL,\n" +
                "  expires_at DATETIME NOT NULL\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        } catch (org.springframework.jdbc.BadSqlGrammarException ignore) {}
    }

    private void ensureAccountsSchema() {
        try {
            jdbc.execute("CREATE SCHEMA IF NOT EXISTS accounts");
        } catch (org.springframework.jdbc.BadSqlGrammarException ignore) {}
    }

    public record LoginResult(String accessToken, String refreshToken, int refreshTtlSeconds, String role) {}
    private record CredRow(String userId, String role, String passwordHash, String algorithm) {}

    public static class Unauthorized extends RuntimeException {
        public Unauthorized(String msg) { super(msg); }
    }
}

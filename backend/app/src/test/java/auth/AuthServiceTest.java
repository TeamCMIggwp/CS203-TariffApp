package auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private EmailService emailService;

    private AuthService authService;

    private final String TEST_JWT_SECRET = "test-secret-key-for-testing-only-minimum-256-bits-required-for-hmac256";
    private final String TEST_ISSUER = "test-issuer";
    private final String TEST_AUDIENCE = "test-audience";

    @BeforeEach
    void setUp() {
        authService = new AuthService(jdbcTemplate, emailService);
        ReflectionTestUtils.setField(authService, "jwtSecret", TEST_JWT_SECRET);
        ReflectionTestUtils.setField(authService, "jwtIssuer", TEST_ISSUER);
        ReflectionTestUtils.setField(authService, "jwtAudience", TEST_AUDIENCE);
        ReflectionTestUtils.setField(authService, "accessTtlSeconds", 900);
        ReflectionTestUtils.setField(authService, "refreshTtlSeconds", 604800);
        ReflectionTestUtils.setField(authService, "allowPlaintext", false);
        ReflectionTestUtils.setField(authService, "googleClientId", "test-client-id");
    }

    // ===== Token Parsing Tests =====

    @Test
    void parseToken_withValidToken_returnsUserIdAndRole() {
        // Arrange
        Algorithm alg = Algorithm.HMAC256(TEST_JWT_SECRET);
        String token = JWT.create()
                .withIssuer(TEST_ISSUER)
                .withAudience(TEST_AUDIENCE)
                .withClaim("userId", "user-123")
                .withClaim("role", "admin")
                .withExpiresAt(java.util.Date.from(Instant.now().plusSeconds(900)))
                .sign(alg);
        String bearerToken = "Bearer " + token;

        // Act
        AuthService.ParsedToken result = authService.parseToken(bearerToken);

        // Assert
        assertThat(result.userId()).isEqualTo("user-123");
        assertThat(result.role()).isEqualTo("admin");
    }

    @Test
    void parseToken_withoutBearer_parsesSuccessfully() {
        // Arrange
        Algorithm alg = Algorithm.HMAC256(TEST_JWT_SECRET);
        String token = JWT.create()
                .withIssuer(TEST_ISSUER)
                .withAudience(TEST_AUDIENCE)
                .withClaim("userId", "user-456")
                .withClaim("role", "user")
                .withExpiresAt(java.util.Date.from(Instant.now().plusSeconds(900)))
                .sign(alg);

        // Act
        AuthService.ParsedToken result = authService.parseToken(token);

        // Assert
        assertThat(result.userId()).isEqualTo("user-456");
        assertThat(result.role()).isEqualTo("user");
    }

    @Test
    void parseToken_withNullToken_throwsUnauthorized() {
        assertThatThrownBy(() -> authService.parseToken(null))
                .isInstanceOf(AuthService.Unauthorized.class)
                .hasMessageContaining("Missing token");
    }

    @Test
    void parseToken_withBlankToken_throwsUnauthorized() {
        assertThatThrownBy(() -> authService.parseToken(""))
                .isInstanceOf(AuthService.Unauthorized.class)
                .hasMessageContaining("Missing token");
    }

    @Test
    void parseToken_withInvalidToken_throwsException() {
        assertThatThrownBy(() -> authService.parseToken("Bearer invalid.token.here"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void parseToken_withExpiredToken_throwsException() {
        // Arrange
        Algorithm alg = Algorithm.HMAC256(TEST_JWT_SECRET);
        String token = JWT.create()
                .withIssuer(TEST_ISSUER)
                .withAudience(TEST_AUDIENCE)
                .withClaim("userId", "user-123")
                .withClaim("role", "user")
                .withExpiresAt(java.util.Date.from(Instant.now().minusSeconds(3600)))
                .sign(alg);

        // Act & Assert
        assertThatThrownBy(() -> authService.parseToken("Bearer " + token))
                .isInstanceOf(Exception.class);
    }

    @Test
    void parseToken_withMissingUserId_throwsUnauthorized() {
        // Arrange
        Algorithm alg = Algorithm.HMAC256(TEST_JWT_SECRET);
        String token = JWT.create()
                .withIssuer(TEST_ISSUER)
                .withAudience(TEST_AUDIENCE)
                .withClaim("role", "user")
                .withExpiresAt(java.util.Date.from(Instant.now().plusSeconds(900)))
                .sign(alg);

        // Act & Assert
        assertThatThrownBy(() -> authService.parseToken("Bearer " + token))
                .isInstanceOf(AuthService.Unauthorized.class)
                .hasMessageContaining("Invalid token");
    }

    @Test
    void parseToken_withNoRole_defaultsToUser() {
        // Arrange
        Algorithm alg = Algorithm.HMAC256(TEST_JWT_SECRET);
        String token = JWT.create()
                .withIssuer(TEST_ISSUER)
                .withAudience(TEST_AUDIENCE)
                .withClaim("userId", "user-789")
                .withExpiresAt(java.util.Date.from(Instant.now().plusSeconds(900)))
                .sign(alg);

        // Act
        AuthService.ParsedToken result = authService.parseToken("Bearer " + token);

        // Assert
        assertThat(result.role()).isEqualTo("user");
    }

    // ===== Login Tests =====

    @Test
    void login_withNullEmail_throwsUnauthorized() {
        // Arrange
        LoginRequest request = new LoginRequest(null, "password");

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthService.Unauthorized.class)
                .hasMessageContaining("Missing credentials");
    }

    @Test
    void login_withNullPassword_throwsUnauthorized() {
        // Arrange
        LoginRequest request = new LoginRequest("user@example.com", null);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthService.Unauthorized.class)
                .hasMessageContaining("Missing credentials");
    }

    @Test
    void login_withNonExistentUser_throwsUnauthorized() {
        // Arrange
        LoginRequest request = new LoginRequest("nonexistent@example.com", "password");
        when(jdbcTemplate.queryForMap(anyString(), eq("nonexistent@example.com")))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthService.Unauthorized.class)
                .hasMessageContaining("Invalid email or password");
    }

    // ===== Refresh Token Tests =====

    @Test
    void refresh_withValidToken_returnsNewTokens() {
        // Arrange
        String refreshToken = "valid-refresh-token";
        Map<String, Object> sessionRow = Map.of(
                "user_id", "user-123",
                "role", "user",
                "expires_at", Timestamp.from(Instant.now().plusSeconds(3600))
        );

        when(jdbcTemplate.queryForMap(anyString(), eq(refreshToken)))
                .thenReturn(sessionRow);
        when(jdbcTemplate.update(anyString(), anyString()))
                .thenReturn(1);
        when(jdbcTemplate.update(anyString(), anyString(), anyString(), any(Timestamp.class)))
                .thenReturn(1);

        // Act
        AuthService.LoginResult result = authService.refresh(refreshToken);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.role()).isEqualTo("user");
    }

    @Test
    void refresh_withExpiredToken_throwsUnauthorized() {
        // Arrange
        String refreshToken = "expired-token";
        Map<String, Object> sessionRow = Map.of(
                "user_id", "user-123",
                "role", "user",
                "expires_at", Timestamp.from(Instant.now().minusSeconds(3600))
        );

        when(jdbcTemplate.queryForMap(anyString(), eq(refreshToken)))
                .thenReturn(sessionRow);

        // Act & Assert
        assertThatThrownBy(() -> authService.refresh(refreshToken))
                .isInstanceOf(AuthService.Unauthorized.class)
                .hasMessageContaining("Session expired");

        verify(jdbcTemplate).update(eq("DELETE FROM accounts.sessions WHERE id = ?"), eq(refreshToken));
    }

    @Test
    void refresh_withNonExistentToken_throwsUnauthorized() {
        // Arrange
        String refreshToken = "non-existent-token";
        when(jdbcTemplate.queryForMap(anyString(), eq(refreshToken)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act & Assert
        assertThatThrownBy(() -> authService.refresh(refreshToken))
                .isInstanceOf(AuthService.Unauthorized.class)
                .hasMessageContaining("Session not found");
    }

    // ===== Logout Tests =====

    @Test
    void logout_deletesSession() {
        // Arrange
        String refreshToken = "token-to-logout";
        when(jdbcTemplate.update(anyString(), eq(refreshToken))).thenReturn(1);

        // Act
        authService.logout(refreshToken);

        // Assert
        verify(jdbcTemplate).update(eq("DELETE FROM accounts.sessions WHERE id = ?"), eq(refreshToken));
    }

    // ===== Profile Tests =====

    @Test
    void getProfile_withValidUserId_returnsProfile() {
        // Arrange
        String userId = "user-123";
        Map<String, Object> userRow = Map.of(
                "id", userId,
                "email", "user@example.com",
                "name", "Test User",
                "role", "user"
        );

        when(jdbcTemplate.queryForMap(eq("SELECT id, email, name, role FROM accounts.users WHERE id = ?"), eq(userId)))
                .thenReturn(userRow);
        when(jdbcTemplate.queryForObject(contains("accounts.accounts"), eq(Integer.class), eq(userId)))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("user_passwords"), eq(Integer.class), eq(userId)))
                .thenReturn(1);

        // Act
        Map<String, Object> profile = authService.getProfile(userId);

        // Assert
        assertThat(profile).isNotNull();
        assertThat(profile.get("userId")).isEqualTo(userId);
        assertThat(profile.get("email")).isEqualTo("user@example.com");
        assertThat(profile.get("name")).isEqualTo("Test User");
        assertThat(profile.get("role")).isEqualTo("user");
        assertThat(profile.get("googleLinked")).isEqualTo(false);
        assertThat(profile.get("hasPassword")).isEqualTo(true);
    }

    @Test
    void getProfile_withNonExistentUser_throwsUnauthorized() {
        // Arrange
        String userId = "non-existent";
        when(jdbcTemplate.queryForMap(anyString(), eq(userId)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act & Assert
        assertThatThrownBy(() -> authService.getProfile(userId))
                .isInstanceOf(AuthService.Unauthorized.class)
                .hasMessageContaining("User not found");
    }

    // ===== Change Password Tests =====

    @Test
    void changePassword_withNullPasswords_returnsFailure() {
        // Act
        Map<String, Object> result = authService.changePassword("user-123", null, "newPassword");

        // Assert
        assertThat(result.get("updated")).isEqualTo(0);
        assertThat(result.get("message")).isEqualTo("Missing password(s)");
    }

    @Test
    void changePassword_withBlankNewPassword_returnsFailure() {
        // Act
        Map<String, Object> result = authService.changePassword("user-123", "currentPassword", "");

        // Assert
        assertThat(result.get("updated")).isEqualTo(0);
        assertThat(result.get("message")).isEqualTo("Missing password(s)");
    }

    @Test
    void changePassword_withNoExistingPassword_returnsFailure() {
        // Arrange
        when(jdbcTemplate.queryForMap(anyString(), anyString()))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act
        Map<String, Object> result = authService.changePassword("user-123", "currentPassword", "newPassword");

        // Assert
        assertThat(result.get("updated")).isEqualTo(0);
        assertThat(result.get("message")).isEqualTo("No existing password found");
    }

    // ===== Password Reset Tests =====

    @Test
    void requestPasswordReset_withNullEmail_returnsWithoutError() {
        // Act - should not throw
        authService.requestPasswordReset(null, "http://localhost:3000", null);

        // Assert - no email should be sent
        verify(emailService, never()).sendPasswordReset(anyString(), anyString());
    }

    @Test
    void requestPasswordReset_withBlankEmail_returnsWithoutError() {
        // Act - should not throw
        authService.requestPasswordReset("", "http://localhost:3000", null);

        // Assert - no email should be sent
        verify(emailService, never()).sendPasswordReset(anyString(), anyString());
    }

    @Test
    void requestPasswordReset_withNonExistentEmail_returnsWithoutError() {
        // Arrange
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), anyString()))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act - should not throw (prevents email enumeration)
        authService.requestPasswordReset("nonexistent@example.com", "http://localhost:3000", null);

        // Assert - no email should be sent
        verify(emailService, never()).sendPasswordReset(anyString(), anyString());
    }

    @Test
    void performPasswordReset_withNullToken_returnsFailure() {
        // Act
        Map<String, Object> result = authService.performPasswordReset(null, "newPassword");

        // Assert
        assertThat(result.get("updated")).isEqualTo(0);
        assertThat(result.get("message")).isEqualTo("Missing token or password");
    }

    @Test
    void performPasswordReset_withBlankToken_returnsFailure() {
        // Act
        Map<String, Object> result = authService.performPasswordReset("", "newPassword");

        // Assert
        assertThat(result.get("updated")).isEqualTo(0);
        assertThat(result.get("message")).isEqualTo("Missing token or password");
    }

    @Test
    void performPasswordReset_withNullPassword_returnsFailure() {
        // Act
        Map<String, Object> result = authService.performPasswordReset("token", null);

        // Assert
        assertThat(result.get("updated")).isEqualTo(0);
        assertThat(result.get("message")).isEqualTo("Missing token or password");
    }

    @Test
    void performPasswordReset_withInvalidToken_returnsFailure() {
        // Arrange
        when(jdbcTemplate.queryForMap(anyString(), anyString()))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act
        Map<String, Object> result = authService.performPasswordReset("invalid-token", "newPassword");

        // Assert
        assertThat(result.get("updated")).isEqualTo(0);
        assertThat(result.get("message")).isEqualTo("Invalid token");
    }

    // ===== Dev Helper Tests =====

    @Test
    void devHash_returnsArgon2Hash() {
        // Act
        String hash = authService.devHash("testPassword");

        // Assert
        assertThat(hash).isNotBlank();
        assertThat(hash).startsWith("$argon2");
    }

    @Test
    void devResetPassword_withNonExistentUser_createsUserAndSetsPassword() {
        // Arrange
        String email = "newuser@example.com";
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(email)))
                .thenThrow(new EmptyResultDataAccessException(1));
        lenient().when(jdbcTemplate.update(anyString(), anyString(), anyString()))
                .thenReturn(1);
        lenient().when(jdbcTemplate.update(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(1);
        doNothing().when(jdbcTemplate).execute(anyString());

        // Act
        int result = authService.devResetPassword(email, "newPassword");

        // Assert
        assertThat(result).isGreaterThanOrEqualTo(0);
    }

    @Test
    void devVerify_withNonExistentUser_returnsNotFound() {
        // Arrange
        when(jdbcTemplate.queryForMap(anyString(), anyString()))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act
        Map<String, Object> result = authService.devVerify("nonexistent@example.com", "password");

        // Assert
        assertThat(result.get("matched")).isEqualTo(false);
        assertThat(result.get("notFound")).isEqualTo(true);
    }

    // ===== Update Profile Tests =====

    @Test
    void updateProfile_withNewEmail_checksUniqueness() {
        // Arrange
        String userId = "user-123";
        String newEmail = "newemail@example.com";
        when(jdbcTemplate.queryForObject(contains("accounts.accounts"), eq(Integer.class), eq(userId)))
                .thenReturn(0); // Not Google-linked
        when(jdbcTemplate.queryForObject(contains("COUNT(1)"), eq(Integer.class), eq(newEmail), eq(userId)))
                .thenReturn(1); // Email already exists

        // Act
        Map<String, Object> result = authService.updateProfile(userId, null, newEmail);

        // Assert
        assertThat(result.get("updated")).isEqualTo(0);
        assertThat(result.get("message")).isEqualTo("Email already in use");
    }

    @Test
    void updateProfile_withBlankName_doesNotUpdate() {
        // Arrange
        String userId = "user-123";
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(userId)))
                .thenReturn(0);
        Map<String, Object> profileData = Map.of(
                "id", userId,
                "email", "user@example.com",
                "name", "Original Name",
                "role", "user"
        );
        when(jdbcTemplate.queryForMap(anyString(), eq(userId)))
                .thenReturn(profileData);

        // Act
        Map<String, Object> result = authService.updateProfile(userId, "", null);

        // Assert
        assertThat(result.get("updated")).isEqualTo(0);
    }
}

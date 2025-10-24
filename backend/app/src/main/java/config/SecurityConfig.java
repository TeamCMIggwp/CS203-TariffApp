package config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity // optional, enables @PreAuthorize if needed later
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtFilter;

	public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
		this.jwtFilter = jwtFilter;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// CORS is handled by web.CorsConfig; don't configure here to avoid duplicate beans
			.csrf(csrf -> csrf.disable())
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(eh -> eh
				.authenticationEntryPoint((request, response, authException) ->
					writeJson(response, 401, "Unauthorized", authException != null ? authException.getMessage() : null, request.getRequestURI()))
				.accessDeniedHandler((request, response, accessDeniedException) ->
					writeJson(response, 403, "Forbidden", accessDeniedException != null ? accessDeniedException.getMessage() : null, request.getRequestURI()))
			)
			.authorizeHttpRequests(auth -> auth
				// Health + Swagger OPEN
				.requestMatchers("/actuator/health").permitAll()
				.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
				.requestMatchers("/hello").permitAll()

				// Auth endpoints open (login/signup/refresh/logout/me handled in controller)
				.requestMatchers("/auth/**").permitAll()

				// Tariffs: GET open; mutating admin-only
				.requestMatchers(HttpMethod.GET, "/api/v1/tariffs/**").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/tariffs/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.PUT, "/api/v1/tariffs/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.DELETE, "/api/v1/tariffs/**").hasRole("ADMIN")

				// News: GET open; mutating admin-only
				.requestMatchers(HttpMethod.GET, "/api/v1/news/**").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/news/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.PUT, "/api/v1/news/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.DELETE, "/api/v1/news/**").hasRole("ADMIN")

				// WITS and WTO data open
				.requestMatchers(HttpMethod.GET, "/api/v1/wits/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/indicators/**").permitAll()

				// Exchange rates open
				.requestMatchers(HttpMethod.GET, "/api/v1/exchange").permitAll()

				// Gemini endpoints (health + analyses) open as before
				.requestMatchers(HttpMethod.GET, "/api/v1/gemini/health").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/gemini/analyses").permitAll()

				// Everything else requires authentication
				.anyRequest().authenticated()
			)
			// Add our JWT filter
			.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	private static void writeJson(HttpServletResponse res, int status, String error, String detail, String path) {
		try {
			res.setStatus(status);
			res.setCharacterEncoding(StandardCharsets.UTF_8.name());
			res.setContentType("application/json");
			String safeDetail = StringUtils.hasText(detail) ? detail.replace("\"", "\\\"") : null;
			String body = "{"
				+ "\"timestamp\":\"" + Instant.now().toString() + "\"," 
				+ "\"status\":" + status + ","
				+ "\"error\":\"" + (error != null ? error : "") + "\"," 
				+ (safeDetail != null ? "\"message\":\"" + safeDetail + "\"," : "")
				+ "\"path\":\"" + (path != null ? path : "") + "\"}";
			res.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
		} catch (Exception ignore) {
			// As a last resort, let default error handling proceed
		}
	}
}

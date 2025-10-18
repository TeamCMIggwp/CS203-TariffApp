package config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security + CORS configuration.
 * - Enables CORS within Spring Security using the CorsConfigurationSource bean
 * - Disables CSRF for /api/** (stateless REST)
 * - Permits Swagger UI and OpenAPI endpoints
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Let Security apply CORS using the CorsConfigurationSource bean
            .cors(c -> {})
            // Ignore CSRF for stateless API endpoints
            .csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/api/**")))
            // Allow Swagger docs & UI + your API (adjust if you add auth later)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .requestMatchers("/api/**").permitAll()
                .anyRequest().permitAll()
            );

        // No form login / basic auth for a pure API
        return http.build();
    }

    /**
     * CORS policy used by Spring Security. Use explicit origins in production.
     * If your Swagger UI and frontend are served from https://teamcmiggwp.duckdns.org,
     * list that origin below. Add more origins if needed.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // Explicit origins are safer than "*", especially with credentials.
        cfg.setAllowedOrigins(List.of(
            "https://teamcmiggwp.duckdns.org"
        ));

        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Origin", "Content-Type", "Accept", "Authorization"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}

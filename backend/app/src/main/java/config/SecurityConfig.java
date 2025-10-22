package config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity // enables @PreAuthorize if you want it
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable()) // stateless API; disable CSRF
            .authorizeHttpRequests(auth -> auth
                // health & swagger (lock Swagger in prod if you want)
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                // ----- Tariff Management rules -----
                // Read: allow GET for everyone
                .requestMatchers(HttpMethod.GET, "/api/v1/tariffs/**").permitAll()

                // Write: only ADMIN
                .requestMatchers(HttpMethod.POST,   "/api/v1/tariffs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/tariffs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/tariffs/**").hasRole("ADMIN")

                // if you also have the WTO read-only endpoint, keep it public:
                .requestMatchers(HttpMethod.GET, "/api/v1/indicators/**").permitAll()

                // everything else: require auth (or .denyAll() if you prefer)
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults()); // simple Basic auth

        return http.build();
    }

    // In-memory users (swap for a real user store later)
    @Bean
    UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
            User.withUsername(System.getenv().getOrDefault("ADMIN_USER", "admin"))
                .password(encoder.encode(System.getenv().getOrDefault("ADMIN_PASS", "change-me-strong")))
                .roles("ADMIN") // ROLE_ADMIN
                .build(),
            // optional read-only user (can GET only)
            User.withUsername(System.getenv().getOrDefault("VIEW_USER", "viewer"))
                .password(encoder.encode(System.getenv().getOrDefault("VIEW_PASS", "viewer-pass")))
                .roles("VIEWER")
                .build()
        );
    }

    @Bean(name = "corsConfigurationSourceSecurity") // any unique name
public CorsConfigurationSource corsConfigurationSourceSecurity() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of(
        "https://teamcmiggwp.duckdns.org",
        "http://localhost:8080",
        "http://localhost:3000"
    ));
    cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization","Content-Type"));
    cfg.setAllowCredentials(true);
    cfg.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
}
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

}


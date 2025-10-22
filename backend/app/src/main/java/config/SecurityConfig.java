package config;

import org.springframework.beans.factory.annotation.Value;
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

@Configuration
@EnableMethodSecurity // optional, for @PreAuthorize if you add it later
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // We do NOT register any CorsConfigurationSource bean here to avoid conflicts.
            // If you need CORS later, add exactly ONE CorsConfigurationSource bean in ONE place.
            .csrf(csrf -> csrf.disable()) // stateless API

            .authorizeHttpRequests(auth -> auth
                // Health + Swagger OPEN
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                // Tariffs: GET = public (read), writes = ADMIN only
                .requestMatchers(HttpMethod.GET, "/api/v1/tariffs/**").permitAll()
                .requestMatchers(HttpMethod.POST,   "/api/v1/tariffs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/tariffs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/v1/tariffs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/tariffs/**").hasRole("ADMIN")

                // Your WTO read-only endpoints stay public
                .requestMatchers(HttpMethod.GET, "/api/v1/indicators/**").permitAll()

                // Everything else requires auth
                .anyRequest().authenticated()
            )

            // Simple HTTP Basic auth (works with Swagger "Authorize")
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt is the Spring-recommended encoder
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder, @Value("${SPRING_SECURITY_USERNAME:testadmin}") String adminUser,
            @Value("${SPRING_SECURITY_PASSWORD:testpw}") String adminPass) {
        
        return new InMemoryUserDetailsManager(
            User.withUsername(adminUser)
                .password(encoder.encode(adminPass))
                .roles("ADMIN")
                .build()
        );
    }
}

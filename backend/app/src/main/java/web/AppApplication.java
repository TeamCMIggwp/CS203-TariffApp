package web; // Keep your existing package

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@SpringBootApplication(scanBasePackages = {"web", "geminianalysis", "wits", "wto", "tariffcalculator"}) // Scan all your packages
public class AppApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
    }

    /**
     * Global CORS configuration for all controllers
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Apply to all endpoints
                .allowedOriginPatterns("*") // Allow all origins for development
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allow all common HTTP methods
                .allowedHeaders("*") // Allow all headers
                .allowCredentials(true) // Allow credentials (cookies, authorization headers)
                .maxAge(3600); // Cache preflight response for 1 hour
    }

    /**
     * Alternative/Additional CORS configuration bean
     * This provides more fine-grained control if needed
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow all origins (for development)
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // Or specify specific origins for production:
        // configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "https://yourdomain.com"));

        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Allow credentials
        configuration.setAllowCredentials(true);

        // Set max age for preflight requests
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply to all paths

        return source;
    }
}
package web;

import java.util.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;
import org.springframework.web.cors.*;
import org.springframework.web.servlet.config.annotation.*;

@SpringBootApplication(scanBasePackages = {
        "web",
        "geminianalysis",
        "wits",
        "wto",
        "tariffcalculator",
        "database",
        "auth",
        "config",
        "scraper",
        "exchangerate"
})
public class AppApplication implements WebMvcConfigurer {

    private static final String[] ALLOWED_METHODS = { "GET", "POST", "PUT", "DELETE", "OPTIONS" };
    private static final String[] ALLOWED_ORIGIN_PATTERNS = { "*" };
    private static final String[] ALLOWED_HEADERS = { "*" };
    private static final long CORS_MAX_AGE_SECONDS = 3600L;

    private final ApiCallLogger apiCallLogger;

    @Autowired
    public AppApplication(ApiCallLogger apiCallLogger) {
        this.apiCallLogger = apiCallLogger;
    }

    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
    }

    /**
     * Register the API call logger interceptor
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiCallLogger);
    }

    /**
     * Global CORS configuration for all controllers
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS)
                .allowedMethods(ALLOWED_METHODS)
                .allowedHeaders(ALLOWED_HEADERS)
                .allowCredentials(true)
                .maxAge(CORS_MAX_AGE_SECONDS);
    }

    /**
     * Alternative/Additional CORS configuration bean
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(ALLOWED_ORIGIN_PATTERNS));
        configuration.setAllowedMethods(Arrays.asList(ALLOWED_METHODS));
        configuration.setAllowedHeaders(Arrays.asList(ALLOWED_HEADERS));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(CORS_MAX_AGE_SECONDS);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}

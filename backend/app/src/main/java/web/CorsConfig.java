package web;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    @Value("${app.cors.allowedOrigin:http://localhost:3000}")
    private String allowedOrigin;

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // Allow exact origin from config and common localhost patterns for dev
        config.setAllowedOriginPatterns(List.of(
            allowedOrigin,
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));
        config.setAllowCredentials(true);
        // Allow all methods/headers to avoid failing OPTIONS preflight on unexpected values
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setExposedHeaders(List.of("Set-Cookie", "Authorization", "Location"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}

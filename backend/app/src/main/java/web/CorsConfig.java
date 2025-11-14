package web;

import java.util.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.annotation.*;
import org.springframework.core.*;
import org.springframework.web.cors.*;
import org.springframework.web.filter.*;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowedOrigin:http://localhost:3000}")
    private String allowedOrigin;

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow multiple origin patterns
        config.setAllowedOriginPatterns(List.of(
                allowedOrigin,
                "http://localhost:3000",
                "http://localhost:5173",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173",
                "https://teamcmiggwp.duckdns.org",  // Legacy backend domain
                "http://teamcmiggwp.duckdns.org",  // HTTP version too
                "https://*.amplifyapp.com",        // Amplify preview/prod domains
                // New custom frontend domains
                "https://www.teamcmiggwpholidaymood.fun",
                "https://teamcmiggwpholidaymood.fun",
                "http://www.teamcmiggwpholidaymood.fun",
                "http://teamcmiggwpholidaymood.fun"
        ));

        config.setAllowCredentials(true);
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

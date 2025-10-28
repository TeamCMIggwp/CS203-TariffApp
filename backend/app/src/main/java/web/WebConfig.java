package web;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Web MVC Configuration for UTF-8 encoding support
 * Ensures all REST API responses use UTF-8 character encoding
 * This fixes character encoding issues where UTF-8 characters 
 * like •, ✓, → appear as â€¢, âœ", â†'
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    /**
     * Configure message converters to use UTF-8 encoding
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Configure String converter for UTF-8
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
        stringConverter.setDefaultCharset(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false);
        converters.add(stringConverter);
        
        // Configure Jackson JSON converter for UTF-8
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setDefaultCharset(StandardCharsets.UTF_8);
        converters.add(jsonConverter);
    }
}
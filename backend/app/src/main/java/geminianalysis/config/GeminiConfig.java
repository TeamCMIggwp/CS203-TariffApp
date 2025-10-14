package geminianalysis.config;

import geminianalysis.GeminiAnalyzer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {
    
    @Value("${google.api.key:#{environment.GOOGLE_API_KEY}}")
    private String apiKey;
    
    @Bean
    public GeminiAnalyzer geminiAnalyzer() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Gemini API key is required");
        }
        return new GeminiAnalyzer(apiKey);
    }
}
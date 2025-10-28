package scraper.util;

import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Utility class for URL operations
 */
@Component
public class UrlUtil {
    
    private final Random random = new Random();
    
    // Trusted official sources for tariff data
    private static final List<String> TRUSTED_SOURCES = Arrays.asList(
        "wto.org",
        "trade.gov",
        "usitc.gov",
        "cbp.gov",
        "worldbank.org",
        "comtrade.un.org",
        "oecd.org",
        "export.gov",
        "trade-tariff.service.gov.uk"
    );
    
    // Realistic browser user agents
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Edge/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15"
    };
    
    /**
     * Fix URL encoding issues
     */
    public String fixEncoding(String url) {
        if (url == null) {
            return null;
        }
        
        return url.replace("|", "%7C")
                  .replace(" ", "%20");
    }
    
    /**
     * Extract domain from URL
     */
    public String extractDomain(String url) {
        // First check if it's a trusted source
        for (String domain : TRUSTED_SOURCES) {
            if (url.contains(domain)) {
                return domain;
            }
        }
        
        // Otherwise extract host from URL
        try {
            return new URL(url).getHost();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Get a random realistic user agent
     */
    public String getRandomUserAgent() {
        return USER_AGENTS[random.nextInt(USER_AGENTS.length)];
    }
    
    /**
     * Check if URL is from a trusted source
     */
    public boolean isTrustedSource(String url) {
        if (url == null) {
            return false;
        }
        
        return TRUSTED_SOURCES.stream()
                .anyMatch(url::contains);
    }
}
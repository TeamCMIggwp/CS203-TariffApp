package scraper.util;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for text processing operations
 */
@Component
public class TextProcessingUtil {
    
    private static final Pattern RATE_PATTERN = Pattern.compile("(\\d+\\.?\\d*)\\s*(%|percent|per cent)");
    private static final Pattern YEAR_PATTERN = Pattern.compile(
        "(?:published|updated|effective|dated|as of|©|copyright)\\s*:?\\s*(20\\d{2})",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern GENERIC_YEAR_PATTERN = Pattern.compile("\\b(202[0-9])\\b");
    
    /**
     * Check if text contains tariff-related keywords
     */
    public boolean containsTariffKeywords(String text) {
        if (text == null) {
            return false;
        }
        
        String lower = text.toLowerCase();
        return lower.contains("tariff") ||
               lower.contains("duty rate") ||
               lower.contains("customs duty") ||
               lower.contains("import duty") ||
               (lower.contains("rate") && (lower.contains("%") || lower.contains("percent")));
    }
    
    /**
     * Extract tariff rate percentage from text
     */
    public Double extractRate(String text) {
        if (text == null) {
            return null;
        }
        
        Matcher matcher = RATE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Extract year from document content
     */
    public String extractYearFromText(String text) {
        if (text == null) {
            return null;
        }
        
        // Look for contextual year patterns first
        Matcher matcher = YEAR_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Fallback: find any recent year
        matcher = GENERIC_YEAR_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Clean and normalize text
     */
    public String cleanText(String text) {
        if (text == null) {
            return "";
        }
        
        return text.replaceAll("\\s+", " ").trim();
    }
}
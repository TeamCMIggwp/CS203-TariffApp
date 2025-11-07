package database.news.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utility class for normalizing URLs to ensure consistent storage and retrieval.
 *
 * Normalization includes:
 * - Converting to lowercase (for scheme and host)
 * - Removing trailing slashes
 * - Removing default ports (80 for http, 443 for https)
 * - Removing fragments (#)
 * - Trimming whitespace
 */
public class UrlNormalizer {

    private static final Logger logger = LoggerFactory.getLogger(UrlNormalizer.class);

    /**
     * Normalize a URL string for consistent storage and comparison.
     *
     * @param url The URL to normalize
     * @return The normalized URL, or the original if normalization fails
     */
    public static String normalize(String url) {
        if (url == null || url.trim().isEmpty()) {
            return url;
        }

        try {
            // Trim whitespace
            String trimmedUrl = url.trim();

            // Parse the URL
            URI uri = new URI(trimmedUrl);

            // Extract components
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();
            String query = uri.getQuery();

            // Normalize scheme and host to lowercase
            if (scheme != null) {
                scheme = scheme.toLowerCase();
            }
            if (host != null) {
                host = host.toLowerCase();
            }

            // Remove default ports
            if (("http".equals(scheme) && port == 80) ||
                ("https".equals(scheme) && port == 443)) {
                port = -1;
            }

            // Remove trailing slash from path (unless it's the root path)
            if (path != null && path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            // Rebuild URI without fragment
            StringBuilder normalized = new StringBuilder();

            if (scheme != null) {
                normalized.append(scheme).append("://");
            }

            if (host != null) {
                normalized.append(host);
            }

            if (port != -1) {
                normalized.append(":").append(port);
            }

            if (path != null && !path.isEmpty()) {
                normalized.append(path);
            }

            if (query != null && !query.isEmpty()) {
                normalized.append("?").append(query);
            }

            String result = normalized.toString();

            if (!result.equals(trimmedUrl)) {
                logger.debug("Normalized URL from '{}' to '{}'", trimmedUrl, result);
            }

            return result;

        } catch (URISyntaxException e) {
            logger.warn("Failed to normalize URL '{}': {}. Returning trimmed original.", url, e.getMessage());
            return url.trim();
        } catch (Exception e) {
            logger.error("Unexpected error normalizing URL '{}': {}. Returning trimmed original.", url, e.getMessage());
            return url.trim();
        }
    }

    /**
     * Check if two URLs are equivalent after normalization.
     *
     * @param url1 First URL
     * @param url2 Second URL
     * @return true if the URLs are equivalent after normalization
     */
    public static boolean areEquivalent(String url1, String url2) {
        if (url1 == null && url2 == null) {
            return true;
        }
        if (url1 == null || url2 == null) {
            return false;
        }

        String normalized1 = normalize(url1);
        String normalized2 = normalize(url2);

        return normalized1.equals(normalized2);
    }
}

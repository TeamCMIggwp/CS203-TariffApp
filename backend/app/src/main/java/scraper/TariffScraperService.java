package scraper;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TariffScraperService {

    private static final Logger log = LoggerFactory.getLogger(TariffScraperService.class);
    private final WebClient webClient;

    @Value("${google.search.api.key:}")
    private String googleApiKey;

    @Value("${google.search.engine.id:}")
    private String searchEngineId;

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
            "trade-tariff.service.gov.uk");

    // Realistic browser user agents
    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Edge/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15"
    };

    private Random random = new Random();

    public TariffScraperService() {
        this.webClient = WebClient.builder().build();
    }

    /**
     * Get a random realistic user agent
     */
    private String getRandomUserAgent() {
        return USER_AGENTS[random.nextInt(USER_AGENTS.length)];
    }

    /**
     * Main method: Search Google and scrape results asynchronously over time
     */
    @Async
    public CompletableFuture<ScrapeResult> searchAndScrape(String query, int maxResults) {
        log.info("Starting search and scrape for: {}", query);

        ScrapeResult result = new ScrapeResult(query, new Date());

        try {
            // Step 1: Google Custom Search
            List<SearchHit> searchResults = performGoogleSearch(query, maxResults);
            result.setTotalSourcesFound(searchResults.size());
            log.info("Found {} sources from Google search", searchResults.size());

            // Step 2: Scrape each source with delays
            List<ScrapedData> scrapedData = new ArrayList<>();
            for (int i = 0; i < searchResults.size(); i++) {
                SearchHit hit = searchResults.get(i);

                log.info("Scraping {}/{}: {}", i + 1, searchResults.size(), hit.url());

                try {
                    ScrapedData data = scrapeURL(hit.url(), hit.title());
                    if (data != null) {
                        scrapedData.add(data);
                        result.setSourcesScraped(result.getSourcesScraped() + 1);
                    }

                    // Delay between requests to be respectful
                    if (i < searchResults.size() - 1) {
                        Thread.sleep(2000); // 2 seconds between scrapes
                    }

                } catch (Exception e) {
                    log.warn("Failed to scrape {}: {}", hit.url(), e.getMessage());
                    result.addError(hit.url(), e.getMessage());
                }
            }

            result.setData(scrapedData);
            result.setCompleted(true);
            log.info("Scraping completed. Successfully scraped {}/{} sources",
                    result.getSourcesScraped(), result.getTotalSourcesFound());

        } catch (Exception e) {
            log.error("Error in search and scrape: {}", e.getMessage(), e);
            result.setCompleted(true);
            result.addError("GENERAL", e.getMessage());
        }

        return CompletableFuture.completedFuture(result);
    }

    /**
     * Perform Google Custom Search with multiple query variations
     */
    private List<SearchHit> performGoogleSearch(String query, int maxResults) {
        List<SearchHit> results = new ArrayList<>();

        if (googleApiKey == null || googleApiKey.isEmpty() ||
                searchEngineId == null || searchEngineId.isEmpty()) {
            log.warn("Google Search API not configured. Skipping search.");
            return results;
        }

        try {
            // Multiple search variations to get more results
            String[] searchQueries = {
                    query + " tariff rate",
                    query + " import duty",
                    query + " customs tariff",
                    query + " trade tariff"
            };

            Set<String> seenUrls = new HashSet<>();

            for (String searchQuery : searchQueries) {
                if (results.size() >= maxResults)
                    break;

                log.info("Searching for: {}", searchQuery);
                List<SearchHit> batch = executeGoogleSearch(searchQuery, maxResults - results.size());

                // Deduplicate
                for (SearchHit hit : batch) {
                    if (!seenUrls.contains(hit.url()) && results.size() < maxResults) {
                        results.add(hit);
                        seenUrls.add(hit.url());
                    }
                }
            }

            log.info("Search returned {} total results", results.size());

        } catch (Exception e) {
            log.error("Google search failed: {}", e.getMessage());
        }

        return results;
    }

    /**
     * Execute actual Google search API call
     */
    private List<SearchHit> executeGoogleSearch(String query, int maxResults) {
        try {
            String url = String.format(
                    "https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&num=%d",
                    googleApiKey,
                    searchEngineId,
                    URLEncoder.encode(query, StandardCharsets.UTF_8),
                    Math.min(maxResults, 10));

            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseSearchResults(response);

        } catch (Exception e) {
            log.error("Search API call failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Parse Google Search JSON response
     */
    /**
     * Parse Google Search JSON response using Jackson (proper JSON parsing)
     */
    private List<SearchHit> parseSearchResults(String json) {
        List<SearchHit> results = new ArrayList<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            // Check for items
            if (!root.has("items")) {
                log.warn("No 'items' field found in Google search response");

                // Check for errors
                if (root.has("error")) {
                    log.error("Google API error: {}", root.get("error"));
                }

                return results;
            }

            JsonNode items = root.get("items");
            log.info("Found {} items in search response", items.size());

            for (JsonNode item : items) {
                String url = item.has("link") ? item.get("link").asText() : null;
                String title = item.has("title") ? item.get("title").asText() : null;

                if (url != null && title != null) {
                    results.add(new SearchHit(url, title));
                    log.debug("Parsed result: {} - {}", title, url);
                }
            }

            log.info("Successfully parsed {} search results", results.size());

        } catch (Exception e) {
            log.error("Failed to parse search results: {}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * Smart URL scraper - detects file type and uses appropriate method
     */
    private ScrapedData scrapeURL(String url, String title) {
        try {
            // Fix URL encoding issues
            url = fixURLEncoding(url);

            String lowerUrl = url.toLowerCase();

            // Determine file type and scrape accordingly
            if (lowerUrl.endsWith(".pdf")) {
                return scrapePDF(url, title);
            } else if (lowerUrl.endsWith(".doc") || lowerUrl.endsWith(".docx")) {
                return scrapeWordDocument(url, title);
            } else {
                return scrapeHTML(url, title);
            }

        } catch (Exception e) {
            log.warn("Failed to scrape {}: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Fix URL encoding issues
     */
    private String fixURLEncoding(String url) {
        // Fix pipe characters and other special chars
        url = url.replace("|", "%7C");
        url = url.replace(" ", "%20");
        return url;
    }

    /**
     * Scrape PDF documents
     */
    private ScrapedData scrapePDF(String url, String title) {
        try {
            log.info("Scraping PDF: {}", url);

            // Download PDF
            InputStream inputStream = new URL(url).openStream();
            PDDocument document = PDDocument.load(inputStream);

            // Extract text
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            document.close();
            inputStream.close();

            log.info("Extracted {} characters from PDF", text.length());

            // Process text same way as HTML
            return processExtractedText(url, title, text);

        } catch (Exception e) {
            log.warn("Failed to scrape PDF {}: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Scrape Word documents (.doc and .docx)
     */
    private ScrapedData scrapeWordDocument(String url, String title) {
        try {
            log.info("Scraping Word document: {}", url);

            InputStream inputStream = new URL(url).openStream();
            String text;

            if (url.toLowerCase().endsWith(".docx")) {
                // Handle .docx (newer format)
                XWPFDocument document = new XWPFDocument(inputStream);
                XWPFWordExtractor extractor = new XWPFWordExtractor(document);
                text = extractor.getText();
                extractor.close();
            } else {
                // Handle .doc (older format)
                HWPFDocument document = new HWPFDocument(inputStream);
                WordExtractor extractor = new WordExtractor(document);
                text = extractor.getText();
                extractor.close();
            }

            inputStream.close();

            log.info("Extracted {} characters from Word document", text.length());

            // Process text
            return processExtractedText(url, title, text);

        } catch (Exception e) {
            log.warn("Failed to scrape Word document {}: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Scrape HTML websites with improved anti-blocking
     */
    private ScrapedData scrapeHTML(String url, String title) {
        try {
            // Use random user agent and realistic browser headers
            Document doc = Jsoup.connect(url)
                    .timeout(15000)
                    .userAgent(getRandomUserAgent())
                    .referrer("https://www.google.com/")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .followRedirects(true)
                    .get();

            ScrapedData data = new ScrapedData(url, title);

            // Extract tariff-related content
            List<String> relevantText = new ArrayList<>();
            Double extractedRate = null;

            // Look for tables with tariff data
            Elements tables = doc.select("table");
            for (Element table : tables) {
                String tableText = table.text().toLowerCase();
                if (containsTariffKeywords(tableText)) {
                    relevantText.add(cleanText(table.text()));
                    if (extractedRate == null) {
                        extractedRate = extractRate(table.text());
                    }
                }
            }

            // Look for paragraphs with tariff info
            Elements paragraphs = doc.select("p, div.content, article, section");
            for (Element p : paragraphs) {
                String text = p.text();
                if (containsTariffKeywords(text) && text.length() > 50) {
                    relevantText.add(cleanText(text));
                    if (extractedRate == null) {
                        extractedRate = extractRate(text);
                    }
                }
            }

            if (!relevantText.isEmpty() || extractedRate != null) {
                data.setRelevantText(relevantText);
                data.setExtractedRate(extractedRate);
                data.setSourceDomain(extractDomain(url));

                // Try to find publish date
                String date = extractDate(doc);
                data.setPublishDate(date);

                return data;
            }

        } catch (Exception e) {
            log.warn("Failed to scrape HTML {}: {}", url, e.getMessage());
        }

        return null;
    }

    /**
     * Process extracted text from PDFs or Word documents
     */
    private ScrapedData processExtractedText(String url, String title, String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        ScrapedData data = new ScrapedData(url, title);

        // Split text into chunks for processing
        String[] paragraphs = text.split("\n\n");
        List<String> relevantText = new ArrayList<>();
        Double extractedRate = null;

        for (String paragraph : paragraphs) {
            if (containsTariffKeywords(paragraph) && paragraph.length() > 50) {
                relevantText.add(cleanText(paragraph));
                if (extractedRate == null) {
                    extractedRate = extractRate(paragraph);
                }
            }
        }

        if (!relevantText.isEmpty() || extractedRate != null) {
            data.setRelevantText(relevantText);
            data.setExtractedRate(extractedRate);
            data.setSourceDomain(extractDomain(url));
            return data;
        }

        return null;
    }

    /**
     * Check if text contains tariff keywords
     */
    private boolean containsTariffKeywords(String text) {
        String lower = text.toLowerCase();
        return lower.contains("tariff") ||
                lower.contains("duty rate") ||
                lower.contains("customs duty") ||
                lower.contains("import duty") ||
                (lower.contains("rate") && (lower.contains("%") || lower.contains("percent")));
    }

    /**
     * Extract tariff rate from text
     */
    private Double extractRate(String text) {
        Pattern pattern = Pattern.compile("(\\d+\\.?\\d*)\\s*(%|percent|per cent)");
        Matcher matcher = pattern.matcher(text);

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
     * Extract domain from URL
     */
    private String extractDomain(String url) {
        for (String domain : TRUSTED_SOURCES) {
            if (url.contains(domain)) {
                return domain;
            }
        }
        try {
            return new URL(url).getHost();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Extract publish date from document
     */
    private String extractDate(Document doc) {
        String[] selectors = { "time", ".publish-date", ".date", "[datetime]",
                "meta[property='article:published_time']" };

        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                String date = elements.first().text();
                if (!date.isEmpty()) {
                    return date;
                }
                String datetime = elements.first().attr("datetime");
                if (!datetime.isEmpty()) {
                    return datetime;
                }
                String content = elements.first().attr("content");
                if (!content.isEmpty()) {
                    return content;
                }
            }
        }

        return null;
    }

    /**
     * Clean extracted text
     */
    private String cleanText(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    // Data classes
    private record SearchHit(String url, String title) {
    }

    public static class ScrapedData {
        private String url;
        private String title;
        private String sourceDomain;
        private List<String> relevantText;
        private Double extractedRate;
        private String publishDate;

        public ScrapedData(String url, String title) {
            this.url = url;
            this.title = title;
            this.relevantText = new ArrayList<>();
        }

        // Getters and setters
        public String getUrl() {
            return url;
        }

        public String getTitle() {
            return title;
        }

        public String getSourceDomain() {
            return sourceDomain;
        }

        public void setSourceDomain(String sourceDomain) {
            this.sourceDomain = sourceDomain;
        }

        public List<String> getRelevantText() {
            return relevantText;
        }

        public void setRelevantText(List<String> relevantText) {
            this.relevantText = relevantText;
        }

        public Double getExtractedRate() {
            return extractedRate;
        }

        public void setExtractedRate(Double extractedRate) {
            this.extractedRate = extractedRate;
        }

        public String getPublishDate() {
            return publishDate;
        }

        public void setPublishDate(String publishDate) {
            this.publishDate = publishDate;
        }
    }

    public static class ScrapeResult {
        private String query;
        private Date startTime;
        private Date endTime;
        private boolean completed;
        private int totalSourcesFound;
        private int sourcesScraped;
        private List<ScrapedData> data;
        private Map<String, String> errors;

        public ScrapeResult(String query, Date startTime) {
            this.query = query;
            this.startTime = startTime;
            this.completed = false;
            this.sourcesScraped = 0;
            this.data = new ArrayList<>();
            this.errors = new HashMap<>();
        }

        public void addError(String url, String error) {
            this.errors.put(url, error);
        }

        // Getters and setters
        public String getQuery() {
            return query;
        }

        public Date getStartTime() {
            return startTime;
        }

        public Date getEndTime() {
            return endTime;
        }

        public void setEndTime(Date endTime) {
            this.endTime = endTime;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
            if (completed)
                this.endTime = new Date();
        }

        public int getTotalSourcesFound() {
            return totalSourcesFound;
        }

        public void setTotalSourcesFound(int totalSourcesFound) {
            this.totalSourcesFound = totalSourcesFound;
        }

        public int getSourcesScraped() {
            return sourcesScraped;
        }

        public void setSourcesScraped(int sourcesScraped) {
            this.sourcesScraped = sourcesScraped;
        }

        public List<ScrapedData> getData() {
            return data;
        }

        public void setData(List<ScrapedData> data) {
            this.data = data;
        }

        public Map<String, String> getErrors() {
            return errors;
        }
    }
}
package scraper.service;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import scraper.exception.ScrapingFailedException;
import scraper.model.ScrapedData;
import scraper.util.TextProcessingUtil;
import scraper.util.UrlUtil;

import java.io.InputStream;
import java.net.URL;

/**
 * Service responsible for scraping content from URLs
 * Handles different document types: HTML, PDF, Word documents
 * Note: Rate extraction removed - now handled by Gemini AI on frontend
 */
@Service
public class ContentScraperService {
    
    private static final Logger log = LoggerFactory.getLogger(ContentScraperService.class);
    
    @Autowired
    private TextProcessingUtil textProcessor;
    
    @Autowired
    private UrlUtil urlUtil;
    
    /**
     * Main scraping method - automatically detects document type
     */
    public ScrapedData scrape(String url, String title) {
        try {
            url = urlUtil.fixEncoding(url);
            String lowerUrl = url.toLowerCase();
            
            // Route to appropriate scraper based on file type
            if (lowerUrl.endsWith(".pdf")) {
                return scrapePdf(url, title);
            } else if (lowerUrl.endsWith(".doc") || lowerUrl.endsWith(".docx")) {
                return scrapeWordDocument(url, title);
            } else {
                return scrapeHtml(url, title);
            }
            
        } catch (Exception e) {
            log.warn("Failed to scrape {}: {}", url, e.getMessage());
            throw new ScrapingFailedException("Failed to scrape: " + url, e);
        }
    }
    
    /**
     * Scrape HTML web pages
     */
    private ScrapedData scrapeHtml(String url, String title) {
        try {
            log.debug("Scraping HTML: {}", url);
            
            Document doc = Jsoup.connect(url)
                    .timeout(15000)
                    .userAgent(urlUtil.getRandomUserAgent())
                    .referrer("https://www.google.com/")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Connection", "keep-alive")
                    .followRedirects(true)
                    .get();
            
            ScrapedData data = new ScrapedData(url, title);
            data.setSourceDomain(urlUtil.extractDomain(url));
            
            // Extract tariff-related content from tables
            extractFromTables(doc, data);
            
            // Extract from paragraphs and sections
            extractFromParagraphs(doc, data);
            
            // Try to extract publish date
            data.setPublishDate(extractPublishDate(doc));
            
            return hasRelevantContent(data) ? data : null;
            
        } catch (Exception e) {
            log.warn("Failed to scrape HTML {}: {}", url, e.getMessage());
            return null;
        }
    }
    
    /**
     * Scrape PDF documents
     */
    private ScrapedData scrapePdf(String url, String title) {
        try {
            log.debug("Scraping PDF: {}", url);
            
            try (InputStream inputStream = new URL(url).openStream();
                 PDDocument document = PDDocument.load(inputStream)) {
                
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                
                log.debug("Extracted {} characters from PDF", text.length());
                
                return processExtractedText(url, title, text);
            }
            
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
            log.debug("Scraping Word document: {}", url);
            
            String text;
            try (InputStream inputStream = new URL(url).openStream()) {
                if (url.toLowerCase().endsWith(".docx")) {
                    try (XWPFDocument document = new XWPFDocument(inputStream);
                         XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                        text = extractor.getText();
                    }
                } else {
                    try (HWPFDocument document = new HWPFDocument(inputStream);
                         WordExtractor extractor = new WordExtractor(document)) {
                        text = extractor.getText();
                    }
                }
            }
            
            log.debug("Extracted {} characters from Word document", text.length());
            
            return processExtractedText(url, title, text);
            
        } catch (Exception e) {
            log.warn("Failed to scrape Word document {}: {}", url, e.getMessage());
            return null;
        }
    }
    
    /**
     * Process plain text extracted from PDF or Word documents
     * Note: Rate extraction removed - handled by Gemini AI
     */
    private ScrapedData processExtractedText(String url, String title, String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        ScrapedData data = new ScrapedData(url, title);
        data.setSourceDomain(urlUtil.extractDomain(url));
        
        // Split into paragraphs and extract relevant content
        String[] paragraphs = text.split("\n\n");
        
        for (String paragraph : paragraphs) {
            if (textProcessor.containsTariffKeywords(paragraph) && paragraph.length() > 50) {
                data.getRelevantText().add(textProcessor.cleanText(paragraph));
            }
        }
        
        // Try to extract year from content
        String year = textProcessor.extractYearFromText(text);
        if (year != null) {
            data.setPublishDate(year);
        }
        
        return hasRelevantContent(data) ? data : null;
    }
    
    /**
     * Extract tariff data from HTML tables
     * Note: Rate extraction removed - handled by Gemini AI
     */
    private void extractFromTables(Document doc, ScrapedData data) {
        Elements tables = doc.select("table");
        
        for (Element table : tables) {
            String tableText = table.text();
            if (textProcessor.containsTariffKeywords(tableText)) {
                data.getRelevantText().add(textProcessor.cleanText(tableText));
            }
        }
    }
    
    /**
     * Extract tariff data from HTML paragraphs
     * Note: Rate extraction removed - handled by Gemini AI
     */
    private void extractFromParagraphs(Document doc, ScrapedData data) {
        Elements elements = doc.select("p, div.content, article, section");
        
        for (Element element : elements) {
            String text = element.text();
            if (textProcessor.containsTariffKeywords(text) && text.length() > 50) {
                data.getRelevantText().add(textProcessor.cleanText(text));
            }
        }
    }
    
    /**
     * Extract publish date from HTML document
     */
    private String extractPublishDate(Document doc) {
        String[] selectors = {
            "time",
            ".publish-date",
            ".date",
            "[datetime]",
            "meta[property='article:published_time']"
        };
        
        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                Element element = elements.first();
                
                // Try different attributes
                String date = element.text();
                if (!date.isEmpty()) return date;
                
                date = element.attr("datetime");
                if (!date.isEmpty()) return date;
                
                date = element.attr("content");
                if (!date.isEmpty()) return date;
            }
        }
        
        return null;
    }
    
    /**
     * Check if scraped data has relevant content
     */
    private boolean hasRelevantContent(ScrapedData data) {
        return !data.getRelevantText().isEmpty();
    }
}
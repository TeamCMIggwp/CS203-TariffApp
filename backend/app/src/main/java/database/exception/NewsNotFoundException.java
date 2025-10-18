package database.exception;

public class NewsNotFoundException extends RuntimeException {
    private String newsLink;
    
    public NewsNotFoundException(String newsLink) {
        super(String.format("News not found for link: %s", newsLink));
        this.newsLink = newsLink;
    }
    
    // Getter
    public String getNewsLink() { 
        return newsLink; 
    }
}
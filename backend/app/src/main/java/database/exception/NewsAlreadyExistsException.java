package database.exception;

public class NewsAlreadyExistsException extends RuntimeException {
    private String newsLink;
    
    public NewsAlreadyExistsException(String newsLink) {
        super(String.format("News already exists for link: %s", newsLink));
        this.newsLink = newsLink;
    }
    
    // Getter
    public String getNewsLink() { 
        return newsLink; 
    }
}
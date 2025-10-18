package database;

public class NewsEntity {

    private String newsLink;
    private String remarks;

    // Getters and setters
    
    public String getNewsLink() {
        return newsLink;
    }

    public void setNewsLink(String newsLink) {
        this.newsLink = newsLink;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    @Override
    public String toString() {
        return "NewsEntity{" +
                "newsLink='" + newsLink + '\'' +
                ", remarks='" + remarks + '\'' +
                '}';
    }
}
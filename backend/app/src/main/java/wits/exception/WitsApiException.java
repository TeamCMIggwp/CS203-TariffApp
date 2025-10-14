package wits.exception;

public class WitsApiException extends RuntimeException {
    private int statusCode;
    
    public WitsApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public WitsApiException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public int getStatusCode() { return statusCode; }
}
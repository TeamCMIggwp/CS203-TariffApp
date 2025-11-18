package common.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Extended error response for validation errors
 * Contains field-specific validation error details
 */
public class ValidationErrorResponse extends ApiErrorResponse {
    private Map<String, String> validationErrors;

    public ValidationErrorResponse(LocalDateTime timestamp, int status,
                                 String error, Map<String, String> validationErrors,
                                 String path) {
        super(timestamp, status, error, "Validation failed for one or more fields", path);
        this.validationErrors = validationErrors;
    }

    public Map<String, String> getValidationErrors() { return validationErrors; }
}

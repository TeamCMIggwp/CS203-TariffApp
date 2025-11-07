package database.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import database.news.exception.NewsAlreadyExistsException;
import database.news.exception.NewsNotFoundException;
import database.tariffs.exception.TariffAlreadyExistsException;
import database.tariffs.exception.TariffNotFoundException;
import database.exception.GlobalExceptionHandler.ErrorResponse;
import database.exception.GlobalExceptionHandler.ValidationErrorResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ---------- TariffAlreadyExistsException ----------

    @Test
    void handleTariffAlreadyExists_returns409WithErrorResponse() {
        WebRequest request = mock(WebRequest.class);
        TariffAlreadyExistsException ex =
                new TariffAlreadyExistsException("702", "156", 100630, "2020");

        when(request.getDescription(false)).thenReturn("uri=/tariffs");

        ResponseEntity<ErrorResponse> response =
                handler.handleTariffAlreadyExists(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.CONFLICT.value(), body.getStatus());
        assertEquals("Conflict", body.getError());
        assertEquals(ex.getMessage(), body.getMessage());
        assertEquals("/tariffs", body.getPath());
        assertNotNull(body.getTimestamp());
    }

    // ---------- TariffNotFoundException ----------

    @Test
    void handleTariffNotFound_returns404WithErrorResponse() {
        WebRequest request = mock(WebRequest.class);
        TariffNotFoundException ex =
                new TariffNotFoundException("702", "156", 100630, "2020");

        when(request.getDescription(false)).thenReturn("uri=/tariffs/one");

        ResponseEntity<ErrorResponse> response =
                handler.handleTariffNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.NOT_FOUND.value(), body.getStatus());
        assertEquals("Not Found", body.getError());
        assertEquals(ex.getMessage(), body.getMessage());
        assertEquals("/tariffs/one", body.getPath());
        assertNotNull(body.getTimestamp());
    }

    // ---------- NewsAlreadyExistsException ----------

    @Test
    void handleNewsAlreadyExists_returns409WithErrorResponse() {
        WebRequest request = mock(WebRequest.class);
        NewsAlreadyExistsException ex =
                new NewsAlreadyExistsException("Breaking headline");

        when(request.getDescription(false)).thenReturn("uri=/news");

        ResponseEntity<ErrorResponse> response =
                handler.handleNewsAlreadyExists(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.CONFLICT.value(), body.getStatus());
        assertEquals("Conflict", body.getError());
        assertEquals(ex.getMessage(), body.getMessage());
        assertEquals("/news", body.getPath());
    }

    // ---------- NewsNotFoundException ----------

    @Test
    void handleNewsNotFound_returns404WithErrorResponse() {
        WebRequest request = mock(WebRequest.class);
        NewsNotFoundException ex =
                new NewsNotFoundException("123");

        when(request.getDescription(false)).thenReturn("uri=/news/123");

        ResponseEntity<ErrorResponse> response =
                handler.handleNewsNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.NOT_FOUND.value(), body.getStatus());
        assertEquals("Not Found", body.getError());
        assertEquals(ex.getMessage(), body.getMessage());
        assertEquals("/news/123", body.getPath());
    }

    // ---------- MethodArgumentNotValidException (Validation) ----------

    @Test
    void handleValidationExceptions_returns400WithValidationErrorResponse() throws Exception {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/tariffs/create");

        // Build a BindingResult with field errors
        Object target = new Object();
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(target, "tariffRequest");
        bindingResult.addError(new FieldError("tariffRequest", "reporter", "must not be blank"));
        bindingResult.addError(new FieldError("tariffRequest", "year", "must be a valid year"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ValidationErrorResponse> response =
                handler.handleValidationExceptions(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ValidationErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
        assertEquals("Validation Failed", body.getError());
        assertEquals("Validation failed for one or more fields", body.getMessage());
        assertEquals("/tariffs/create", body.getPath());
        assertNotNull(body.getTimestamp());

        Map<String, String> errors = body.getValidationErrors();
        assertNotNull(errors);
        assertEquals(2, errors.size());
        assertEquals("must not be blank", errors.get("reporter"));
        assertEquals("must be a valid year", errors.get("year"));
    }

    // ---------- Generic Exception ----------

    @Test
    void handleGlobalException_returns500WithWrappedMessage() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/any");

        Exception ex = new RuntimeException("boom");

        ResponseEntity<ErrorResponse> response =
                handler.handleGlobalException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals("Internal Server Error", body.getError());
        assertEquals("An unexpected error occurred: boom", body.getMessage());
        assertEquals("/any", body.getPath());
        assertNotNull(body.getTimestamp());
    }

    // ---------- Inner class: ErrorResponse getters ----------

    @Test
    void errorResponse_getters_returnValues() {
        LocalDateTime now = LocalDateTime.now();
        ErrorResponse er = new ErrorResponse(
                now,
                418,
                "I am a teapot",
                "Short and stout",
                "/brew"
        );

        assertEquals(now, er.getTimestamp());
        assertEquals(418, er.getStatus());
        assertEquals("I am a teapot", er.getError());
        assertEquals("Short and stout", er.getMessage());
        assertEquals("/brew", er.getPath());
    }

    // ---------- Inner class: ValidationErrorResponse getters ----------

    @Test
    void validationErrorResponse_getters_returnValidationErrorsAndBaseFields() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> validation = new HashMap<>();
        validation.put("field1", "error1");
        validation.put("field2", "error2");

        ValidationErrorResponse ver = new ValidationErrorResponse(
                now,
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                validation,
                "/path"
        );

        // Base ErrorResponse fields
        assertEquals(now, ver.getTimestamp());
        assertEquals(HttpStatus.BAD_REQUEST.value(), ver.getStatus());
        assertEquals("Validation Failed", ver.getError());
        assertEquals("Validation failed for one or more fields", ver.getMessage());
        assertEquals("/path", ver.getPath());

        // Extra validation map
        assertEquals(validation, ver.getValidationErrors());
        assertEquals("error1", ver.getValidationErrors().get("field1"));
        assertEquals("error2", ver.getValidationErrors().get("field2"));
    }
}

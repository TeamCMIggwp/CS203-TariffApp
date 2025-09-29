package auth;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class AuthExceptionHandler {
    @ExceptionHandler(AuthService.Unauthorized.class)
    public ResponseEntity<?> handleUnauthorized(AuthService.Unauthorized ex) {
        return ResponseEntity.status(401).body(Map.of("message", ex.getMessage()));
    }
}

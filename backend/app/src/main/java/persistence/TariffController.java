package persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:3000", "https://teamcmiggwp.duckdns.org"})
public class TariffController {
    @Autowired
    private TariffRepository tariffRepository;

    @PostMapping("/update-tariff")
    public ResponseEntity<?> updateTariffRate(@RequestBody TariffRate tariffRate) {
        try {
            // Validate input
            if (tariffRate.getPartnerId() == null || tariffRate.getCountryId() == null || 
                tariffRate.getProductHsCode() == null || tariffRate.getYear() == null || 
                tariffRate.getRate() == null) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("All fields are required"));
            }

            tariffRepository.updateTariffRate(tariffRate);
            return ResponseEntity.ok(new SuccessResponse("Tariff rate updated successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Failed to update tariff rate: " + e.getMessage()));
        }
    }

    private static class ErrorResponse {
        private final String message;
        public ErrorResponse(String message) { 
            this.message = message; 
        }

        @JsonProperty("message")
        public String getMessage() { 
            return message; 
        }
    }

    private static class SuccessResponse {
        private final String message;
        public SuccessResponse(String message) { this.message = message; }

        @JsonProperty("message")
        public String getMessage() { 
            return message; 
        }
    }
}

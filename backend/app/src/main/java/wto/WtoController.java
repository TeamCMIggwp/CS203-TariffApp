package wto;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WtoController {
    private final WtoApiService wto;

    public WtoController(WtoApiService wto) { 
        this.wto = wto; 
    }

    @GetMapping("/api/wto/ricedata")
    public ResponseEntity<String> indicators() {
        return wto.getAllRiceData();
    }
}
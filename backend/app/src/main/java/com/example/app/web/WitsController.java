package com.example.app.web;

import com.example.app.service.WitsApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WitsController {

  private final WitsApiService wits;

  public WitsController(WitsApiService wits) {
    this.wits = wits;
  }

  // hardcoded service method
    @GetMapping("/api/wits/tariffs")
    public ResponseEntity<String> tariffs() {
        return wits.getTariffData();
    }

    // input version, uncomment when frontend is ready
//   @GetMapping("/api/wits/tariffs")
//   public ResponseEntity<String> getTariffs(
//       @RequestParam String reporter,
//       @RequestParam String partner,
//       @RequestParam String product,
//       @RequestParam String year,
//       @RequestParam(defaultValue = "reported") String datatype
//   ) {
//     return wits.getTariffData(reporter, partner, product, year, datatype);
//   }
}


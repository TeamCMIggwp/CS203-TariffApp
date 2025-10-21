package wto;

import wto.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * RESTful controller for fetching WTO time-series observations.
 *
 * Canonical resource:
 *   GET /api/v1/indicators/{indicator}/observations
 *
 * Frontend passes the indicator in the path and ONLY the relevant query params:
 *
 * HSP group (e.g., HS_P_0070):
 *   Required: r, pc, ps
 *   Optional: p, fmt (default json), mode (optional; set if you want "full")
 *   Example:
 *     /api/v1/indicators/HS_P_0070/observations?r=840&pc=100630&ps=2018-2024&fmt=json&mode=full
 *
 * TPA group (TP_A_0160 / TP_A_0170):
 *   Required: r, ps
 *   Optional: fmt (default json)
 *   Example:
 *     /api/v1/indicators/TP_A_0170/observations?r=840&ps=2015-2024
 *
 * TPB group (TP_B_0180 / TP_B_0190):
 *   Required: r
 *   Optional: fmt (default json); mode defaults to "full" if omitted
 *   Example:
 *     /api/v1/indicators/TP_B_0180/observations?r=840&mode=full
 */
@RestController
@RequestMapping("/api/v1/indicators")
public class WtoController {

    private final WtoApiService svc;

    public WtoController(WtoApiService svc) {
        this.svc = svc;
    }

    @GetMapping("/{indicator}/observations")
    public ResponseEntity<?> getObservations(
            @PathVariable String indicator,
            @RequestParam(required = false) String r,
            @RequestParam(required = false) String p,
            @RequestParam(required = false) String pc,
            @RequestParam(required = false) String ps,
            @RequestParam(defaultValue = "json") String fmt,
            @RequestParam(required = false) String mode
    ) {
        String i = indicator.trim().toUpperCase();

        // ---------- TPA group: TP_A_0160 / TP_A_0170 ----------
        if (i.equals("TP_A_0160") || i.equals("TP_A_0170")) {
            if (isBlank(r) || isBlank(ps)) {
                return unprocessable("Required params for " + i + ": r, ps");
            }
            WtoTPARequest req = new WtoTPARequest();
            req.setIndicator(i);
            req.setReporter(r);
            req.setPeriod(ps);
            req.setFormat(fmt);
            return svc.callWtoApi(req.toQuery());
        }

        // ---------- TPB group: TP_B_0180 / TP_B_0190 ----------
        if (i.equals("TP_B_0180") || i.equals("TP_B_0190")) {
            if (isBlank(r)) {
                return unprocessable("Required param for " + i + ": r");
            }
            if (isBlank(mode)) mode = "full"; // default per your spec for TPB
            WtoTPBRequest req = new WtoTPBRequest();
            req.setIndicator(i);
            req.setReporter(r);
            req.setFormat(fmt);
            req.setMode(mode);
            return svc.callWtoApi(req.toQuery());
        }

        // ---------- Default: treat as HSP group (e.g., HS_P_0070) ----------
        if (isBlank(r) || isBlank(pc) || isBlank(ps)) {
            return unprocessable("Required params for " + i + " (HSP): r, pc, ps");
        }
        // Only set mode if provided (you can default it to "full" here if you want)
        WtoHSPRequest req = new WtoHSPRequest();
        req.setIndicator(i);
        req.setReporter(r);
        req.setPartner(p);       // optional
        req.setProductCode(pc);
        req.setPeriod(ps);
        req.setFormat(fmt);
        if (!isBlank(mode)) req.setMode(mode);

        return svc.callWtoApi(req.toQuery());
    }

    // -------- Helpers --------
    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static ResponseEntity<String> unprocessable(String msg) {
        return ResponseEntity.unprocessableEntity().body(msg);
    }
}

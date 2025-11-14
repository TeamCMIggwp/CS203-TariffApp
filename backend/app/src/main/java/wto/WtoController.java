package wto;

import wto.dto.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/indicators")
@Tag(name = "WTO", description = "WTO time-series endpoints")
public class WtoController {

    // Indicator type constants
    private static final String INDICATOR_TPA_0160 = "TP_A_0160";
    private static final String INDICATOR_TPA_0170 = "TP_A_0170";
    private static final String INDICATOR_TPB_0180 = "TP_B_0180";
    private static final String INDICATOR_TPB_0190 = "TP_B_0190";
    private static final String MODE_FULL = "full";

    private final WtoApiService svc;

    public WtoController(WtoApiService svc) {
        this.svc = svc;
    }

    @Operation(
        summary = "Get observations",
        description = """
            Fetch WTO time-series observations.
            - HSP (e.g., HS_P_0070): requires r, pc, ps (p optional)
            - TPA (TP_A_0160/TP_A_0170): requires r, ps
            - TPB (TP_B_0180/TP_B_0190): requires r (mode defaults to full)
            """
    )
    @GetMapping("/{indicator}/observations")
    public ResponseEntity<?> getObservations(
            @Parameter(name = "indicator", description = "Indicator code, e.g. HS_P_0070 / TP_A_0170 / TP_B_0180", example = "HS_P_0070")
            @PathVariable(name = "indicator") String indicator,

            @Parameter(name = "r", description = "Reporting economy code (ISO numeric), e.g. 840 = USA", example = "840")
            @RequestParam(name = "r", required = false) String r,

            @Parameter(name = "p", description = "Partner economy code (ISO numeric), optional for HSP", example = "356")
            @RequestParam(name = "p", required = false) String p,

            @Parameter(name = "pc", description = "HS 6-digit product code (HSP only), e.g. 100630 (rice)", example = "100630")
            @RequestParam(name = "pc", required = false) String pc,

            @Parameter(name = "ps", description = "Period (year or range), e.g. 2018 or 2015-2024", example = "2018-2024")
            @RequestParam(name = "ps", required = false) String ps,

            @Parameter(name = "fmt", description = "Output format", example = "json")
            @RequestParam(name = "fmt", defaultValue = "json") String fmt,

            @Parameter(name = "mode", description = "Detail mode: full or compact (TPB requires full; HSP optional)", example = "full")
            @RequestParam(name = "mode", required = false) String mode,

            @Parameter(name = "echo", description = "If true, bypasses WTO call and echoes your inputs", example = "false")
            @RequestParam(name = "echo", required = false, defaultValue = "false") boolean echo
    ) {
        String i = indicator.trim().toUpperCase();

        if (echo) {
            return ResponseEntity.ok(Map.of(
                    "echo", Map.of("indicator", i, "r", r, "p", p, "pc", pc, "ps", ps, "fmt", fmt, "mode", mode)
            ));
        }

        // TPA group
        if (i.equals(INDICATOR_TPA_0160) || i.equals(INDICATOR_TPA_0170)) {
            if (isBlank(r) || isBlank(ps)) {
                return ResponseEntity.unprocessableEntity().body("Required params for " + i + ": r, ps");
            }
            WtoTPARequest req = new WtoTPARequest();
            req.setIndicator(i); req.setReporter(r); req.setPeriod(ps); req.setFormat(fmt);
            return svc.callWtoApi(req.toQuery());
        }

        // TPB group
        if (i.equals(INDICATOR_TPB_0180) || i.equals(INDICATOR_TPB_0190)) {
            if (isBlank(r)) {
                return ResponseEntity.unprocessableEntity().body("Required param for " + i + ": r");
            }
            if (isBlank(mode)) mode = MODE_FULL;
            WtoTPBRequest req = new WtoTPBRequest();
            req.setIndicator(i); req.setReporter(r); req.setFormat(fmt); req.setMode(mode);
            return svc.callWtoApi(req.toQuery());
        }

        // Default: HSP
        if (isBlank(r) || isBlank(pc) || isBlank(ps)) {
            return ResponseEntity.unprocessableEntity().body("Required params for " + i + " (HSP): r, pc, ps");
        }
        WtoHSPRequest req = new WtoHSPRequest();
        req.setIndicator(i); req.setReporter(r); req.setPartner(p);
        req.setProductCode(pc); req.setPeriod(ps); req.setFormat(fmt);
        if (!isBlank(mode)) req.setMode(mode);
        return svc.callWtoApi(req.toQuery());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
package wto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "WTO Data", description = "World Trade Organization tariff and trade data for rice products")
public class WtoController {
    private final WtoApiService wto;

    public WtoController(WtoApiService wto) { 
        this.wto = wto; 
    }

    @GetMapping("/api/wto/ricedata")
    @Operation(
        summary = "Get WTO rice tariff data",
        description = """
            Retrieves preferential tariff data for all rice products from the WTO API.
            This endpoint returns data for rice product codes: 100610, 100620, 100630, 100640 (including broken rice).
            
            The data includes:
            - Preferential tariff rates between countries
            - Product classifications (HS codes)
            - Trade agreements (e.g., MERCOSUR)
            - Annual tariff rates and values
            
            **Example URL:** https://teamcmiggwp.duckdns.org/api/wto/ricedata
            
            **cURL Example:**

            curl -X GET "https://teamcmiggwp.duckdns.org/api/wto/ricedata" -H "accept: application/json"

            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved rice tariff data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "string"),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                          "Dataset": [
                            {
                              "IndicatorCategoryCode": "TPA_PRF_HS",
                              "IndicatorCategory": "Preferential tariffs",
                              "IndicatorCode": "HS_P_0070",
                              "Indicator": "Lowest preferential tariff - simple average ad valorem tariff at HS 6-digit",
                              "ReportingEconomyCode": "068",
                              "ReportingEconomy": "Bolivia, Plurinational State of",
                              "PartnerEconomyCode": "032",
                              "PartnerEconomy": "Argentina",
                              "ProductOrSectorClassificationCode": "HS",
                              "ProductOrSectorClassification": "Harmonized System",
                              "ProductOrSectorCode": "100640",
                              "ProductOrSector": "Broken rice",
                              "PeriodCode": "A",
                              "Period": "Annual",
                              "FrequencyCode": "A",
                              "Frequency": "Annual",
                              "UnitCode": "PCT",
                              "Unit": "Percent",
                              "Year": 2020,
                              "ValueFlagCode": "PREF",
                              "ValueFlag": "Preferential scheme",
                              "TextValue": "Free-trade agreement duty rate for the Southern Common Market (MERCOSUR)",
                              "Value": 0.0
                            },
                            {
                              "IndicatorCategoryCode": "TPA_PRF_HS",
                              "IndicatorCategory": "Preferential tariffs",
                              "IndicatorCode": "HS_P_0070",
                              "Indicator": "Lowest preferential tariff - simple average ad valorem tariff at HS 6-digit",
                              "ReportingEconomyCode": "068",
                              "ReportingEconomy": "Bolivia, Plurinational State of",
                              "PartnerEconomyCode": "032",
                              "PartnerEconomy": "Argentina",
                              "ProductOrSectorClassificationCode": "HS",
                              "ProductOrSectorClassification": "Harmonized System",
                              "ProductOrSectorCode": "100640",
                              "ProductOrSector": "Broken rice",
                              "PeriodCode": "A",
                              "Period": "Annual",
                              "FrequencyCode": "A",
                              "Frequency": "Annual",
                              "UnitCode": "PCT",
                              "Unit": "Percent",
                              "Year": 2022,
                              "ValueFlagCode": "PREF",
                              "ValueFlag": "Preferential scheme",
                              "TextValue": "Free-trade agreement duty rate for the Southern Common Market (MERCOSUR)",
                              "Value": 0.0
                            },
                            {
                              "IndicatorCategoryCode": "TPA_PRF_HS",
                              "IndicatorCategory": "Preferential tariffs",
                              "IndicatorCode": "HS_P_0070",
                              "Indicator": "Lowest preferential tariff - simple average ad valorem tariff at HS 6-digit",
                              "ReportingEconomyCode": "068",
                              "ReportingEconomy": "Bolivia, Plurinational State of",
                              "PartnerEconomyCode": "076",
                              "PartnerEconomy": "Brazil",
                              "ProductOrSectorClassificationCode": "HS",
                              "ProductOrSectorClassification": "Harmonized System",
                              "ProductOrSectorCode": "100640",
                              "ProductOrSector": "Broken rice",
                              "PeriodCode": "A",
                              "Period": "Annual",
                              "FrequencyCode": "A",
                              "Frequency": "Annual",
                              "UnitCode": "PCT",
                              "Unit": "Percent",
                              "Year": 2020,
                              "ValueFlagCode": "PREF",
                              "ValueFlag": "Preferential scheme",
                              "TextValue": "Free-trade agreement duty rate for the Southern Common Market (MERCOSUR)",
                              "Value": 0.0
                            }
                          ]
                        }
                        """,
                    description = "WTO rice tariff data with preferential rates for different countries and years"
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing WTO API key",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "Unauthorized access to WTO API"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - Failed to retrieve data from WTO API",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "Failed to communicate with WTO API"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<String> indicators() {
        return wto.getAllRiceData();
    }
}
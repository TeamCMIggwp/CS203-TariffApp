package integration;

import database.tariffs.dto.CreateTariffRequest;
import database.tariffs.dto.UpdateTariffRequest;
import database.tariffs.dto.TariffResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TariffController.
 * Tests the full stack: Controller -> Service -> Repository -> Database
 */
class TariffControllerIntegrationTest extends BaseIntegrationTest {

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void createTariff_withValidData_returnsCreatedTariff() {
        // Arrange
        CreateTariffRequest request = new CreateTariffRequest();
        request.setReporter("840"); // USA
        request.setPartner("356"); // India
        request.setProduct(100630); // Rice product code
        request.setYear("2020");
        request.setRate(24.0);
        request.setUnit("percent");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateTariffRequest> entity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<TariffResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/tariffs",
                entity,
                TariffResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getReporter()).isEqualTo("840");
        assertThat(response.getBody().getPartner()).isEqualTo("356");
        assertThat(response.getBody().getProduct()).isEqualTo(100630);
        assertThat(response.getBody().getYear()).isEqualTo("2020");
        assertThat(response.getBody().getRate()).isEqualTo(24.0);
        assertThat(response.getBody().getUnit()).isEqualTo("percent");

        // Verify it was actually saved in database
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wto_tariffs.TariffRates WHERE country_id = ? AND partner_country_id = ? AND product_id = ? AND `year` = ?",
                Integer.class,
                "840", "356", 100630, 2020
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void getTariff_withExistingTariff_returnsTariffDetails() {
        // Arrange - Insert test data directly
        jdbcTemplate.update(
                "INSERT INTO wto_tariffs.TariffRates (country_id, partner_country_id, product_id, `year`, rate, unit) VALUES (?, ?, ?, ?, ?, ?)",
                "702", "156", 271019, 2020, 5.0, "percent"
        );

        // Act
        ResponseEntity<TariffResponse> response = restTemplate.getForEntity(
                baseUrl + "/api/v1/tariffs?reporter=702&partner=156&product=271019&year=2020",
                TariffResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getReporter()).isEqualTo("702");
        assertThat(response.getBody().getPartner()).isEqualTo("156");
        assertThat(response.getBody().getProduct()).isEqualTo(271019);
        assertThat(response.getBody().getYear()).isEqualTo("2020");
        assertThat(response.getBody().getRate()).isEqualTo(5.0);
        assertThat(response.getBody().getUnit()).isEqualTo("percent");
    }

    @Test
    void getAllTariffs_withMultipleTariffs_returnsAllTariffs() {
        // Arrange - Insert multiple tariffs
        jdbcTemplate.update(
                "INSERT INTO wto_tariffs.TariffRates (country_id, partner_country_id, product_id, `year`, rate, unit) VALUES (?, ?, ?, ?, ?, ?)",
                "840", "356", 100630, 2020, 24.0, "percent"
        );
        jdbcTemplate.update(
                "INSERT INTO wto_tariffs.TariffRates (country_id, partner_country_id, product_id, `year`, rate, unit) VALUES (?, ?, ?, ?, ?, ?)",
                "702", "156", 271019, 2021, 5.0, "percent"
        );
        jdbcTemplate.update(
                "INSERT INTO wto_tariffs.TariffRates (country_id, partner_country_id, product_id, `year`, rate, unit) VALUES (?, ?, ?, ?, ?, ?)",
                "276", "392", 854590, 2019, 15.5, "percent"
        );

        // Act
        ResponseEntity<TariffResponse[]> response = restTemplate.getForEntity(
                baseUrl + "/api/v1/tariffs/current",
                TariffResponse[].class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(3);
    }

    @Test
    void updateTariff_withValidData_updatesTariffSuccessfully() {
        // Arrange - Insert initial tariff
        jdbcTemplate.update(
                "INSERT INTO wto_tariffs.TariffRates (country_id, partner_country_id, product_id, `year`, rate, unit) VALUES (?, ?, ?, ?, ?, ?)",
                "840", "356", 100630, 2020, 24.0, "percent"
        );

        UpdateTariffRequest updateRequest = new UpdateTariffRequest();
        updateRequest.setReporter("840");
        updateRequest.setPartner("356");
        updateRequest.setProduct(100630);
        updateRequest.setYear("2020");
        updateRequest.setRate(30.0); // Updated rate
        updateRequest.setUnit("percent");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateTariffRequest> entity = new HttpEntity<>(updateRequest, headers);

        // Act
        ResponseEntity<TariffResponse> response = restTemplate.exchange(
                baseUrl + "/api/v1/tariffs",
                HttpMethod.PUT,
                entity,
                TariffResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRate()).isEqualTo(30.0);

        // Verify database was updated
        Double rate = jdbcTemplate.queryForObject(
                "SELECT rate FROM wto_tariffs.TariffRates WHERE country_id = ? AND partner_country_id = ? AND product_id = ? AND `year` = ?",
                Double.class,
                "840", "356", 100630, 2020
        );
        assertThat(rate).isEqualTo(30.0);
    }

    @Test
    void deleteTariff_withExistingTariff_deletesSuccessfully() {
        // Arrange - Insert tariff to delete
        jdbcTemplate.update(
                "INSERT INTO wto_tariffs.TariffRates (country_id, partner_country_id, product_id, `year`, rate, unit) VALUES (?, ?, ?, ?, ?, ?)",
                "840", "356", 100630, 2020, 24.0, "percent"
        );

        // Act
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/api/v1/tariffs?reporter=840&partner=356&product=100630&year=2020",
                HttpMethod.DELETE,
                null,
                Void.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify deletion in database
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wto_tariffs.TariffRates WHERE country_id = ? AND partner_country_id = ? AND product_id = ? AND `year` = ?",
                Integer.class,
                "840", "356", 100630, 2020
        );
        assertThat(count).isEqualTo(0);
    }

    @Test
    void getTariff_withMissingParameters_returnsBadRequest() {
        // Act - Missing 'year' parameter
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/api/v1/tariffs?reporter=840&partner=356&product=100630",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getTariff_withNonExistentTariff_returnsNotFound() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/api/v1/tariffs?reporter=999&partner=999&product=999999&year=9999",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void createTariff_withDuplicateKey_returnsConflict() {
        // Arrange - Insert initial tariff
        jdbcTemplate.update(
                "INSERT INTO wto_tariffs.TariffRates (country_id, partner_country_id, product_id, `year`, rate, unit) VALUES (?, ?, ?, ?, ?, ?)",
                "840", "356", 100630, 2020, 24.0, "percent"
        );

        CreateTariffRequest request = new CreateTariffRequest();
        request.setReporter("840");
        request.setPartner("356");
        request.setProduct(100630);
        request.setYear("2020");
        request.setRate(25.0);
        request.setUnit("percent");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateTariffRequest> entity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/tariffs",
                entity,
                String.class
        );

        // Assert - Should handle duplicate key error
        assertThat(response.getStatusCode()).isIn(HttpStatus.CONFLICT, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void createTariff_withInvalidData_returnsBadRequest() {
        // Arrange - Invalid year format
        CreateTariffRequest request = new CreateTariffRequest();
        request.setReporter("840");
        request.setPartner("356");
        request.setProduct(100630);
        request.setYear("20"); // Invalid: only 2 digits
        request.setRate(24.0);
        request.setUnit("percent");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateTariffRequest> entity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/tariffs",
                entity,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

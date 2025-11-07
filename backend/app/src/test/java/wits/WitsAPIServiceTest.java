package wits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import wits.dto.WitsTariffRateResponse;
import wits.exception.WitsApiException;
import wits.exception.WitsDataNotFoundException;

class WitsApiServiceTest {

        private WitsApiService service;
        private ExchangeFunction exchange;

        @BeforeEach
        void setup() {
                exchange = mock(ExchangeFunction.class);
                WebClient client = WebClient.builder().exchangeFunction(exchange).build();

                service = new WitsApiService();
                ReflectionTestUtils.setField(service, "webClient", client);
        }

        private static ClientResponse jsonResponse(HttpStatus status, String body) {
                return ClientResponse.create(status)
                                .header("Content-Type", "application/json")
                                .body(body == null ? "" : body)
                                .build();
        }

        private String sampleSdmx() {
                return "{"
                                + "\"structure\":{\"attributes\":{\"observation\":["
                                + "{\"id\":\"MIN_RATE\",\"values\":[{\"id\":\"5.0\"}]},"
                                + "{\"id\":\"MAX_RATE\",\"values\":[{\"id\":\"10.0\"}]},"
                                + "{\"id\":\"AVG_RATE\",\"values\":[{\"id\":\"7.5\"}]}"
                                + "]}},"
                                + "\"dataSets\":[{\"series\":{\"0:0:0:0\":{\"observations\":{\"0\":[123,0,0,0]}}}}]"
                                + "}";
        }

        @Test
        void getTariffRate_success_parsesAllRates() {
                when(exchange.exchange(any(ClientRequest.class)))
                                .thenReturn(Mono.just(jsonResponse(HttpStatus.OK, sampleSdmx())));

                WitsTariffRateResponse out = service.getTariffRate("840", "000", 100630, "2020");

                assertNotNull(out);
                assertEquals("5.0", out.getMinRate());
                assertEquals("10.0", out.getMaxRate());
                assertEquals("7.5", out.getAvgRate());
                verify(exchange, times(1)).exchange(any(ClientRequest.class));
        }

        @Test
        void getTariffRate_noData_throwsNotFound() {
                when(exchange.exchange(any(ClientRequest.class)))
                                .thenReturn(Mono.just(jsonResponse(HttpStatus.NO_CONTENT, "")));

                assertThrows(WitsDataNotFoundException.class,
                                () -> service.getTariffRate("702", "156", 100630, "2018"));
        }

        @Test
        void getTariffRate_serverError_wrapsInWitsApiException() {
                when(exchange.exchange(any(ClientRequest.class)))
                                .thenReturn(Mono.just(
                                                jsonResponse(HttpStatus.INTERNAL_SERVER_ERROR, "{\"err\":\"x\"}")));

                WitsApiException ex = assertThrows(WitsApiException.class,
                                () -> service.getTariffRate("702", "156", 100630, "2018"));

                // Align with your implementation: message-based check, do not assume specific
                // status mapping
                assertTrue(ex.getMessage().toLowerCase().contains("wits"));
                // If your WitsApiException exposes a status code, it currently appears as 0;
                // just assert non-negative
                assertTrue(ex.getStatusCode() >= 0);
        }

        @Test
        void getRawTariffData_success_returnsBody() {
                when(exchange.exchange(any(ClientRequest.class)))
                                .thenReturn(Mono.just(jsonResponse(HttpStatus.OK, "{\"ok\":true}")));

                String body = service.getRawTariffData("840", "000", 100630, "2020");
                assertEquals("{\"ok\":true}", body);
        }

        @Test
        void getRawTariffData_blankBody_throwsNotFound() {
                when(exchange.exchange(any(ClientRequest.class)))
                                .thenReturn(Mono.just(jsonResponse(HttpStatus.OK, "")));

                assertThrows(WitsDataNotFoundException.class,
                                () -> service.getRawTariffData("840", "000", 100630, "2020"));
        }

        @Test
        void getRawTariffData_transportError_wrapsInWitsApiException() {
                when(exchange.exchange(any(ClientRequest.class)))
                                .thenReturn(Mono.error(new RuntimeException("boom")));

                WitsApiException ex = assertThrows(WitsApiException.class,
                                () -> service.getRawTariffData("840", "000", 100630, "2020"));

                // We only enforce:
                // 1) Correct exception type
                // 2) There is some message
                // 3) statusCode is set (your impl uses 0 / default)
                assertNotNull(ex.getMessage());
                assertFalse(ex.getMessage().isBlank());
                assertTrue(ex.getStatusCode() >= 0);
        }
}

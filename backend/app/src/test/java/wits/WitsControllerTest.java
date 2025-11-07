package wits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import wits.dto.WitsTariffRateResponse;

@ExtendWith(MockitoExtension.class)
class WitsControllerTest {

    @Mock
    private WitsApiService witsService;

    @InjectMocks
    private WitsController controller;

    @Test
    void getTariffRate_success_returnsBodyFromService() {
        WitsTariffRateResponse resp =
            new WitsTariffRateResponse("840","000",100630,"2020","5.0","10.0","7.5");
        when(witsService.getTariffRate("840","000",100630,"2020")).thenReturn(resp);

        ResponseEntity<WitsTariffRateResponse> entity =
                controller.getTariffRate("840","000",100630,"2020");

        assertEquals(200, entity.getStatusCodeValue());
        assertSame(resp, entity.getBody());
        verify(witsService).getTariffRate("840","000",100630,"2020");
        verifyNoMoreInteractions(witsService);
    }

    @Test
    void getRawTariffData_success_returnsBodyFromService() {
        when(witsService.getRawTariffData("702","156",100630,"2019")).thenReturn("{\"ok\":true}");

        ResponseEntity<String> entity =
                controller.getRawTariffData("702","156",100630,"2019");

        assertEquals(200, entity.getStatusCodeValue());
        assertEquals("{\"ok\":true}", entity.getBody());
        verify(witsService).getRawTariffData("702","156",100630,"2019");
        verifyNoMoreInteractions(witsService);
    }
}

package database.tariffs.service;

import database.tariffs.dto.CreateTariffRequest;
import database.tariffs.dto.TariffResponse;
import database.tariffs.dto.UpdateTariffRequest;
import database.tariffs.entity.TariffRateEntity;
import database.tariffs.exception.TariffAlreadyExistsException;
import database.tariffs.exception.TariffNotFoundException;
import database.tariffs.repository.TariffRateRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TariffServiceTest {

    @Mock
    private TariffRateRepository repository;

    @InjectMocks
    private TariffService service;

    // Test data
    private static final String REPORTER = "702";
    private static final String PARTNER = "156";
    private static final Integer PRODUCT = 1001;
    private static final String YEAR = "2023";
    private static final Double RATE = 5.0;
    private static final String UNIT = "percent";

    @Test
    void createTariff_success() {
        // Given
        CreateTariffRequest request = new CreateTariffRequest();
        request.setReporter(REPORTER);
        request.setPartner(PARTNER);
        request.setProduct(PRODUCT);
        request.setYear(YEAR);
        request.setRate(RATE);
        request.setUnit(UNIT);

        when(repository.exists(REPORTER, PARTNER, PRODUCT, YEAR)).thenReturn(false);

        // When
        TariffResponse response = service.createTariff(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getReporter()).isEqualTo(REPORTER);
        assertThat(response.getPartner()).isEqualTo(PARTNER);
        assertThat(response.getProduct()).isEqualTo(PRODUCT);
        assertThat(response.getYear()).isEqualTo(YEAR);
        assertThat(response.getRate()).isEqualTo(RATE);
        assertThat(response.getUnit()).isEqualTo(UNIT);

        verify(repository).exists(REPORTER, PARTNER, PRODUCT, YEAR);
        verify(repository).create(REPORTER, PARTNER, PRODUCT, YEAR, RATE, UNIT);
    }

    @Test
    void createTariff_alreadyExists_throwsException() {
        // Given
        CreateTariffRequest request = new CreateTariffRequest();
        request.setReporter(REPORTER);
        request.setPartner(PARTNER);
        request.setProduct(PRODUCT);
        request.setYear(YEAR);
        request.setRate(RATE);
        request.setUnit(UNIT);

        when(repository.exists(REPORTER, PARTNER, PRODUCT, YEAR)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> service.createTariff(request))
                .isInstanceOf(TariffAlreadyExistsException.class);

        verify(repository).exists(REPORTER, PARTNER, PRODUCT, YEAR);
        verify(repository, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void getTariff_found_returnsResponse() {
        // Given
        TariffRateEntity entity = new TariffRateEntity();
        entity.setCountryIsoNumeric(REPORTER);
        entity.setPartnerIsoNumeric(PARTNER);
        entity.setProductHsCode(PRODUCT);
        entity.setYear(YEAR);
        entity.setRate(RATE);
        entity.setUnit(UNIT);

        when(repository.getTariff(REPORTER, PARTNER, PRODUCT, YEAR)).thenReturn(entity);

        // When
        TariffResponse response = service.getTariff(REPORTER, PARTNER, PRODUCT, YEAR);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getReporter()).isEqualTo(REPORTER);
        assertThat(response.getPartner()).isEqualTo(PARTNER);
        assertThat(response.getProduct()).isEqualTo(PRODUCT);
        assertThat(response.getYear()).isEqualTo(YEAR);
        assertThat(response.getRate()).isEqualTo(RATE);
        assertThat(response.getUnit()).isEqualTo(UNIT);

        verify(repository).getTariff(REPORTER, PARTNER, PRODUCT, YEAR);
    }

    @Test
    void getTariff_notFound_throwsException() {
        // Given
        when(repository.getTariff(REPORTER, PARTNER, PRODUCT, YEAR)).thenReturn(null);

        // When/Then
        assertThatThrownBy(() -> service.getTariff(REPORTER, PARTNER, PRODUCT, YEAR))
                .isInstanceOf(TariffNotFoundException.class);

        verify(repository).getTariff(REPORTER, PARTNER, PRODUCT, YEAR);
    }

    @Test
    void updateTariff_success() {
        // Given
        UpdateTariffRequest request = new UpdateTariffRequest();
        request.setRate(7.5);
        request.setUnit("ad valorem");

        when(repository.exists(REPORTER, PARTNER, PRODUCT, YEAR)).thenReturn(true);
        when(repository.update(REPORTER, PARTNER, PRODUCT, YEAR, 7.5, "ad valorem")).thenReturn(1);

        // When
        TariffResponse response = service.updateTariff(REPORTER, PARTNER, PRODUCT, YEAR, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRate()).isEqualTo(7.5);
        assertThat(response.getUnit()).isEqualTo("ad valorem");

        verify(repository).exists(REPORTER, PARTNER, PRODUCT, YEAR);
        verify(repository).update(REPORTER, PARTNER, PRODUCT, YEAR, 7.5, "ad valorem");
    }

    @Test
    void updateTariff_notFoundOnCheck_throwsException() {
        // Given
        UpdateTariffRequest request = new UpdateTariffRequest();
        request.setRate(7.5);
        request.setUnit("ad valorem");

        when(repository.exists(REPORTER, PARTNER, PRODUCT, YEAR)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> service.updateTariff(REPORTER, PARTNER, PRODUCT, YEAR, request))
                .isInstanceOf(TariffNotFoundException.class);

        verify(repository).exists(REPORTER, PARTNER, PRODUCT, YEAR);
        verify(repository, never()).update(any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateTariff_notFoundOnUpdate_throwsException() {
        // Given
        UpdateTariffRequest request = new UpdateTariffRequest();
        request.setRate(7.5);
        request.setUnit("ad valorem");

        when(repository.exists(REPORTER, PARTNER, PRODUCT, YEAR)).thenReturn(true);
        when(repository.update(REPORTER, PARTNER, PRODUCT, YEAR, 7.5, "ad valorem")).thenReturn(0);

        // When/Then
        assertThatThrownBy(() -> service.updateTariff(REPORTER, PARTNER, PRODUCT, YEAR, request))
                .isInstanceOf(TariffNotFoundException.class);

        verify(repository).exists(REPORTER, PARTNER, PRODUCT, YEAR);
        verify(repository).update(REPORTER, PARTNER, PRODUCT, YEAR, 7.5, "ad valorem");
    }

    @Test
    void deleteTariff_success() {
        // Given
        when(repository.exists(REPORTER, PARTNER, PRODUCT, YEAR)).thenReturn(true);
        when(repository.delete(REPORTER, PARTNER, PRODUCT, YEAR)).thenReturn(1);

        // When
        service.deleteTariff(REPORTER, PARTNER, PRODUCT, YEAR);

        // Then
        verify(repository).exists(REPORTER, PARTNER, PRODUCT, YEAR);
        verify(repository).delete(REPORTER, PARTNER, PRODUCT, YEAR);
    }

    @Test
    void deleteTariff_notFoundOnCheck_throwsException() {
        // Given
        when(repository.exists(REPORTER, PARTNER, PRODUCT, YEAR)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> service.deleteTariff(REPORTER, PARTNER, PRODUCT, YEAR))
                .isInstanceOf(TariffNotFoundException.class);

        verify(repository).exists(REPORTER, PARTNER, PRODUCT, YEAR);
        verify(repository, never()).delete(any(), any(), any(), any());
    }

    @Test
    void deleteTariff_notFoundOnDelete_throwsException() {
        // Given
        when(repository.exists(REPORTER, PARTNER, PRODUCT, YEAR)).thenReturn(true);
        when(repository.delete(REPORTER, PARTNER, PRODUCT, YEAR)).thenReturn(0);

        // When/Then
        assertThatThrownBy(() -> service.deleteTariff(REPORTER, PARTNER, PRODUCT, YEAR))
                .isInstanceOf(TariffNotFoundException.class);

        verify(repository).exists(REPORTER, PARTNER, PRODUCT, YEAR);
        verify(repository).delete(REPORTER, PARTNER, PRODUCT, YEAR);
    }

    @Test
    void getAllTariffs_returnsAllTariffs() {
        // Given
        TariffRateEntity entity1 = new TariffRateEntity();
        entity1.setCountryIsoNumeric("702");
        entity1.setPartnerIsoNumeric("156");
        entity1.setProductHsCode(1001);
        entity1.setYear("2023");
        entity1.setRate(5.0);
        entity1.setUnit("percent");

        TariffRateEntity entity2 = new TariffRateEntity();
        entity2.setCountryIsoNumeric("840");
        entity2.setPartnerIsoNumeric("156");
        entity2.setProductHsCode(1002);
        entity2.setYear("2024");
        entity2.setRate(3.0);
        entity2.setUnit("ad valorem");

        when(repository.getAllTariffs()).thenReturn(Arrays.asList(entity1, entity2));

        // When
        List<TariffResponse> responses = service.getAllTariffs();

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getReporter()).isEqualTo("702");
        assertThat(responses.get(1).getReporter()).isEqualTo("840");

        verify(repository).getAllTariffs();
    }
}

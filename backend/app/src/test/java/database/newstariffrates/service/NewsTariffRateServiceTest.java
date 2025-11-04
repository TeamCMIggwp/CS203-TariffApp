package database.newstariffrates.service;

import database.newstariffrates.dto.CreateNewsTariffRateRequest;
import database.newstariffrates.dto.NewsTariffRateResponse;
import database.newstariffrates.entity.NewsTariffRate;
import database.newstariffrates.repository.NewsTariffRateRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.Timestamp;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsTariffRateServiceTest {

    @Mock
    private NewsTariffRateRepository repository;

    @InjectMocks
    private NewsTariffRateService service;

    private static final String NEWS_LINK = "https://example.com/news/1";
    private static final String COUNTRY_ID = "702";
    private static final String PARTNER_COUNTRY_ID = "156";
    private static final Integer PRODUCT_ID = 1001;
    private static final Integer TARIFF_TYPE_ID = 1;
    private static final Integer YEAR = 2023;
    private static final Double RATE = 5.0;
    private static final String UNIT = "percent";

    @Test
    void createTariffRate_success() {
        // Given
        CreateNewsTariffRateRequest request = new CreateNewsTariffRateRequest();
        request.setNewsLink(NEWS_LINK);
        request.setCountryId(COUNTRY_ID);
        request.setPartnerCountryId(PARTNER_COUNTRY_ID);
        request.setProductId(PRODUCT_ID);
        request.setTariffTypeId(TARIFF_TYPE_ID);
        request.setYear(YEAR);
        request.setRate(RATE);
        request.setUnit(UNIT);

        NewsTariffRate savedEntity = new NewsTariffRate();
        savedEntity.setTariffId(1);
        savedEntity.setNewsLink(NEWS_LINK);
        savedEntity.setCountryId(COUNTRY_ID);
        savedEntity.setPartnerCountryId(PARTNER_COUNTRY_ID);
        savedEntity.setProductId(PRODUCT_ID);
        savedEntity.setTariffTypeId(TARIFF_TYPE_ID);
        savedEntity.setYear(YEAR);
        savedEntity.setRate(RATE);
        savedEntity.setUnit(UNIT);
        savedEntity.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        savedEntity.setInEffect(true);

        when(repository.existsByNewsLink(NEWS_LINK)).thenReturn(false);
        when(repository.save(any(NewsTariffRate.class))).thenReturn(savedEntity);

        // When
        NewsTariffRateResponse response = service.createTariffRate(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTariffId()).isEqualTo(1);
        assertThat(response.getNewsLink()).isEqualTo(NEWS_LINK);
        assertThat(response.getCountryId()).isEqualTo(COUNTRY_ID);
        assertThat(response.getPartnerCountryId()).isEqualTo(PARTNER_COUNTRY_ID);
        assertThat(response.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(response.getTariffTypeId()).isEqualTo(TARIFF_TYPE_ID);
        assertThat(response.getYear()).isEqualTo(YEAR);
        assertThat(response.getRate()).isEqualTo(RATE);
        assertThat(response.getUnit()).isEqualTo(UNIT);
        assertThat(response.getInEffect()).isTrue();

        verify(repository).existsByNewsLink(NEWS_LINK);
        verify(repository).save(any(NewsTariffRate.class));
    }

    @Test
    void createTariffRate_alreadyExists_throwsException() {
        // Given
        CreateNewsTariffRateRequest request = new CreateNewsTariffRateRequest();
        request.setNewsLink(NEWS_LINK);
        request.setCountryId(COUNTRY_ID);
        request.setPartnerCountryId(PARTNER_COUNTRY_ID);
        request.setProductId(PRODUCT_ID);
        request.setTariffTypeId(TARIFF_TYPE_ID);
        request.setYear(YEAR);
        request.setRate(RATE);
        request.setUnit(UNIT);

        when(repository.existsByNewsLink(NEWS_LINK)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> service.createTariffRate(request))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("already exists");

        verify(repository).existsByNewsLink(NEWS_LINK);
        verify(repository, never()).save(any());
    }

    @Test
    void getTariffRateByNewsLink_found_returnsResponse() {
        // Given
        NewsTariffRate entity = new NewsTariffRate();
        entity.setTariffId(1);
        entity.setNewsLink(NEWS_LINK);
        entity.setCountryId(COUNTRY_ID);
        entity.setPartnerCountryId(PARTNER_COUNTRY_ID);
        entity.setProductId(PRODUCT_ID);
        entity.setTariffTypeId(TARIFF_TYPE_ID);
        entity.setYear(YEAR);
        entity.setRate(RATE);
        entity.setUnit(UNIT);
        entity.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        entity.setInEffect(true);

        when(repository.findByNewsLink(NEWS_LINK)).thenReturn(Optional.of(entity));

        // When
        NewsTariffRateResponse response = service.getTariffRateByNewsLink(NEWS_LINK);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTariffId()).isEqualTo(1);
        assertThat(response.getNewsLink()).isEqualTo(NEWS_LINK);
        assertThat(response.getRate()).isEqualTo(RATE);

        verify(repository).findByNewsLink(NEWS_LINK);
    }

    @Test
    void getTariffRateByNewsLink_notFound_throwsException() {
        // Given
        when(repository.findByNewsLink(NEWS_LINK)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.getTariffRateByNewsLink(NEWS_LINK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No tariff rate found");

        verify(repository).findByNewsLink(NEWS_LINK);
    }

    @Test
    void existsByNewsLink_returnsTrue() {
        // Given
        when(repository.existsByNewsLink(NEWS_LINK)).thenReturn(true);

        // When
        boolean exists = service.existsByNewsLink(NEWS_LINK);

        // Then
        assertThat(exists).isTrue();
        verify(repository).existsByNewsLink(NEWS_LINK);
    }

    @Test
    void existsByNewsLink_returnsFalse() {
        // Given
        when(repository.existsByNewsLink(NEWS_LINK)).thenReturn(false);

        // When
        boolean exists = service.existsByNewsLink(NEWS_LINK);

        // Then
        assertThat(exists).isFalse();
        verify(repository).existsByNewsLink(NEWS_LINK);
    }
}

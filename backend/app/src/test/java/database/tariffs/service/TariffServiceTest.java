package database.tariffs.service;

import database.tariffs.dto.TariffResponse;
import database.tariffs.entity.TariffRateEntity;
import database.tariffs.exception.TariffNotFoundException;
import database.tariffs.repository.TariffRateRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TariffServiceTest {

  @Mock
  TariffRateRepository repo;

  @InjectMocks
  TariffService service; // works even if TariffService has no explicit constructor

  @Test
  void getTariff_happyPath() {
    // Repository returns ENTITY
    TariffRateEntity e = new TariffRateEntity();
    e.setCountryIsoNumeric("702");
    e.setPartnerIsoNumeric("156");
    e.setProductHsCode(1001);
    e.setYear("2023");
    e.setRate(5.0);
    e.setUnit("percent");
    when(repo.getTariff("702","156",1001,"2023")).thenReturn(e);

    // Service returns DTO
    TariffResponse out = service.getTariff("702","156",1001,"2023");

    // If TariffResponse is a record: out.rate()
    // If it's a POJO: out.getRate()
    double rate = tryGetRate(out);
    assertThat(rate).isEqualTo(5.0);
  }

  @Test
  void deleteTariff_notFound_throws() {
    when(repo.exists("702","156",1001,"2023")).thenReturn(false);
    assertThatThrownBy(() -> service.deleteTariff("702","156",1001,"2023"))
        .isInstanceOf(TariffNotFoundException.class);
  }

  // Small helper so this compiles regardless of record vs getter style
  private double tryGetRate(TariffResponse dto) {
    try {
      // record style: double rate()
      return (double) dto.getClass().getMethod("rate").invoke(dto);
    } catch (Exception ignore) {
      try {
        // bean style: Double getRate()
        Object v = dto.getClass().getMethod("getRate").invoke(dto);
        return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
      } catch (Exception e) {
        throw new AssertionError("TariffResponse has neither rate() nor getRate()", e);
      }
    }
  }
}

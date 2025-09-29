package database;

import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TariffRateService {
    private final TariffRateRepository repo;

    public TariffRateService(TariffRateRepository repo) {
        this.repo = repo;
    }

    /** Returns DB rate as text (so WitsApiService can return a String body easily). */
    public Optional<String> retrieveTariffRateAsText(String reporter, String partner, String product, String year) {
        Double rate = repo.getTariffRate(reporter, partner, product, year); // all Strings
        return Optional.ofNullable(rate).map(String::valueOf);
    }
}

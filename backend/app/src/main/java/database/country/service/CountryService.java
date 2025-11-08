package database.country.service;

import database.country.dto.CountryResponse;
import database.country.entity.Country;
import database.country.repository.CountryRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CountryService {
    private static final Logger logger = LoggerFactory.getLogger(CountryService.class);

    @Autowired
    private CountryRepository repository;

    /**
     * Get all countries
     */
    public List<CountryResponse> getAllCountries() {
        logger.info("Retrieving all countries");

        List<Country> entities = repository.getAllCountries();

        return entities.stream()
                .map(entity -> new CountryResponse(
                    entity.getIsoNumeric(),
                    entity.getCountryName(),
                    entity.getRegion()
                ))
                .collect(Collectors.toList());
    }
}

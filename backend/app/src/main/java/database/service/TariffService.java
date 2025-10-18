package database.service;

import database.TariffRateRepository;
import database.TariffRateEntity;
import database.dto.CreateTariffRequest;
import database.dto.UpdateTariffRequest;
import database.dto.TariffResponse;
import database.exception.TariffAlreadyExistsException;
import database.exception.TariffNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TariffService {
    private static final Logger logger = LoggerFactory.getLogger(TariffService.class);
    
    @Autowired
    private TariffRateRepository repository;
    
    /**
     * Create new tariff - throws TariffAlreadyExistsException if exists
     */
    @Transactional
    public TariffResponse createTariff(CreateTariffRequest request) {
        logger.info("Creating tariff for: reporter={}, partner={}, product={}, year={}",
                request.getReporter(), request.getPartner(), request.getProduct(), request.getYear());
        
        // Check if already exists
        if (repository.exists(request.getReporter(), request.getPartner(), 
                             request.getProduct(), request.getYear())) {
            throw new TariffAlreadyExistsException(
                request.getReporter(), request.getPartner(), 
                request.getProduct(), request.getYear()
            );
        }
        
        // Create new tariff
        repository.create(
            request.getReporter(),
            request.getPartner(),
            request.getProduct(),
            request.getYear(),
            request.getRate(),
            request.getUnit()
        );
        
        logger.info("Successfully created tariff");
        
        return new TariffResponse(
            request.getReporter(),
            request.getPartner(),
            request.getProduct(),
            request.getYear(),
            request.getRate(),
            request.getUnit()
        );
    }
    
    /**
     * Update existing tariff - throws TariffNotFoundException if not exists
     */
    @Transactional
    public TariffResponse updateTariff(String reporter, String partner, 
                                      Integer product, String year,
                                      UpdateTariffRequest request) {
        logger.info("Updating tariff for: reporter={}, partner={}, product={}, year={}",
                reporter, partner, product, year);
        
        // Check if exists
        if (!repository.exists(reporter, partner, product, year)) {
            throw new TariffNotFoundException(reporter, partner, product, year);
        }
        
        // Update tariff
        int rowsUpdated = repository.update(
            reporter, partner, product, year,
            request.getRate(), request.getUnit()
        );
        
        if (rowsUpdated == 0) {
            throw new TariffNotFoundException(reporter, partner, product, year);
        }
        
        logger.info("Successfully updated tariff");
        
        return new TariffResponse(
            reporter, partner, product, year,
            request.getRate(), request.getUnit()
        );
    }
    
    /**
     * Get tariff by composite key
     */
    public TariffResponse getTariff(String reporter, String partner, 
                                   Integer product, String year) {
        logger.info("Retrieving tariff for: reporter={}, partner={}, product={}, year={}",
                reporter, partner, product, year);
        
        TariffRateEntity entity = repository.getTariff(reporter, partner, product, year);
        
        if (entity == null) {
            throw new TariffNotFoundException(reporter, partner, product, year);
        }
        
        return new TariffResponse(
            entity.getCountryIsoNumeric(),
            entity.getPartnerIsoNumeric(),
            entity.getProductHsCode(),
            entity.getYear(),
            entity.getRate(),
            entity.getUnit()
        );
    }
    
    /**
     * Get all tariffs
     */
    public List<TariffResponse> getAllTariffs() {
        logger.info("Retrieving all tariffs");
        
        List<TariffRateEntity> entities = repository.getAllTariffs();
        
        return entities.stream()
                .map(entity -> new TariffResponse(
                    entity.getCountryIsoNumeric(),
                    entity.getPartnerIsoNumeric(),
                    entity.getProductHsCode(),
                    entity.getYear(),
                    entity.getRate(),
                    entity.getUnit()
                ))
                .collect(Collectors.toList());
    }
}
package database.newstariffrates.service;

import database.newstariffrates.dto.CreateNewsTariffRateRequest;
import database.newstariffrates.dto.NewsTariffRateResponse;
import database.newstariffrates.entity.NewsTariffRate;
import database.newstariffrates.repository.NewsTariffRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NewsTariffRateService {

    private static final Logger logger = LoggerFactory.getLogger(NewsTariffRateService.class);

    @Autowired
    private NewsTariffRateRepository repository;

    /**
     * Create a new tariff rate from news article
     * Enforces one-to-one relationship: one news link = one tariff rate
     */
    @Transactional
    public NewsTariffRateResponse createTariffRate(CreateNewsTariffRateRequest request) {
        logger.info("Creating tariff rate for news: {}", request.getNewsLink());

        // Check if tariff rate already exists for this news link
        if (repository.existsByNewsLink(request.getNewsLink())) {
            throw new DataIntegrityViolationException(
                "A tariff rate already exists for this news source. Each news article can only have one tariff rate entry."
            );
        }

        // Create new entity
        NewsTariffRate entity = new NewsTariffRate();
        entity.setNewsLink(request.getNewsLink());
        entity.setCountryId(request.getCountryId());
        entity.setPartnerCountryId(request.getPartnerCountryId());
        entity.setProductId(request.getProductId());
        entity.setTariffTypeId(request.getTariffTypeId());
        entity.setYear(request.getYear());
        entity.setRate(request.getRate());
        entity.setUnit(request.getUnit());

        // Save to database
        NewsTariffRate saved = repository.save(entity);

        logger.info("Tariff rate created successfully with ID: {}", saved.getId());

        // Convert to response DTO
        return convertToResponse(saved);
    }

    /**
     * Get tariff rate by news link
     */
    public NewsTariffRateResponse getTariffRateByNewsLink(String newsLink) {
        logger.info("Fetching tariff rate for news: {}", newsLink);

        NewsTariffRate entity = repository.findByNewsLink(newsLink)
            .orElseThrow(() -> new IllegalArgumentException("No tariff rate found for news: " + newsLink));

        return convertToResponse(entity);
    }

    /**
     * Check if tariff rate exists for a news link
     */
    public boolean existsByNewsLink(String newsLink) {
        return repository.existsByNewsLink(newsLink);
    }

    /**
     * Convert entity to response DTO
     */
    private NewsTariffRateResponse convertToResponse(NewsTariffRate entity) {
        NewsTariffRateResponse response = new NewsTariffRateResponse();
        response.setId(entity.getId());
        response.setNewsLink(entity.getNewsLink());
        response.setCountryId(entity.getCountryId());
        response.setPartnerCountryId(entity.getPartnerCountryId());
        response.setProductId(entity.getProductId());
        response.setTariffTypeId(entity.getTariffTypeId());
        response.setYear(entity.getYear());
        response.setRate(entity.getRate());
        response.setUnit(entity.getUnit());
        response.setLastUpdated(entity.getLastUpdated());
        response.setInEffect(entity.getInEffect());
        return response;
    }
}

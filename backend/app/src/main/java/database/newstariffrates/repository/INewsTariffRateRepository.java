package database.newstariffrates.repository;

import database.newstariffrates.entity.NewsTariffRate;
import java.util.Optional;

/**
 * Repository interface for NewsTariffRate operations.
 * Follows Dependency Inversion Principle - high-level modules depend on this abstraction.
 */
public interface INewsTariffRateRepository {

    /**
     * Check if a tariff rate already exists for a given news link
     */
    boolean existsByNewsLink(String newsLink);

    /**
     * Find tariff rate by news link
     */
    Optional<NewsTariffRate> findByNewsLink(String newsLink);

    /**
     * Save new tariff rate
     */
    NewsTariffRate save(NewsTariffRate tariffRate);
}

package database.tariffs.repository;

import database.tariffs.entity.TariffRateEntity;
import java.util.List;

/**
 * Repository interface for TariffRate operations.
 * Follows Dependency Inversion Principle - high-level modules depend on this abstraction.
 */
public interface ITariffRateRepository {

    /**
     * Check if tariff exists by composite key
     */
    boolean exists(String reporter, String partner, Integer product, String year);

    /**
     * Get tariff by composite key
     * @return TariffRateEntity or null if not found
     */
    TariffRateEntity getTariff(String reporter, String partner, Integer product, String year);

    /**
     * Get all tariffs
     */
    List<TariffRateEntity> getAllTariffs();

    /**
     * Create new tariff
     */
    void create(String reporter, String partner, Integer product, String year,
                Double rate, String unit);

    /**
     * Update existing tariff
     * @return number of rows updated
     */
    int update(String reporter, String partner, Integer product, String year,
               Double rate, String unit);

    /**
     * Delete tariff by composite key
     * @return number of rows deleted
     */
    int delete(String reporter, String partner, Integer product, String year);
}

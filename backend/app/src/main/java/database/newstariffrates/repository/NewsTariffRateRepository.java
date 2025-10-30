package database.newstariffrates.repository;

import database.newstariffrates.entity.NewsTariffRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewsTariffRateRepository extends JpaRepository<NewsTariffRate, Long> {

    /**
     * Check if a tariff rate already exists for a given news link
     * Ensures one news source = one tariff rate constraint
     */
    boolean existsByNewsLink(String newsLink);

    /**
     * Find tariff rate by news link
     */
    Optional<NewsTariffRate> findByNewsLink(String newsLink);
}

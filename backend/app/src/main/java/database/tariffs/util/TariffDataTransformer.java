package database.tariffs.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for transforming tariff data.
 * Follows Single Responsibility Principle - only handles data transformation.
 */
public class TariffDataTransformer {

    private static final String DEFAULT_UNIT = "percent";
    private static final int MAX_UNIT_LENGTH = 20;
    private static final int RATE_DECIMAL_PLACES = 3;

    /**
     * Round rate to 3 decimal places for DECIMAL(6,3) database column
     */
    public static BigDecimal roundRate(Double rate) {
        if (rate == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(rate).setScale(RATE_DECIMAL_PLACES, RoundingMode.HALF_UP);
    }

    /**
     * Normalize unit value - apply default and truncate if needed
     */
    public static String normalizeUnit(String unit) {
        if (unit == null || unit.trim().isEmpty()) {
            return DEFAULT_UNIT;
        }

        if (unit.length() > MAX_UNIT_LENGTH) {
            return unit.substring(0, MAX_UNIT_LENGTH);
        }

        return unit;
    }

    /**
     * Convert year string to Integer
     */
    public static Integer parseYear(String year) {
        if (year == null || year.trim().isEmpty()) {
            throw new IllegalArgumentException("Year cannot be null or empty");
        }
        return Integer.valueOf(year);
    }
}

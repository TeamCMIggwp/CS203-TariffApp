package database.country.entity;

public class Country {

    private String isoNumeric;
    private String countryName;
    private String region;
    private java.sql.Timestamp lastUpdated;

    // Getters and Setters
    public String getIsoNumeric() {
        return isoNumeric;
    }

    public void setIsoNumeric(String isoNumeric) {
        this.isoNumeric = isoNumeric;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public java.sql.Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(java.sql.Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "Country{" +
                "isoNumeric='" + isoNumeric + '\'' +
                ", countryName='" + countryName + '\'' +
                ", region='" + region + '\'' +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}

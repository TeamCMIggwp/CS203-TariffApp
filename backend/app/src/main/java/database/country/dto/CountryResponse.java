package database.country.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CountryResponse {

    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("region")
    private String region;

    public CountryResponse(String code, String name, String region) {
        this.code = code;
        this.name = name;
        this.region = region;
    }

    // Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}

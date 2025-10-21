package wto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WtoTariffRateResponse {
    
    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("categoryCode")
    private String categoryCode;

    @JsonProperty("categoryLabel")
    private String categoryLabel;

    @JsonProperty("subcategoryCode")
    private String subcategoryCode;

    @JsonProperty("subcategoryLabel")
    private String subcategoryLabel;

    @JsonProperty("unitCode")
    private String unitCode;

    @JsonProperty("unitLabel")
    private String unitLabel;

    @JsonProperty("startYear")
    private Integer startYear;

    @JsonProperty("endYear")
    private Integer endYear;

    @JsonProperty("frequencyCode")
    private String frequencyCode;

    @JsonProperty("frequencyLabel")
    private String frequencyLabel;

    @JsonProperty("numberReporters")
    private Integer numberReporters;

    @JsonProperty("numberPartners")
    private Integer numberPartners;

    @JsonProperty("productSectorClassificationCode")
    private String productSectorClassificationCode;

    @JsonProperty("productSectorClassificationLabel")
    private String productSectorClassificationLabel;

    @JsonProperty("hasMetadata")
    private String hasMetadata;

    @JsonProperty("numberDecimals")
    private Integer numberDecimals;

    @JsonProperty("numberDatapoints")
    private Long numberDatapoints;

    @JsonProperty("updateFrequency")
    private String updateFrequency;

    @JsonProperty("description")
    private String description;

    @JsonProperty("sortOrder")
    private Integer sortOrder;

    // Getters and setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }

    public String getCategoryLabel() { return categoryLabel; }
    public void setCategoryLabel(String categoryLabel) { this.categoryLabel = categoryLabel; }

    public String getSubcategoryCode() { return subcategoryCode; }
    public void setSubcategoryCode(String subcategoryCode) { this.subcategoryCode = subcategoryCode; }

    public String getSubcategoryLabel() { return subcategoryLabel; }
    public void setSubcategoryLabel(String subcategoryLabel) { this.subcategoryLabel = subcategoryLabel; }

    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }

    public String getUnitLabel() { return unitLabel; }
    public void setUnitLabel(String unitLabel) { this.unitLabel = unitLabel; }

    public Integer getStartYear() { return startYear; }
    public void setStartYear(Integer startYear) { this.startYear = startYear; }

    public Integer getEndYear() { return endYear; }
    public void setEndYear(Integer endYear) { this.endYear = endYear; }

    public String getFrequencyCode() { return frequencyCode; }
    public void setFrequencyCode(String frequencyCode) { this.frequencyCode = frequencyCode; }

    public String getFrequencyLabel() { return frequencyLabel; }
    public void setFrequencyLabel(String frequencyLabel) { this.frequencyLabel = frequencyLabel; }

    public Integer getNumberReporters() { return numberReporters; }
    public void setNumberReporters(Integer numberReporters) { this.numberReporters = numberReporters; }

    public Integer getNumberPartners() { return numberPartners; }
    public void setNumberPartners(Integer numberPartners) { this.numberPartners = numberPartners; }

    public String getProductSectorClassificationCode() { return productSectorClassificationCode; }
    public void setProductSectorClassificationCode(String productSectorClassificationCode) { this.productSectorClassificationCode = productSectorClassificationCode; }

    public String getProductSectorClassificationLabel() { return productSectorClassificationLabel; }
    public void setProductSectorClassificationLabel(String productSectorClassificationLabel) { this.productSectorClassificationLabel = productSectorClassificationLabel; }

    public String getHasMetadata() { return hasMetadata; }
    public void setHasMetadata(String hasMetadata) { this.hasMetadata = hasMetadata; }

    public Integer getNumberDecimals() { return numberDecimals; }
    public void setNumberDecimals(Integer numberDecimals) { this.numberDecimals = numberDecimals; }

    public Long getNumberDatapoints() { return numberDatapoints; }
    public void setNumberDatapoints(Long numberDatapoints) { this.numberDatapoints = numberDatapoints; }

    public String getUpdateFrequency() { return updateFrequency; }
    public void setUpdateFrequency(String updateFrequency) { this.updateFrequency = updateFrequency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    @Override
    public String toString() {
        return "WTORateResponse{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", categoryCode='" + categoryCode + '\'' +
                ", categoryLabel='" + categoryLabel + '\'' +
                ", subcategoryCode='" + subcategoryCode + '\'' +
                ", subcategoryLabel='" + subcategoryLabel + '\'' +
                ", unitCode='" + unitCode + '\'' +
                ", unitLabel='" + unitLabel + '\'' +
                ", startYear=" + startYear +
                ", endYear=" + endYear +
                ", frequencyCode='" + frequencyCode + '\'' +
                ", frequencyLabel='" + frequencyLabel + '\'' +
                ", numberReporters=" + numberReporters +
                ", numberPartners=" + numberPartners +
                ", productSectorClassificationCode='" + productSectorClassificationCode + '\'' +
                ", productSectorClassificationLabel='" + productSectorClassificationLabel + '\'' +
                ", hasMetadata='" + hasMetadata + '\'' +
                ", numberDecimals=" + numberDecimals +
                ", numberDatapoints=" + numberDatapoints +
                ", updateFrequency='" + updateFrequency + '\'' +
                ", description='" + description + '\'' +
                ", sortOrder=" + sortOrder +
                '}';
    }
}

package org.base.mobile.dto;

public class RegionalMapDTO {
    private String region;
    private String brand;
    private String quarter;
    private String yearOfProduction;
    private Integer quantity;
    private RegionClDTO RegionCl;

    // Getters and Setters
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getQuarter() { return quarter; }
    public void setQuarter(String quarter) { this.quarter = quarter; }
    public String getYearOfProduction() { return yearOfProduction; }
    public void setYearOfProduction(String yearOfProduction) { this.yearOfProduction = yearOfProduction; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public RegionClDTO getRegionCl() { return RegionCl; }
    public void setRegionCl(RegionClDTO regionCl) { this.RegionCl = regionCl; }
}

package com.example.demo.dto;
public class ClientProfileDTO {
    private String mainokvedCode;
    private String mainokvedName;
    private Boolean hasCredits;
    private Boolean hasVed;
    private String vedCountry;
    private Long revenue;
    private String groupId;

    public String getMainokvedCode() {
        return mainokvedCode;
    }

    public void setMainokvedCode(String mainokvedCode) {
        this.mainokvedCode = mainokvedCode;
    }

    public String getMainokvedName() {
        return mainokvedName;
    }

    public void setMainokvedName(String mainokvedName) {
        this.mainokvedName = mainokvedName;
    }

    public Boolean getHasCredits() {
        return hasCredits;
    }

    public void setHasCredits(Boolean hasCredits) {
        this.hasCredits = hasCredits;
    }

    public Boolean getHasVed() {
        return hasVed;
    }

    public void setHasVed(Boolean hasVed) {
        this.hasVed = hasVed;
    }

    public String getVedCountry() {
        return vedCountry;
    }

    public void setVedCountry(String vedCountry) {
        this.vedCountry = vedCountry;
    }

    public Long getRevenue() {
        return revenue;
    }

    public void setRevenue(Long revenue) {
        this.revenue = revenue;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
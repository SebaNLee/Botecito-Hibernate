package ar.edu.itba.paw.models;

import java.math.BigDecimal;

public class ItemSearchCriteria {
    private String searchQuery;
    private Integer locationOptionId;
    private String date;
    private String startTime;
    private String endTime;
    private Integer capacity;
    private BigDecimal maxWeightKg;
    private String sort;

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(final String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public Integer getLocationOptionId() {
        return locationOptionId;
    }

    public void setLocationOptionId(final Integer locationOptionId) {
        this.locationOptionId = locationOptionId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(final String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(final String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(final String endTime) {
        this.endTime = endTime;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(final Integer capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getMaxWeightKg() {
        return maxWeightKg;
    }

    public void setMaxWeightKg(final BigDecimal maxWeightKg) {
        this.maxWeightKg = maxWeightKg;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(final String sort) {
        this.sort = sort;
    }
}

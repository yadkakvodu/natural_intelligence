package com.example.demo.dto;

public class ParamsDTO {
    private Integer limit;
    private Integer offset;
    private String topicFilter;
    private Boolean isAnalyticsOnly;
    private String newsId;
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getAnalyticsOnly() {
        return isAnalyticsOnly;
    }

    public void setAnalyticsOnly(Boolean analyticsOnly) {
        isAnalyticsOnly = analyticsOnly;
    }

    public String getNewsId() {
        return newsId;
    }

    public void setNewsId(String newsId) {
        this.newsId = newsId;
    }

    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }

    public Integer getOffset() { return offset; }
    public void setOffset(Integer offset) { this.offset = offset; }

    public String getTopicFilter() { return topicFilter; }
    public void setTopicFilter(String topicFilter) { this.topicFilter = topicFilter; }

    public Boolean getIsAnalyticsOnly() { return isAnalyticsOnly; }
    public void setIsAnalyticsOnly(Boolean isAnalyticsOnly) { this.isAnalyticsOnly = isAnalyticsOnly; }
}
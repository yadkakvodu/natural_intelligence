package com.example.demo.dto;

import java.util.List;

public class RawInsightDTO {
    private String newsId;
    private String title;
    private String summary;
    private String publishedAt;
    private String source;
    private Boolean isAnalytics;
    private Double relevanceScore;
    private List<SemanticTag> semanticTags;
    private List<Source> sources;

    public String getNewsId() { return newsId; }
    public void setNewsId(String newsId) { this.newsId = newsId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getPublishedAt() { return publishedAt; }
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Boolean getIsAnalytics() { return isAnalytics; }
    public void setIsAnalytics(Boolean isAnalytics) { this.isAnalytics = isAnalytics; }

    public Double getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(Double relevanceScore) { this.relevanceScore = relevanceScore; }

    public List<SemanticTag> getSemanticTags() { return semanticTags; }
    public void setSemanticTags(List<SemanticTag> semanticTags) { this.semanticTags = semanticTags; }

    public List<Source> getSources() { return sources; }
    public void setSources(List<Source> sources) { this.sources = sources; }
}
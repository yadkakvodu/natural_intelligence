package com.example.demo.dto;

import java.util.List;

public class PythonInsightResponseDTO {
    private String newsId;
    private String title;
    private String summary;
    private String publishedAt;
    private String source;
    private Boolean isAnalytics;
    private Double relevanceScore;

    // Специфичные поля для детального инсайта
    private String whatHappened;
    private String whyImportantForClient;
    private List<String> risks;
    private List<String> opportunities;
    private String bankPosition;

    private List<SemanticTag> semanticTags;
    private List<Source> sources;

    // --- ГЕТТЕРЫ И СЕТТЕРЫ ---

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

    // ВОТ ОН - НУЖНЫЙ ГЕТТЕР
    public Boolean getIsAnalytics() { return isAnalytics; }
    public void setIsAnalytics(Boolean isAnalytics) { this.isAnalytics = isAnalytics; }

    public Double getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(Double relevanceScore) { this.relevanceScore = relevanceScore; }

    public String getWhatHappened() { return whatHappened; }
    public void setWhatHappened(String whatHappened) { this.whatHappened = whatHappened; }

    public String getWhyImportantForClient() { return whyImportantForClient; }
    public void setWhyImportantForClient(String whyImportantForClient) { this.whyImportantForClient = whyImportantForClient; }

    public List<String> getRisks() { return risks; }
    public void setRisks(List<String> risks) { this.risks = risks; }

    public List<String> getOpportunities() { return opportunities; }
    public void setOpportunities(List<String> opportunities) { this.opportunities = opportunities; }

    public String getBankPosition() { return bankPosition; }
    public void setBankPosition(String bankPosition) { this.bankPosition = bankPosition; }

    public List<SemanticTag> getSemanticTags() { return semanticTags; }
    public void setSemanticTags(List<SemanticTag> semanticTags) { this.semanticTags = semanticTags; }

    public List<Source> getSources() { return sources; }
    public void setSources(List<Source> sources) { this.sources = sources; }
}
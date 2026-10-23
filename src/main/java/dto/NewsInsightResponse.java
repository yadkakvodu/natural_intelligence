package dto;

import java.util.List;

public class NewsInsightResponse {
    private String insightId;          // "insight_id"
    private String newsId;             // "news_id"
    private String title;
    private String summary;
    private String publishedAt;        // "published_at"
    private String source;
    private Boolean isAnalytics;       // "is_analytics"
    private Double relevanceScore;     // "relevance_score"
    private String relevanceLevel;     // "relevance_level"

    private String whatHappened;       // "what_happened"
    private String whyImportantForClient; // "why_important_for_client"

    private List<String> risks;        // Массив строк
    private List<String> opportunities;// Массив строк

    private String bankPosition;       // "bank_position"

    private List<SemanticTag> semanticTags; // "semantic_tags"
    private List<Source> sources;

    public String getInsightId() {
        return insightId;
    }
    public void setInsightId(String insightId) {
        this.insightId = insightId;
    }

    public String getNewsId() {
        return newsId;
    }

    public void setNewsId(String newsId) {
        this.newsId = newsId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Boolean getAnalytics() {
        return isAnalytics;
    }

    public void setAnalytics(Boolean analytics) {
        isAnalytics = analytics;
    }

    public Double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public String getRelevanceLevel() {
        return relevanceLevel;
    }

    public void setRelevanceLevel(String relevanceLevel) {
        this.relevanceLevel = relevanceLevel;
    }

    public String getWhatHappened() {
        return whatHappened;
    }

    public void setWhatHappened(String whatHappened) {
        this.whatHappened = whatHappened;
    }

    public String getWhyImportantForClient() {
        return whyImportantForClient;
    }

    public void setWhyImportantForClient(String whyImportantForClient) {
        this.whyImportantForClient = whyImportantForClient;
    }

    public List<String> getRisks() {
        return risks;
    }

    public void setRisks(List<String> risks) {
        this.risks = risks;
    }

    public List<String> getOpportunities() {
        return opportunities;
    }

    public void setOpportunities(List<String> opportunities) {
        this.opportunities = opportunities;
    }

    public String getBankPosition() {
        return bankPosition;
    }

    public void setBankPosition(String bankPosition) {
        this.bankPosition = bankPosition;
    }

    public List<SemanticTag> getSemanticTags() {
        return semanticTags;
    }

    public void setSemanticTags(List<SemanticTag> semanticTags) {
        this.semanticTags = semanticTags;
    }

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }
}

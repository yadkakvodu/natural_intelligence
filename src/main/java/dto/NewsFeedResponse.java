package dto;


import java.util.List;

public class NewsFeedResponse {

    private Pagination pagination;
    private List<InsightItem> items;



    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public List<InsightItem> getItems() {
        return items;
    }

    public void setItems(List<InsightItem> items) {
        this.items = items;
    }
}

class Pagination {
    private int limit;
    private int offset;
    private long count;
    // геттеры/

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}

class InsightItem {
    private String insightId;
    private String newsId;
    private String title;
    private String summary;
    private String publishedAt;
    private String source;
    private Boolean isAnalytics;
    private Double relevanceScore;
    private String relevanceLevel;
    private List<SemanticTag> semanticTags;
    private List<Source> sources;
    // геттеры/сеттеры

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

class Source {}
class SemanticTag {}

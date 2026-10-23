package entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "news")
public class News {

    @Id
    @Column(name = "news_id")
    private String newsId;

    private String title;
    private String summary;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    private String source;

    @Column(name = "is_analytics")
    private Boolean isAnalytics;

    @Column(name = "body_md")
    private String bodyMd;


    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private String embedding;

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

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
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

    public String getBodyMd() {
        return bodyMd;
    }

    public void setBodyMd(String bodyMd) {
        this.bodyMd = bodyMd;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }
}
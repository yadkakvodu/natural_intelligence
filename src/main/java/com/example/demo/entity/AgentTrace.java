package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "agent_traces")
public class AgentTrace {
    @Id
    @Column(name = "trace_id")
    private UUID traceId;

    @Column(name = "synthetic_u")
    private String syntheticU;

    @Column(name = "news_id")
    private String newsId;

    @Column(name = "request_type")
    private String requestType;

    @Column(name = "steps", columnDefinition = "jsonb")
    private String steps; // Hibernate не умеет напрямую в jsonb, работаем через строку

    @Column(name = "llm_model_used")
    private String llmModelUsed;

    @Column(name = "tokens_input")
    private Integer tokensInput;

    @Column(name = "tokens_output")
    private Integer tokensOutput;

    @Column(name = "relevance_score")
    private Double relevanceScore;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public UUID getTraceId() {
        return traceId;
    }

    public void setTraceId(UUID traceId) {
        this.traceId = traceId;
    }

    public String getSyntheticU() {
        return syntheticU;
    }

    public void setSyntheticU(String syntheticU) {
        this.syntheticU = syntheticU;
    }

    public String getNewsId() {
        return newsId;
    }

    public void setNewsId(String newsId) {
        this.newsId = newsId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public String getLlmModelUsed() {
        return llmModelUsed;
    }

    public void setLlmModelUsed(String llmModelUsed) {
        this.llmModelUsed = llmModelUsed;
    }

    public Integer getTokensInput() {
        return tokensInput;
    }

    public void setTokensInput(Integer tokensInput) {
        this.tokensInput = tokensInput;
    }

    public Integer getTokensOutput() {
        return tokensOutput;
    }

    public void setTokensOutput(Integer tokensOutput) {
        this.tokensOutput = tokensOutput;
    }

    public Double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
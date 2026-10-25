package com.example.demo.dto;

import java.util.List;

public class ChatResponseDTO {
    private String messageId;
    private String role;
    private String content;
    private String createdAt;
    private List<Source> citations;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<Source> getCitations() {
        return citations;
    }

    public void setCitations(List<Source> citations) {
        this.citations = citations;
    }
}
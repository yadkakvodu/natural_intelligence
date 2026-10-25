package com.example.demo.dto;

import java.util.List;

public class LlmRequestDTO {
    private List<LlmMessageDTO> messages;
    private Double temperature = 0.3;
    private Integer maxTokens = 500;

    public List<LlmMessageDTO> getMessages() { return messages; }
    public void setMessages(List<LlmMessageDTO> messages) { this.messages = messages; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
}
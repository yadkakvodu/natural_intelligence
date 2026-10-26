package com.example.demo.dto;

import java.util.List;

public class PythonResponseDTO {
    private int totalFound;
    private List<RawInsightDTO> items;
    private Integer tokensInput;
    private Integer tokensOutput;
    private Integer processingTimeMs;

    public int getTotalFound() {
        return totalFound;
    }

    public void setTotalFound(int totalFound) {
        this.totalFound = totalFound;
    }

    public List<RawInsightDTO> getItems() {
        return items;
    }

    public void setItems(List<RawInsightDTO> items) {
        this.items = items;
    }

    public int getTokensInput() {
        return tokensInput;
    }

    public void setTokensInput(int tokensInput) {
        this.tokensInput = tokensInput;
    }

    public int getTokensOutput() {
        return tokensOutput;
    }

    public void setTokensOutput(int tokensOutput) {
        this.tokensOutput = tokensOutput;
    }

    public int getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(int processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
}


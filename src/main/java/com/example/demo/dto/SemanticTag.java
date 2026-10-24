package com.example.demo.dto;

public class SemanticTag {
    private String tag;
    private String value;
    private String label;
    private Double influenceWeight;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Double getInfluenceWeight() {
        return influenceWeight;
    }

    public void setInfluenceWeight(Double influenceWeight) {
        this.influenceWeight = influenceWeight;
    }
}

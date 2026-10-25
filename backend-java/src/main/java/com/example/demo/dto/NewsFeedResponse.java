package com.example.demo.dto;


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


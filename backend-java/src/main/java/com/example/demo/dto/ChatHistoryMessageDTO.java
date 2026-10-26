package com.example.demo.dto;

import java.util.List;

public class ChatHistoryMessageDTO {

    Pagination pagination;

    public List<ChatHistoryDTO> getItems() {
        return items;
    }

    public void setItems(List<ChatHistoryDTO> items) {
        this.items = items;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    List<ChatHistoryDTO> items;

}

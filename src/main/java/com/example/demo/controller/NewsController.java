package com.example.demo.controller;

import com.example.demo.dto.ChatRequestDTO;
import com.example.demo.dto.ChatResponseDTO;
import com.example.demo.dto.NewsFeedResponse;
import com.example.demo.dto.NewsInsightResponse;
import com.example.demo.service.NewsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients/{synthetic_u}")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/news-feed")
    public ResponseEntity<NewsFeedResponse> getNewsFeed(
            @PathVariable("synthetic_u") String syntheticU,
            @RequestParam(value = "limit", defaultValue = "5") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "order", defaultValue = "relevance_score,desc") String order,
            @RequestParam(value = "is_analytics_only", required = false) Boolean isAnalyticsOnly,
            @RequestParam(value = "topic", required = false) String topic
    ) {
        if (!syntheticU.matches("^U_[A-Z0-9]+$")) {
            throw new IllegalArgumentException("Неверный формат synthetic_u. Ожидается строка вида 'U_...'");
        }
        if (limit < 1 || limit > 20) {
            throw new IllegalArgumentException("limit должен быть от 1 до 20");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset не может быть отрицательным");
        }

        List<String> allowedOrders = List.of("published_at,desc", "relevance,desc");
        if (!allowedOrders.contains(order)) {
            throw new IllegalArgumentException("Неверный формат order. Допустимо: " + allowedOrders);
        }

        if (topic != null) {
            List<String> allowedTopics = List.of("cbr_rate", "ved_china", "oil", "macro", "taxes");
            if (!allowedTopics.contains(topic)) {
                throw new IllegalArgumentException("Неверный topic. Допустимо: " + allowedTopics);
            }
        }

        NewsFeedResponse response = newsService.generateNewsFeed(syntheticU, limit, offset, order, isAnalyticsOnly, topic);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/news/{news_id}/insight")
    public ResponseEntity<NewsInsightResponse> getPersonalizedBusinessInsight(

            @PathVariable("synthetic_u") String syntheticU,
            @PathVariable("news_id") String newsId

    ) {

        if (!syntheticU.matches("^U_[A-Z0-9]+$")) {
            throw new IllegalArgumentException("Неверный формат synthetic_u. Ожидается строка вида 'U_...'");
        }
        if (newsId == null || newsId.isBlank() || !newsId.matches("^[a-z0-9-]+$")) {
            throw new IllegalArgumentException("Неверный формат news_id. Ожидается строка вида 'alfa-macro-2026-09-07'");
        }

        NewsInsightResponse response = newsService.generateBusinessInsight(syntheticU, newsId);
        return ResponseEntity.ok(response);

    }

    @PostMapping("/news/{news_id}/chat")
    public ResponseEntity<ChatResponseDTO> chatWithAi(
            @PathVariable("synthetic_u") String syntheticU,
            @PathVariable("news_id") String newsId,
            @RequestBody ChatRequestDTO request
    ) {
        if (!syntheticU.matches("^U_[A-Z0-9]+$")) {
            throw new IllegalArgumentException("Неверный формат synthetic_u");
        }
        if (newsId == null || newsId.isBlank()) {
            throw new IllegalArgumentException("Неверный формат news_id");
        }
        if (request.getMessage() == null || request.getMessage().isBlank() || request.getMessage().length() > 1000) {
            throw new IllegalArgumentException("Поле message обязательно и не может быть пустым (макс 1000 символов)");
        }

        ChatResponseDTO response = newsService.generateChatAnswer(syntheticU, newsId, request.getMessage());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
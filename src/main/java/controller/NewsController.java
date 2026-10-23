package controller;

import dto.NewsFeedResponse;
import service.NewsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        if (!syntheticU.startsWith("U_")) {
            throw new IllegalArgumentException("Неверный формат synthetic_u. Ожидается строка вида 'U_...'");
        }
        if (limit < 1 || limit > 20) {
            throw new IllegalArgumentException("limit должен быть от 1 до 20");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset не может быть отрицательным");
        }

        NewsFeedResponse response = newsService.generateNewsFeed(syntheticU, limit, offset, order, isAnalyticsOnly, topic);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/news/{news_id}/insight")
    public ResponseEntity<NewsFeedResponse> getPersonalizedBusinessInsight(
        @PathVariable("synthetic_u") String synthetic_u,
        @PathVariable("news_id") String newsId
    ) {





    }

}
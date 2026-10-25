package com.example.demo.client;

import com.example.demo.dto.ChatResponseDTO;
import com.example.demo.dto.PythonInsightResponseDTO;
import com.example.demo.dto.PythonRequestDTO;
import com.example.demo.dto.PythonResponseDTO;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class PythonClient {

    private final RestClient restClient;

    public PythonClient() {
        // 1. Настраиваем таймауты согласно ТЗ (п. 5)
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(60)); // <--- УВЕЛИЧЬТЕ ДО 60

        // 2. Настраиваем базовый URL.
        // ВАЖНО: Когда друг даст вам IP, вы замените строку ниже.
        // Пока оставляем localhost:8000, чтобы вы могли запустить Python у себя для теста.
        String pythonBaseUrl = "http://localhost:8000";

        this.restClient = RestClient.builder()
                .baseUrl(pythonBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public PythonResponseDTO generateFeed(PythonRequestDTO request, String traceId) {
        return restClient.post()
                .uri("/internal/v1/generate") // Путь из ТЗ (п.5)
                .header("X-Trace-Id", traceId) // Сквозная трассировка (п.7)
                .body(request)
                .retrieve()
                .body(PythonResponseDTO.class);
    }

    public PythonInsightResponseDTO generateInsight(PythonRequestDTO request, String traceId) {
        return restClient.post()
                .uri("/internal/v1/generate")
                .header("X-Trace-Id", traceId)
                .body(request)
                .retrieve()
                .body(PythonInsightResponseDTO.class);
    }

    public ChatResponseDTO generateChatAnswer(PythonRequestDTO request, String traceId) {
        return restClient.post()
                .uri("/internal/v1/generate")
                .header("X-Trace-Id", traceId)
                .body(request)
                .retrieve()
                .body(ChatResponseDTO.class);
    }
}

//package com.example.demo.client;
//
//import com.example.demo.dto.*;
//import org.springframework.stereotype.Component;
//import java.util.List;
//
//@Component
//public class PythonClient {
//
//    // --- ЗАГЛУШКА ДЛЯ 1-ГО ЭНДПОИНТА (news-feed) ---
//    public PythonResponseDTO generateFeed(PythonRequestDTO request, String traceId) {
//        System.out.println("=== MOCK: Вызов Python для FEED ===");
//        System.out.println("Клиент: " + request.getSyntheticU());
//
//        PythonResponseDTO mockResponse = new PythonResponseDTO();
//        mockResponse.setTotalFound(1);
//
//        RawInsightDTO fakeRaw = new RawInsightDTO();
//        fakeRaw.setNewsId("fake-news-123");
//        fakeRaw.setTitle("Тестовая новость (Python заглушка)");
//        fakeRaw.setSummary("Этот текст сгенерирован в Java-заглушке.");
//        fakeRaw.setPublishedAt("2026-10-24T10:00:00Z");
//        fakeRaw.setSource("Альфа-Банк");
//        fakeRaw.setIsAnalytics(true);
//        fakeRaw.setRelevanceScore(0.95);
//
//        mockResponse.setItems(List.of(fakeRaw));
//        return mockResponse;
//    }
//
//    // --- ЗАГЛУШКА ДЛЯ 2-ГО ЭНДПОИНТА (insight) ---
//    public PythonInsightResponseDTO generateInsight(PythonRequestDTO request, String traceId) {
//        System.out.println("=== MOCK: Вызов Python для INSIGHT ===");
//
//        PythonInsightResponseDTO mock = new PythonInsightResponseDTO();
//        mock.setNewsId(request.getParams().getNewsId());
//        mock.setTitle("Детальный разбор (Python заглушка)");
//        mock.setSummary("Тестовое описание");
//        mock.setWhatHappened("ЦБ сохранил ставку.");
//        mock.setWhyImportantForClient("У вас есть кредиты, поэтому это важно.");
//        mock.setRisks(List.of("Риск 1", "Риск 2"));
//        mock.setOpportunities(List.of("Возможность 1"));
//        mock.setBankPosition("Позиция банка: ждите.");
//        mock.setRelevanceScore(0.95);
//
//        return mock;
//    }
//
//    // --- ЗАГЛУШКА ДЛЯ 3-ГО ЭНДПОИНТА (chat) ---
//    public ChatResponseDTO generateChatAnswer(PythonRequestDTO request, String traceId) {
//        System.out.println("=== MOCK: Вызов Python для CHAT ===");
//
//        ChatResponseDTO mock = new ChatResponseDTO();
//        mock.setMessageId("mock-msg-123");
//        mock.setRole("assistant");
//        mock.setContent("Это тестовый ответ от Java-заглушки. Питон еще не подключен.");
//        mock.setCreatedAt("2026-10-24T10:00:00Z");
//
//        return mock;
//    }
//}
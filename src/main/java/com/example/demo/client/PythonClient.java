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
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

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
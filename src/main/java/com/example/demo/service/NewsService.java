package com.example.demo.service;

import com.example.demo.client.PythonClient;
import com.example.demo.dto.*;
import com.example.demo.entity.Client;
import com.example.demo.entity.News;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class NewsService {

    private final ClientRepository clientRepository;
    private final NewsRepository newsRepository;
    private final PythonClient pythonClient; // <-- ДОБАВЛЕНО

    // <-- ДОБАВЛЕНО В КОНСТРУКТОР
    public NewsService(ClientRepository clientRepository, NewsRepository newsRepository, PythonClient pythonClient) {
        this.clientRepository = clientRepository;
        this.newsRepository = newsRepository;
        this.pythonClient = pythonClient;
    }

    public NewsFeedResponse generateNewsFeed(
            String syntheticU, int limit, int offset, String order, Boolean isAnalyticsOnly, String topic
    ) {
        // 1. Достаем клиента из базы
        Client client = clientRepository.findById(syntheticU)
                .orElseThrow(() -> new RuntimeException("Компания с идентификатором " + syntheticU + " не найдена"));

        // 2. Собираем профиль (7 полей)
        ClientProfileDTO profile = new ClientProfileDTO();
        profile.setMainokvedCode(client.getMainOkvedCode());
        profile.setMainokvedName(client.getMainOkvedCode());
        profile.setHasCredits(Boolean.TRUE.equals(client.getClProductLoaMark()));
        profile.setHasVed(Boolean.TRUE.equals(client.getFtsFlag()));
        profile.setVedCountry(client.getFtsCountryName());
        profile.setRevenue(client.getRevenueCompany() != null ? client.getRevenueCompany().longValue() : 0L);
        profile.setGroupId(client.getGroupId());

        // 3. Формируем запрос к Python
        PythonRequestDTO pyRequest = new PythonRequestDTO();
        pyRequest.setRequestType("feed");
        pyRequest.setSyntheticU(syntheticU);
        pyRequest.setClientProfile(profile);

        ParamsDTO params = new ParamsDTO();
        params.setLimit(limit);
        params.setOffset(offset);
        params.setTopicFilter(topic);
        params.setIsAnalyticsOnly(isAnalyticsOnly);
        pyRequest.setParams(params);

        // 4. Генерируем Trace ID и зовем Python
        String traceId = UUID.randomUUID().toString();

        PythonResponseDTO pyResponse;
        try {
            pyResponse = pythonClient.generateFeed(pyRequest, traceId); // <-- ИСПОЛЬЗУЕМ PYTHON CLIENT
        } catch (Exception e) {
            throw new RuntimeException("Сервис генерации временно недоступен: " + e.getMessage());
        }

        // 5. ТРАНСФОРМАЦИЯ ОТВЕТА
        List<InsightItem> finalItems = new ArrayList<>();

        if (pyResponse != null && pyResponse.getItems() != null) {
            int fromIndex = Math.min(offset, pyResponse.getItems().size());
            int toIndex = Math.min(offset + limit, pyResponse.getItems().size());
            List<RawInsightDTO> paginatedRaw = pyResponse.getItems().subList(fromIndex, toIndex);

            // 5.2. Обогащаем элементы
            for (RawInsightDTO raw : paginatedRaw) {
                InsightItem item = new InsightItem();
                item.setNewsId(raw.getNewsId());
                item.setTitle(raw.getTitle());
                item.setSummary(raw.getSummary());
                item.setPublishedAt(raw.getPublishedAt());
                item.setSource(raw.getSource());
                item.setAnalytics(raw.getIsAnalytics());
                item.setSemanticTags(raw.getSemanticTags());
                item.setSources(raw.getSources());

                item.setInsightId(UUID.randomUUID().toString());

                double score = raw.getRelevanceScore() != null ? raw.getRelevanceScore() : 0.0;
                item.setRelevanceScore(score);
                if (score > 0.8) {
                    item.setRelevanceLevel("HIGH");
                } else if (score > 0.5) {
                    item.setRelevanceLevel("MEDIUM");
                } else {
                    item.setRelevanceLevel("LOW");
                }

                finalItems.add(item);
            }
        }

        // 5.3. Сортировка
        if ("published_at,desc".equals(order) || "date".equals(order)) {
            finalItems.sort((a, b) -> {
                if (a.getPublishedAt() == null || b.getPublishedAt() == null) return 0;
                return b.getPublishedAt().compareTo(a.getPublishedAt());
            });
        }

        // 6. Финальный ответ
        NewsFeedResponse response = new NewsFeedResponse();
        response.setItems(finalItems);

        Pagination pagination = new Pagination();
        pagination.setLimit(limit);
        pagination.setOffset(offset);
        pagination.setCount(pyResponse != null ? pyResponse.getTotalFound() : 0);
        response.setPagination(pagination);

        return response;
    }

    public NewsInsightResponse generateBusinessInsight(String syntheticU, String newsId) {
        // 1. Ищем клиента
        Client client = clientRepository.findById(syntheticU)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));

        // 2. Формируем профиль (7 полей, как в первом методе)
        ClientProfileDTO profile = new ClientProfileDTO();
        profile.setMainokvedCode(client.getMainOkvedCode());
        profile.setMainokvedName(client.getMainOkvedCode());
        profile.setHasCredits(Boolean.TRUE.equals(client.getClProductLoaMark()));
        profile.setHasVed(Boolean.TRUE.equals(client.getFtsFlag()));
        profile.setVedCountry(client.getFtsCountryName());
        profile.setRevenue(client.getRevenueCompany() != null ? client.getRevenueCompany().longValue() : 0L);
        profile.setGroupId(client.getGroupId());

        // 3. Формируем запрос (request_type = insight)
        PythonRequestDTO pyRequest = new PythonRequestDTO();
        pyRequest.setRequestType("insight"); // <--- ВАЖНО
        pyRequest.setSyntheticU(syntheticU);
        pyRequest.setClientProfile(profile);

        ParamsDTO params = new ParamsDTO();
        params.setNewsId(newsId); // <--- Передаем ID новости
        pyRequest.setParams(params);

        // 4. Зовем Python
        String traceId = UUID.randomUUID().toString();
        PythonInsightResponseDTO pyResponse;
        try {
            pyResponse = pythonClient.generateInsight(pyRequest, traceId);
        } catch (Exception e) {
            throw new RuntimeException("Сервис генерации инсайтов недоступен: " + e.getMessage());
        }

        // 5. Трансформация (ТЗ п. 6)
        NewsInsightResponse response = new NewsInsightResponse();
        response.setNewsId(pyResponse.getNewsId());
        response.setTitle(pyResponse.getTitle());
        response.setSummary(pyResponse.getSummary());
        response.setPublishedAt(pyResponse.getPublishedAt());
        response.setSource(pyResponse.getSource());
        response.setAnalytics(pyResponse.getIsAnalytics());

        // Генерируем ID и вычисляем уровень
        response.setInsightId(UUID.randomUUID().toString());
        double score = pyResponse.getRelevanceScore() != null ? pyResponse.getRelevanceScore() : 0.0;
        response.setRelevanceScore(score);
        if (score > 0.8) response.setRelevanceLevel("HIGH");
        else if (score > 0.5) response.setRelevanceLevel("MEDIUM");
        else response.setRelevanceLevel("LOW");

        // Переносим специфичные поля
        response.setWhatHappened(pyResponse.getWhatHappened());
        response.setWhyImportantForClient(pyResponse.getWhyImportantForClient());
        response.setRisks(pyResponse.getRisks());
        response.setOpportunities(pyResponse.getOpportunities());
        response.setBankPosition(pyResponse.getBankPosition());

        response.setSemanticTags(pyResponse.getSemanticTags());
        response.setSources(pyResponse.getSources());

        return response;
    }

    public ChatResponseDTO generateChatAnswer(String syntheticU, String newsId, String message) {
        // 1. Проверяем клиента
        Client client = clientRepository.findById(syntheticU)
                .orElseThrow(() -> new RuntimeException("Компания с идентификатором " + syntheticU + " не найдена"));

        // 2. Проверяем новость
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new RuntimeException("Новость с идентификатором " + newsId + " не найдена"));

        // 3. Собираем профиль (как всегда)
        ClientProfileDTO profile = new ClientProfileDTO();
        // ... (заполняем 7 полей, как в первом методе)

        // 4. Формируем запрос в Python
        PythonRequestDTO pyRequest = new PythonRequestDTO();
        pyRequest.setRequestType("chat");
        pyRequest.setSyntheticU(syntheticU);
        pyRequest.setClientProfile(profile);

        ParamsDTO params = new ParamsDTO();
        params.setNewsId(newsId);
        params.setMessage(message);
        pyRequest.setParams(params);

        // 5. Зовем Python
        String traceId = UUID.randomUUID().toString();
        ChatResponseDTO pyResponse = pythonClient.generateChatAnswer(pyRequest, traceId);

        // 6. TODO: Сохранить сообщение в базу (в таблицу messages)
        // Пока просто возвращаем то, что ответил Python
        return pyResponse;
    }
}

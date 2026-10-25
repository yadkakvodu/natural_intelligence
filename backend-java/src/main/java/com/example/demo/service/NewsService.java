package com.example.demo.service;

import com.example.demo.client.PythonClient;
import com.example.demo.dto.*;
import com.example.demo.entity.AgentTrace;
import com.example.demo.entity.Client;
import com.example.demo.entity.Message;
import com.example.demo.entity.News;
import com.example.demo.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class NewsService {

    private final ClientRepository clientRepository;
    private final NewsRepository newsRepository;
    private final PythonClient pythonClient;
    private final MessageRepository messageRepository;
    private final BudgetGuardService budgetGuardService;
    private final AgentTraceRepository agentTraceRepository;


    public NewsService(ClientRepository clientRepository, NewsRepository newsRepository,
                       PythonClient pythonClient, MessageRepository messageRepository, BudgetGuardService budgetGuardService,
                       AgentTraceRepository agentTraceRepository) {
        this.clientRepository = clientRepository;
        this.newsRepository = newsRepository;
        this.pythonClient = pythonClient;
        this.messageRepository = messageRepository;
        this.budgetGuardService = budgetGuardService;
        this.agentTraceRepository = agentTraceRepository;
    }

    public NewsFeedResponse generateNewsFeed(
            String syntheticU, int limit, int offset, String order, Boolean isAnalyticsOnly, String topic
    ) {
        // 1. Достаем клиента из базы (старый проверенный способ)
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

        budgetGuardService.checkBudget(500);

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

        if ("published_at,desc".equals(order) || "date".equals(order)) {
            finalItems.sort((a, b) -> {
                if (a.getPublishedAt() == null || b.getPublishedAt() == null) return 0;
                return b.getPublishedAt().compareTo(a.getPublishedAt());
            });
        }

        NewsFeedResponse response = new NewsFeedResponse();

//        AgentTrace trace = new AgentTrace();
//        trace.setTraceId(UUID.fromString(traceId)); // traceId, который мы генерировали
//        trace.setSyntheticU(syntheticU);
//        trace.setRequestType("feed");
//        trace.setTokensInput(pyResponse.getTokensInput());
//        trace.setTokensOutput(pyResponse.getTokensOutput());
//        trace.setCreatedAt(java.time.OffsetDateTime.now());
//// trace.setSteps(...) - тут можно сохранить JSON-шаги от Python
//        agentTraceRepository.save(trace);

        // Сохраняем трейс через нативный запрос (чтобы обойти проблему с jsonb)
        agentTraceRepository.saveTraceRaw(
                UUID.fromString(traceId), // traceId
                syntheticU,               // syntheticU
                null,                     // newsId (для ленты null, если нужен — передайте ID)
                "feed",                   // requestType
                "{}",                     // steps (пустой JSON)
// Замените эти две строки:
                pyResponse.getTokensInput(),
                pyResponse.getTokensOutput(),
                java.time.OffsetDateTime.now()
        );
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
        budgetGuardService.checkBudget(500);

        PythonRequestDTO pyRequest = new PythonRequestDTO();
        pyRequest.setRequestType("insight");
        pyRequest.setSyntheticU(syntheticU);
        pyRequest.setClientProfile(profile);

        ParamsDTO params = new ParamsDTO();
        params.setNewsId(newsId);
        pyRequest.setParams(params);

        String traceId = UUID.randomUUID().toString();
        PythonInsightResponseDTO pyResponse;
        try {
            pyResponse = pythonClient.generateInsight(pyRequest, traceId);
        } catch (Exception e) {
            throw new RuntimeException("Сервис генерации инсайтов недоступен: " + e.getMessage());
        }

        NewsInsightResponse response = new NewsInsightResponse();
        response.setNewsId(pyResponse.getNewsId());
        response.setTitle(pyResponse.getTitle());
        response.setSummary(pyResponse.getSummary());
        response.setPublishedAt(pyResponse.getPublishedAt());
        response.setSource(pyResponse.getSource());
        response.setAnalytics(pyResponse.getIsAnalytics());

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
        // 1. Валидация клиента и новости (Gatekeeper)
        if (!clientRepository.existsById(syntheticU)) {
            throw new RuntimeException("Компания с идентификатором " + syntheticU + " не найдена");
        }
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new RuntimeException("Новость с идентификатором " + newsId + " не найдена"));

        // 2. Проверка бюджета (Pre-check)
        budgetGuardService.checkBudget(3000); // В ТЗ сказано: прогноз 3000 токенов

        // 3. Сборка контекста (Context Builder)
        String conversationId = syntheticU + "_" + newsId;

        // Получаем историю диалога (последние 10 сообщений)
        List<Message> history = messageRepository.findTop10ByConversationIdOrderByCreatedAtAsc(conversationId);

        // Формируем массив сообщений для LLM
        List<LlmMessageDTO> llmMessages = new ArrayList<>();

        // 3.1. Системный промпт (по ТЗ)
        String systemPrompt = String.format(
                "Ты финансовый советник Альфа-Банка. Новость: '%s'. Кратко: '%s'. Отвечай СТРОГО в контексте этой новости.",
                news.getTitle(), news.getSummary()
        );
        llmMessages.add(new LlmMessageDTO("system", systemPrompt));

        // 3.2. История диалога (добавляем то, что уже было сказано)
        for (Message msg : history) {
            llmMessages.add(new LlmMessageDTO(msg.getRole(), msg.getContent()));
        }

        // 3.3. Новый вопрос пользователя
        llmMessages.add(new LlmMessageDTO("user", message));

        // 4. Формируем запрос к Python
        PythonRequestDTO pyRequest = new PythonRequestDTO();
        pyRequest.setRequestType("chat");
        pyRequest.setSyntheticU(syntheticU);

        // (Клиентский профиль тоже можно передать, если Python его использует)
        ClientProfileDTO profile = new ClientProfileDTO(); // ... заполнить ...
        pyRequest.setClientProfile(profile);

        LlmRequestDTO llmRequest = new LlmRequestDTO();
        llmRequest.setMessages(llmMessages);
        pyRequest.setLlmRequest(llmRequest);

        // 5. Вызов Python
        String traceId = UUID.randomUUID().toString();
        ChatResponseDTO pyResponse;
        try {
            pyResponse = pythonClient.generateChatAnswer(pyRequest, traceId);
        } catch (Exception e) {
            throw new RuntimeException("Сервис генерации ответов временно недоступен: " + e.getMessage());
        }

        // 6. ВАЖНО: По ТЗ v3.0 Java НЕ сохраняет сообщения в БД.
        // Это делает Python асинхронно.

        // 7. Возвращаем ответ фронтенду
        return pyResponse;
    }


    public ChatHistoryMessageDTO getChatHistory(String syntheticU, String newsId, int limit, int offset) {

        // 1. Валидация существования клиента и новости
        clientRepository.findById(syntheticU)
                .orElseThrow(() -> new RuntimeException("Компания с идентификатором " + syntheticU + " не найдена"));

        newsRepository.findById(newsId)
                .orElseThrow(() -> new RuntimeException("Новость с идентификатором " + newsId + " не найдена"));

        String conversationId = syntheticU + "_" + newsId;

        // 3. Настраиваем пагинацию для базы данных
        // offset / limit дает нам номер страницы (например, offset=0, limit=20 -> страница 0)
        int pageNumber = offset / limit;
        Pageable pageable = PageRequest.of(pageNumber, limit);

        // 4. Идем в базу данных
        Page<Message> messagePage = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable);

        // 5. Трансформируем сущности из базы в DTO для фронтенда
        List<ChatHistoryDTO> items = new ArrayList<>();

        for (Message msg : messagePage.getContent()) {
            ChatHistoryDTO item = new ChatHistoryDTO();

            // Безопасно конвертируем UUID в String
            item.setMessageId(msg.getMessageId() != null ? msg.getMessageId().toString() : null);
            item.setRole(msg.getRole());
            item.setContent(msg.getContent());

            // Форматируем дату в ISO 8601 (например: 2026-10-24T10:00:00Z)
            if (msg.getCreatedAt() != null) {
                item.setCreatedAt(msg.getCreatedAt().toString());
            }

            // TODO: Парсинг citations (ссылок на источники), если они есть в виде JSON-строки
            // item.setCitations(...);

            items.add(item);
        }

        // 6. Собираем финальный ответ с пагинацией
        ChatHistoryMessageDTO response = new ChatHistoryMessageDTO();

        Pagination pagination = new Pagination();
        pagination.setLimit(limit);
        pagination.setOffset(offset);
        pagination.setCount(messagePage.getTotalElements()); // Общее количество сообщений в базе

        response.setPagination(pagination);
        response.setItems(items);

        return response;
    }

    public List<SourceResponseDTO> getSources(String syntheticU, String newsId) {
        // 1. Проверяем клиента
        clientRepository.findById(syntheticU)
                .orElseThrow(() -> new RuntimeException("Компания с идентификатором " + syntheticU + " не найдена"));

        // 2. Проверяем текущую новость
        News currentNews = newsRepository.findById(newsId)
                .orElseThrow(() -> new RuntimeException("Новость с идентификатором " + newsId + " не найдена"));

        List<SourceResponseDTO> sources = new ArrayList<>();

        // 3. Добавляем саму текущую новость как первый источник
        sources.add(mapToSourceDTO(currentNews));

        // 4. Достаем 2-3 смежные новости (для простоты берем просто последние из базы)
        // В реальном проекте тут был бы запрос по совпадению тегов (topics)
        List<News> relatedNews = newsRepository.findAll(PageRequest.of(0, 3)).getContent();

        for (News news : relatedNews) {
            // Чтобы не дублировать текущую новость в списке
            if (!news.getNewsId().equals(newsId)) {
                sources.add(mapToSourceDTO(news));
            }
        }

        // 5. Сортируем: сначала аналитика Альфа-Банка, потом остальные (по ТЗ)
        sources.sort((a, b) -> {
            boolean aIsAlfa = "alfa_analytics".equals(a.getType());
            boolean bIsAlfa = "alfa_analytics".equals(b.getType());
            if (aIsAlfa && !bIsAlfa) return -1;
            if (!aIsAlfa && bIsAlfa) return 1;
            return 0;
        });

        return sources;
    }

    // Вспомогательный метод для маппинга Entity в DTO
    private SourceResponseDTO mapToSourceDTO(News news) {
        SourceResponseDTO dto = new SourceResponseDTO();
        dto.setId(news.getNewsId());
        dto.setTitle(news.getTitle());
        dto.setName(news.getSource()); // Название источника (РБК, Альфа-Банк и т.д.)
        dto.setPublishedAt(news.getPublishedAt() != null ? news.getPublishedAt().toString() : null);

        // Определяем тип (по ТЗ: alfa_analytics / regulator / media)
        if (Boolean.TRUE.equals(news.getAnalytics())) {
            dto.setType("alfa_analytics");
        } else if (news.getSource() != null && news.getSource().contains("Банк России")) {
            dto.setType("regulator");
        } else {
            dto.setType("media");
        }

        return dto;
    }

}

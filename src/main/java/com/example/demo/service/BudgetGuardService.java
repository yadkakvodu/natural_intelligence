package com.example.demo.service;

import com.example.demo.repository.AgentTraceRepository;
import org.springframework.stereotype.Service;

@Service
public class BudgetGuardService {

    private final AgentTraceRepository traceRepository;

    // Лимит в токенах (примерно 500 рублей, зависит от модели)
    private static final long MAX_TOKENS_PER_DAY = 100000;

    public BudgetGuardService(AgentTraceRepository traceRepository) {
        this.traceRepository = traceRepository;
    }

    public void checkBudget(int estimatedTokensToSpend) {
        Long spentTokens = traceRepository.getTotalTokensInLast24Hours();

        if (spentTokens + estimatedTokensToSpend > MAX_TOKENS_PER_DAY) {
            // Выбрасываем ошибку, которую перехватит GlobalExceptionHandler
            throw new RuntimeException("Превышен лимит бюджета LLM. Попробуйте позже.");
            // (В идеале здесь нужно выбросить кастомное исключение BudgetExceededException и вернуть 429)
        }
    }
}
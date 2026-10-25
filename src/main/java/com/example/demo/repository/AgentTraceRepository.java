package com.example.demo.repository;

import com.example.demo.entity.AgentTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface AgentTraceRepository extends JpaRepository<AgentTrace, UUID> {

    // 1. Суммируем все токены за последние 24 часа (для бюджета)
    @Query(value = "SELECT COALESCE(SUM(tokens_input + tokens_output), 0) FROM agent_traces WHERE created_at > NOW() - INTERVAL '24 hours'", nativeQuery = true)
    Long getTotalTokensInLast24Hours();

    // 2. Сохраняем трейс с принудительным приведением к jsonb
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO agent_traces (trace_id, synthetic_u, news_id, request_type, steps, tokens_input, tokens_output, created_at) " +
            "VALUES (:traceId, :syntheticU, :newsId, :requestType, cast(:steps as jsonb), :tokensInput, :tokensOutput, :createdAt)", nativeQuery = true)
    void saveTraceRaw(
            @Param("traceId") UUID traceId,
            @Param("syntheticU") String syntheticU,
            @Param("newsId") String newsId,
            @Param("requestType") String requestType,
            @Param("steps") String steps,
            @Param("tokensInput") Integer tokensInput,
            @Param("tokensOutput") Integer tokensOutput,
            @Param("createdAt") OffsetDateTime createdAt
    );
}
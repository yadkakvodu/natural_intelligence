package com.example.demo.repository;

import com.example.demo.entity.AgentTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AgentTraceRepository extends JpaRepository<AgentTrace, UUID> {

    // Суммируем все токены за последние 24 часа
    @Query(value = "SELECT COALESCE(SUM(tokens_input + tokens_output), 0) FROM agent_traces WHERE created_at > NOW() - INTERVAL '24 hours'", nativeQuery = true)
    Long getTotalTokensInLast24Hours();
}
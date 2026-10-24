package com.example.demo.repository;

import com.example.demo.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, String> {

    @Query(value = """
        SELECT 
            synthetic_u AS syntheticU,
            mainokved_code AS mainokvedCode,
            COALESCE(cl_product_loa_mark, false) AS hasCredits,
            COALESCE(fts_flag, false) AS hasVed,
            fts_country_name AS vedCountry,
            revenue_company AS revenue,
            group_id AS groupId
        FROM clients 
        WHERE synthetic_u = :syntheticU
        """, nativeQuery = true)
    Object findClientProfileRaw(@Param("syntheticU") String syntheticU);
}
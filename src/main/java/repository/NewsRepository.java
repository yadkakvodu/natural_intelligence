package repository;

import entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, String> {


    @Query(value = """
        SELECT * FROM news 
        WHERE published_at >= :dateLimit 
        ORDER BY embedding <=> cast(:queryVector as vector) 
        LIMIT :limit
        """, nativeQuery = true)
    List<News> findRelevantNews(
            @Param("queryVector") String queryVector,
            @Param("dateLimit") OffsetDateTime dateLimit,
            @Param("limit") int limit
    );


}

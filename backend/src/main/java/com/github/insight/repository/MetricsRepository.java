package com.github.insight.repository;

import com.github.insight.entity.MetricsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MetricsRepository extends JpaRepository<MetricsEntity, String> {
    Optional<MetricsEntity> findByRequestId(String requestId);

    @Query("SELECT e FROM MetricsEntity e WHERE e.calculatedAt < :cutoff")
    List<MetricsEntity> findExpiredMetrics(@Param("cutoff") LocalDateTime cutoff);
}

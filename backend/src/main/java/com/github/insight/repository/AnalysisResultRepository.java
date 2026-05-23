package com.github.insight.repository;

import com.github.insight.entity.AnalysisResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResultEntity, String> {
    Optional<AnalysisResultEntity> findByRequestId(String requestId);

    List<AnalysisResultEntity> findByUserId(String userId);

    List<AnalysisResultEntity> findByGithubId(String githubId);

    Optional<AnalysisResultEntity> findFirstByGithubIdOrderByCreatedAtDesc(String githubId);

    @Query("SELECT e FROM AnalysisResultEntity e WHERE e.createdAt < :cutoff")
    List<AnalysisResultEntity> findExpiredResults(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT e FROM AnalysisResultEntity e WHERE e.userId = :userId ORDER BY e.createdAt DESC")
    List<AnalysisResultEntity> findLatestByUserId(@Param("userId") String userId);

    @Query("SELECT e FROM AnalysisResultEntity e WHERE e.githubId = :githubId ORDER BY e.createdAt DESC")
    List<AnalysisResultEntity> findLatestByGithubId(@Param("githubId") String githubId);
}

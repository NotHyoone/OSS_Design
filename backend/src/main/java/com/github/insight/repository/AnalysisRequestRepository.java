package com.github.insight.repository;

import com.github.insight.entity.AnalysisRequestEntity;
import com.github.insight.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisRequestRepository extends JpaRepository<AnalysisRequestEntity, String> {
    List<AnalysisRequestEntity> findByStatus(RequestStatus status);

    List<AnalysisRequestEntity> findByGithubId(String githubId);

    Optional<AnalysisRequestEntity> findFirstByGithubIdOrderByRequestedAtDesc(String githubId);

    @Query("SELECT e FROM AnalysisRequestEntity e WHERE e.status IN (:statuses) AND e.requestedAt < :cutoff")
    List<AnalysisRequestEntity> findExpiredByStatuses(
        @Param("statuses") List<RequestStatus> statuses,
        @Param("cutoff") LocalDateTime cutoff
    );

    @Query("SELECT e FROM AnalysisRequestEntity e WHERE e.status = :status AND e.requestedAt < :cutoff")
    List<AnalysisRequestEntity> findStuckRequests(
        @Param("status") RequestStatus status,
        @Param("cutoff") LocalDateTime cutoff
    );
}

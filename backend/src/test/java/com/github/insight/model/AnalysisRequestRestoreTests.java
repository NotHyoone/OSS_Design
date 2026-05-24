package com.github.insight.model;

import com.github.insight.model.enums.RequestStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("AnalysisRequest 복원 테스트")
class AnalysisRequestRestoreTests {

    @Test
    @DisplayName("완료되지 않은 요청도 진행률이 보존된다")
    void restorePreservesProgressForRunningRequest() {
        LocalDateTime requestedAt = LocalDateTime.now().minusMinutes(5);

        AnalysisRequest restored = AnalysisRequest.restore(
                "req-restore-1",
                "user-1",
                "octocat",
                requestedAt,
                null,
                null,
                RequestStatus.RUNNING,
                1,
                2,
                67.5,
                "지표 계산 중...");

        assertEquals("req-restore-1", restored.getRequestId());
        assertEquals(RequestStatus.RUNNING, restored.getStatus());
        assertEquals(2, restored.getStep());
        assertEquals(67.5, restored.getOverallPct());
        assertEquals("지표 계산 중...", restored.getDetail());
        assertEquals(requestedAt, restored.getRequestedAt());
    }
}

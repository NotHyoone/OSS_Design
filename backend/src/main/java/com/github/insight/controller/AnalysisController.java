package com.github.insight.controller;

import com.github.insight.dto.AnalysisRequestResponse;
import com.github.insight.dto.AnalysisStatusResponse;
import com.github.insight.dto.CreateRequestBody;
import com.github.insight.model.AnalysisRequest;
import com.github.insight.model.AnalysisRequest.Status;
import com.github.insight.model.AnalysisResult;
import com.github.insight.service.AnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 분석 요청·진행·결과·이력 REST API
 */
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private static final Pattern VALID_ID = Pattern.compile(
            "^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,37}[a-zA-Z0-9])?$"
    );

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * 분석 요청 생성 (UC-02)
     * POST /api/analysis/request  body: { "githubId": "..." }
     */
    @PostMapping("/request")
    public ResponseEntity<?> createRequest(@RequestBody CreateRequestBody body) {
        String githubId = body.githubId();

        if (githubId == null || githubId.isBlank() || !VALID_ID.matcher(githubId).matches()) {
            return ResponseEntity.badRequest().body("유효하지 않은 GitHub ID입니다.");
        }

        AnalysisRequest req = analysisService.createRequest(githubId);
        return ResponseEntity.ok(new AnalysisRequestResponse(req.getRequestId(), 45));
    }

    /**
     * 분석 진행 상태 조회 (UC-03 ~ UC-05)
     * GET /api/analysis/status/{requestId}
     */
    @GetMapping("/status/{requestId}")
    public ResponseEntity<AnalysisStatusResponse> getStatus(@PathVariable String requestId) {
        return analysisService.getRequest(requestId)
                .map(req -> {
                    String stepStatus = switch (req.getStatus()) {
                        case PENDING  -> "running";
                        case RUNNING  -> "running";
                        case DONE     -> "done";
                        case ERROR    -> "error";
                    };
                    return ResponseEntity.ok(new AnalysisStatusResponse(
                            req.getStep(),
                            stepStatus,
                            req.getOverallPct(),
                            req.getDetail()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 분석 결과 조회 (UC-06)
     * GET /api/analysis/result/{githubId}
     */
    @GetMapping("/result/{githubId}")
    public ResponseEntity<AnalysisResult> getResult(@PathVariable String githubId) {
        return analysisService.getLatestResult(githubId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 분석 이력 목록 조회 (UC-08)
     * GET /api/analysis/history/{githubId}
     */
    @GetMapping("/history/{githubId}")
    public ResponseEntity<List<AnalysisResult>> getHistory(@PathVariable String githubId) {
        List<AnalysisResult> list = analysisService.getHistory(githubId);
        if (list.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(list);
    }

    /**
     * 분석 취소 (UC-02 1a.3)
     * POST /api/analysis/cancel/{requestId}
     */
    @PostMapping("/cancel/{requestId}")
    public ResponseEntity<Void> cancel(@PathVariable String requestId) {
        analysisService.cancel(requestId);
        return ResponseEntity.ok().build();
    }
}

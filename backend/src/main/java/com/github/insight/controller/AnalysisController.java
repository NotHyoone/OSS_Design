package com.github.insight.controller;

import com.github.insight.dto.AnalysisRequestResponse;
import com.github.insight.dto.AnalysisStatusResponse;
import com.github.insight.dto.CreateRequestBody;
import com.github.insight.model.AnalysisRequest;
import com.github.insight.model.AnalysisResult;
import com.github.insight.model.Metrics;
import com.github.insight.model.User;
import com.github.insight.model.enums.RequestStatus;
import com.github.insight.service.AnalysisService;
import com.github.insight.service.ReportAssembler;
import com.github.insight.service.ReportGenerator;
import com.github.insight.service.auth.AuthenticationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private static final Pattern VALID_ID = Pattern.compile(
        "^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,37}[a-zA-Z0-9])?$"
    );

    private final AnalysisService analysisService;
    private final ReportAssembler reportAssembler;
    private final ReportGenerator reportGenerator;
    private final AuthenticationService authenticationService;

    public AnalysisController(AnalysisService analysisService,
                               ReportAssembler reportAssembler,
                               ReportGenerator reportGenerator,
                               AuthenticationService authenticationService) {
        this.analysisService       = analysisService;
        this.reportAssembler       = reportAssembler;
        this.reportGenerator       = reportGenerator;
        this.authenticationService = authenticationService;
    }

    /** 분석 요청 생성 (UC-02) */
    @PostMapping("/request")
    public ResponseEntity<?> createRequest(
            @RequestBody CreateRequestBody body,
            @CookieValue(value = "SESSION_ID", required = false) String sessionId) {
        String githubId = body.githubId();
        if (githubId == null || githubId.isBlank() || !VALID_ID.matcher(githubId).matches()) {
            return ResponseEntity.badRequest().body("유효하지 않은 GitHub ID입니다.");
        }

        String userId = null;
        if (sessionId != null) {
            userId = authenticationService.getUserBySession(sessionId)
                .map(u -> u.getUserId()).orElse(null);
        }

        AnalysisRequest req = analysisService.requestAnalysis(userId, githubId);
        return ResponseEntity.ok(new AnalysisRequestResponse(
            req.getRequestId(), 45, req.getResultAccessToken()));
    }

    /** 분석 진행 상태 조회 (UC-03 ~ UC-05) */
    @GetMapping("/status/{requestId}")
    public ResponseEntity<AnalysisStatusResponse> getStatus(@PathVariable String requestId) {
        return analysisService.getRequest(requestId)
            .map(req -> {
                String stepStatus = switch (req.getStatus()) {
                    case PENDING  -> "running";
                    case RUNNING  -> "running";
                    case COMPLETED -> "done";
                    case PARTIAL   -> "done";
                    case FAILED    -> "error";
                };
                return ResponseEntity.ok(new AnalysisStatusResponse(
                    req.getStep(), stepStatus, req.getOverallPct(), req.getDetail()
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /** 분석 결과 조회 (UC-06) */
    @GetMapping("/result/{githubId}")
    public ResponseEntity<?> getResult(
            @PathVariable String githubId,
            @CookieValue(value = "SESSION_ID", required = false) String sessionId) {
        Optional<User> userOpt = authenticatedUser(sessionId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("인증이 필요합니다.");
        }
        if (!isOwner(userOpt.get(), githubId)) {
            return ResponseEntity.status(403).body("본인 데이터만 조회할 수 있습니다.");
        }

        Optional<AnalysisResult> resultOpt = analysisService.getLatestResult(githubId);
        if (resultOpt.isEmpty()) return ResponseEntity.notFound().build();

        AnalysisResult result = resultOpt.get();
        Optional<Metrics> metricsOpt = analysisService.getMetrics(result.getRequestId());
        Map<String, Object> vm = reportAssembler.toViewModel(result, metricsOpt.orElse(null));
        return ResponseEntity.ok(vm);
    }

    /** 요청 ID 기준 분석 결과 조회 */
    @GetMapping("/result/request/{requestId}")
    public ResponseEntity<?> getResultByRequest(
            @PathVariable String requestId,
            @RequestParam(value = "token", required = false) String token,
            @CookieValue(value = "SESSION_ID", required = false) String sessionId) {
        Optional<AnalysisRequest> requestOpt = analysisService.getRequest(requestId);
        if (requestOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AnalysisRequest request = requestOpt.get();
        ResponseEntity<?> accessDenied = denyIfCannotAccessRequest(request, sessionId, token);
        if (accessDenied != null) return accessDenied;

        try {
            AnalysisResult result = analysisService.getResult(requestId);
            Optional<Metrics> metricsOpt = analysisService.getMetrics(requestId);
            Map<String, Object> vm = reportAssembler.toViewModel(result, metricsOpt.orElse(null));
            return ResponseEntity.ok(vm);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** 분석 이력 목록 조회 (UC-08) */
    @GetMapping("/history/{githubId}")
    public ResponseEntity<?> getHistory(
            @PathVariable String githubId,
            @CookieValue(value = "SESSION_ID", required = false) String sessionId) {
        Optional<User> userOpt = authenticatedUser(sessionId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("인증이 필요합니다.");
        }
        if (!isOwner(userOpt.get(), githubId)) {
            return ResponseEntity.status(403).body("본인 데이터만 조회할 수 있습니다.");
        }

        List<AnalysisResult> list = analysisService.getHistory(githubId);
        if (list.isEmpty()) return ResponseEntity.noContent().build();

        List<Map<String, Object>> vms = list.stream()
            .map(r -> {
                Optional<Metrics> m = analysisService.getMetrics(r.getRequestId());
                return reportAssembler.toViewModel(r, m.orElse(null));
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(vms);
    }

    /** PDF 다운로드 (UC-07) */
    @GetMapping("/report/{githubId}")
    public ResponseEntity<?> downloadReport(
            @PathVariable String githubId,
            @CookieValue(value = "SESSION_ID", required = false) String sessionId) {
        Optional<User> userOpt = authenticatedUser(sessionId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("인증이 필요합니다.");
        }
        if (!isOwner(userOpt.get(), githubId)) {
            return ResponseEntity.status(403).body("본인 데이터만 조회할 수 있습니다.");
        }

        Optional<AnalysisResult> resultOpt = analysisService.getLatestResult(githubId);
        if (resultOpt.isEmpty()) return ResponseEntity.notFound().build();

        AnalysisResult result = resultOpt.get();
        Optional<Metrics> metricsOpt = analysisService.getMetrics(result.getRequestId());
        byte[] pdf = reportGenerator.renderPdf(result, metricsOpt.orElse(null));
        String filename = reportGenerator.getFilename(githubId, result.getCreatedAt());

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    /** 요청 ID 기준 PDF 다운로드 */
    @GetMapping("/report/request/{requestId}")
    public ResponseEntity<?> downloadReportByRequest(
            @PathVariable String requestId,
            @RequestParam(value = "token", required = false) String token,
            @CookieValue(value = "SESSION_ID", required = false) String sessionId) {
        Optional<AnalysisRequest> requestOpt = analysisService.getRequest(requestId);
        if (requestOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AnalysisRequest request = requestOpt.get();
        ResponseEntity<?> accessDenied = denyIfCannotAccessRequest(request, sessionId, token);
        if (accessDenied != null) return accessDenied;

        try {
            AnalysisResult result = analysisService.getResult(requestId);
            Optional<Metrics> metricsOpt = analysisService.getMetrics(requestId);
            byte[] pdf = reportGenerator.renderPdf(result, metricsOpt.orElse(null));
            String filename = reportGenerator.getFilename(result.getGithubId(), result.getCreatedAt());

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** 분석 취소 */
    @PostMapping("/cancel/{requestId}")
    public ResponseEntity<?> cancel(
            @PathVariable String requestId,
            @RequestParam(value = "token", required = false) String token,
            @CookieValue(value = "SESSION_ID", required = false) String sessionId) {
        Optional<AnalysisRequest> requestOpt = analysisService.getRequest(requestId);
        if (requestOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AnalysisRequest request = requestOpt.get();
        ResponseEntity<?> accessDenied = denyIfCannotAccessRequest(request, sessionId, token);
        if (accessDenied != null) return accessDenied;

        analysisService.cancel(requestId);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<?> denyIfCannotAccessRequest(AnalysisRequest request, String sessionId, String token) {
        if (request.getUserId() == null) {
            if (analysisService.validateResultAccessToken(request, token)) {
                return null;
            }
            return ResponseEntity.status(403).body("요청 접근 토큰이 필요합니다.");
        }

        Optional<User> userOpt = authenticatedUser(sessionId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("인증이 필요합니다.");
        }
        if (!request.getUserId().equals(userOpt.get().getUserId())) {
            return ResponseEntity.status(403).body("본인 데이터만 조회할 수 있습니다.");
        }
        return null;
    }

    private Optional<User> authenticatedUser(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        return authenticationService.getUserBySession(sessionId);
    }

    private boolean isOwner(User user, String githubId) {
        return user.getGithubId() != null && user.getGithubId().equalsIgnoreCase(githubId);
    }
}

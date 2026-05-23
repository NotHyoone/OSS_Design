package com.github.insight.service;

import com.github.insight.model.AnalysisResult;
import com.github.insight.model.Metrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class ReportGenerator {

    private final ReportAssembler reportAssembler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReportGenerator(ReportAssembler reportAssembler) {
        this.reportAssembler = reportAssembler;
    }

    /**
     * JSON 형식의 리포트를 바이트 배열로 생성.
     * PDF 생성 대신 JSON 리포트 반환 (클라이언트에서 화면 표시 또는 PDF 변환 처리)
     */
    public byte[] renderPdf(AnalysisResult result, Metrics metrics) {
        Map<String, Object> model = reportAssembler.toPdfModel(result, metrics);

        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(model);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("리포트 생성 실패", e);
        }
    }

    public String getFilename(String githubId, LocalDateTime createdAt) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        return String.format("report_%s_%s.json", githubId, createdAt.format(formatter));
    }
}

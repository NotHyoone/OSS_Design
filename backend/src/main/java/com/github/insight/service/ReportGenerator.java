package com.github.insight.service;

import com.github.insight.model.AnalysisResult;
import com.github.insight.model.Metrics;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class ReportGenerator {

    private final ReportAssembler reportAssembler;

    public ReportGenerator(ReportAssembler reportAssembler) {
        this.reportAssembler = reportAssembler;
    }

    /**
     * PDF 렌더링 (실제 PDF 라이브러리 미포함 - 구조 구현).
     * 현재는 JSON 기반 바이트 배열을 반환하며, 실제 구현 시 iText/Apache PDFBox로 교체.
     */
    public byte[] renderPdf(AnalysisResult result, Metrics metrics) {
        Map<String, Object> model = reportAssembler.toPdfModel(result, metrics);
        String content = buildPdfContent(model);
        return content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public String getFilename(String githubId, LocalDateTime date) {
        String dateStr = date != null
            ? date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return githubId + "_Analysis_" + dateStr + ".pdf";
    }

    private String buildPdfContent(Map<String, Object> model) {
        StringBuilder sb = new StringBuilder();
        sb.append("GitHub Activity Insight Report\n");
        sb.append("================================\n");
        model.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
        return sb.toString();
    }
}

package com.github.insight.service;

import com.github.insight.model.AnalysisResult;
import com.github.insight.model.Metrics;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ReportGenerator {

    private final ReportAssembler reportAssembler;

    public ReportGenerator(ReportAssembler reportAssembler) {
        this.reportAssembler = reportAssembler;
    }

    public byte[] renderPdf(AnalysisResult result, Metrics metrics) {
        Map<String, Object> model = reportAssembler.toPdfModel(result, metrics);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document doc = new Document(pdfDoc);

            addTitle(doc, result);
            addSummary(doc, result);
            addMetrics(doc, metrics);
            addStrengths(doc, result);
            addImprovements(doc, result);
            addFooter(doc, result);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF 생성 실패", e);
        }
    }

    private void addTitle(Document doc, AnalysisResult result) {
        Paragraph title = new Paragraph("GitHub Activity Insight Report")
            .setFontSize(24)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20);
        doc.add(title);

        Paragraph subtitle = new Paragraph(result.getGithubId())
            .setFontSize(16)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(30);
        doc.add(subtitle);
    }

    private void addSummary(Document doc, AnalysisResult result) {
        Paragraph header = new Paragraph("분석 요약")
            .setFontSize(14)
            .setBold()
            .setMarginTop(20)
            .setMarginBottom(10);
        doc.add(header);

        String developerType = result.getDeveloperType() != null
            ? result.getDeveloperType().name()
            : "UNKNOWN";

        Table summaryTable = new Table(2);
        summaryTable.addCell("항목");
        summaryTable.addCell("값");
        summaryTable.addCell("개발자 타입");
        summaryTable.addCell(developerType);
        summaryTable.addCell("총 점수");
        summaryTable.addCell(result.getTotalScore() + " / 100");
        summaryTable.addCell("신뢰도");
        summaryTable.addCell(result.getTrustLevel() != null ? result.getTrustLevel().name() : "HIGH");
        summaryTable.addCell("분석 날짜");
        summaryTable.addCell(formatDate(result.getCreatedAt()));

        doc.add(summaryTable);
        doc.add(new Paragraph(""));
    }

    private void addMetrics(Document doc, Metrics metrics) {
        if (metrics == null) return;

        Paragraph header = new Paragraph("상세 메트릭")
            .setFontSize(14)
            .setBold()
            .setMarginTop(20)
            .setMarginBottom(10);
        doc.add(header);

        Table metricsTable = new Table(2);
        metricsTable.addCell("지표");
        metricsTable.addCell("점수");
        metricsTable.addCell("활동도 (Activity)");
        metricsTable.addCell(String.format("%.0f / 100", metrics.getActivityScore()));
        metricsTable.addCell("다양성 (Diversity)");
        metricsTable.addCell(String.format("%.0f / 100", metrics.getDiversityScore()));
        metricsTable.addCell("협업 (Collaboration)");
        metricsTable.addCell(String.format("%.0f / 100", metrics.getCollaborationScore()));
        metricsTable.addCell("지속성 (Persistence)");
        metricsTable.addCell(String.format("%.0f / 100", metrics.getPersistenceScore()));

        doc.add(metricsTable);
        doc.add(new Paragraph(""));
    }

    private void addStrengths(Document doc, AnalysisResult result) {
        Paragraph header = new Paragraph("강점")
            .setFontSize(14)
            .setBold()
            .setMarginTop(20)
            .setMarginBottom(10);
        doc.add(header);

        List<String> strengths = result.getStrengths();
        if (strengths != null && !strengths.isEmpty()) {
            for (String strength : strengths) {
                Paragraph item = new Paragraph("• " + strength)
                    .setMarginBottom(5);
                doc.add(item);
            }
        } else {
            doc.add(new Paragraph("분석 중 강점이 파악되지 않았습니다."));
        }
        doc.add(new Paragraph(""));
    }

    private void addImprovements(Document doc, AnalysisResult result) {
        Paragraph header = new Paragraph("개선 사항")
            .setFontSize(14)
            .setBold()
            .setMarginTop(20)
            .setMarginBottom(10);
        doc.add(header);

        var improvements = result.getImprovements();
        if (improvements != null && !improvements.isEmpty()) {
            for (var item : improvements) {
                Paragraph p = new Paragraph("• " + item.getActionGuide())
                    .setMarginBottom(5);
                doc.add(p);
            }
        } else {
            doc.add(new Paragraph("개선이 필요한 사항이 없습니다."));
        }
        doc.add(new Paragraph(""));
    }

    private void addFooter(Document doc, AnalysisResult result) {
        doc.add(new Paragraph(""));
        Paragraph footer = new Paragraph(
            String.format("Rule Version: %s | Request ID: %s",
                result.getRuleVersion(),
                result.getRequestId()))
            .setFontSize(9)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(30);
        doc.add(footer);
    }

    private String formatDate(LocalDateTime date) {
        if (date == null) return "N/A";
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getFilename(String githubId, LocalDateTime date) {
        String dateStr = date != null
            ? date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return githubId + "_Analysis_" + dateStr + ".pdf";
    }
}


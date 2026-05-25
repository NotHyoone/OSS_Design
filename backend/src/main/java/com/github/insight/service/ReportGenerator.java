package com.github.insight.service;

import com.github.insight.model.AnalysisResult;
import com.github.insight.model.Metrics;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ReportGenerator {

    private static final String FONT_REGULAR = "/usr/share/fonts/truetype/nanum/NanumGothic.ttf";
    private static final String FONT_BOLD    = "/usr/share/fonts/truetype/nanum/NanumGothicBold.ttf";

    private final ReportAssembler reportAssembler;

    public ReportGenerator(ReportAssembler reportAssembler) {
        this.reportAssembler = reportAssembler;
    }

    public byte[] renderPdf(AnalysisResult result, Metrics metrics) {
        Map<String, Object> model = reportAssembler.toPdfModel(result, metrics);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 60, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            BaseFont bfRegular = loadBaseFont(FONT_REGULAR);
            BaseFont bfBold    = loadBaseFont(FONT_BOLD);

            Font fontTitle  = new Font(bfBold,    22, Font.NORMAL, new Color(0x1f, 0x6f, 0xeb));
            Font fontH2     = new Font(bfBold,    14, Font.NORMAL, new Color(0x1f, 0x6f, 0xeb));
            Font fontBody   = new Font(bfRegular, 11, Font.NORMAL, Color.BLACK);
            Font fontSmall  = new Font(bfRegular,  9, Font.NORMAL, new Color(0x66, 0x66, 0x66));
            Font fontScore  = new Font(bfBold,    36, Font.NORMAL, new Color(0x1f, 0x6f, 0xeb));

            // ── Title ──────────────────────────────────────────────────────────
            Paragraph title = new Paragraph("GitHub Activity Insight Report", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            document.add(title);

            String dateStr = result.getCreatedAt() != null
                ? result.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));
            Paragraph dateP = new Paragraph(dateStr + " 기준", fontSmall);
            dateP.setAlignment(Element.ALIGN_CENTER);
            dateP.setSpacingAfter(16);
            document.add(dateP);

            document.add(new Chunk(new LineSeparator(1, 100, new Color(0x1f, 0x6f, 0xeb),
                    Element.ALIGN_CENTER, -2)));
            document.add(new Paragraph(" "));

            // ── GitHub ID / Developer Type ─────────────────────────────────────
            String githubId = (String) model.getOrDefault("githubId", "");
            Paragraph idP = new Paragraph("@" + githubId,
                    new Font(bfBold, 16, Font.NORMAL, Color.BLACK));
            idP.setSpacingBefore(6);
            idP.setSpacingAfter(2);
            document.add(idP);

            Paragraph typeP = new Paragraph(
                    (String) model.getOrDefault("developerType", ""),
                    new Font(bfRegular, 12, Font.NORMAL, new Color(0x55, 0x55, 0x55)));
            typeP.setSpacingAfter(16);
            document.add(typeP);

            // ── Total Score ────────────────────────────────────────────────────
            PdfPTable scoreBox = new PdfPTable(2);
            scoreBox.setWidthPercentage(55);
            scoreBox.setHorizontalAlignment(Element.ALIGN_LEFT);
            scoreBox.setWidths(new float[]{1, 2});
            scoreBox.setSpacingAfter(20);

            PdfPCell scoreNumCell = new PdfPCell(
                    new Phrase(String.valueOf(result.getTotalScore()), fontScore));
            scoreNumCell.setBorder(Rectangle.NO_BORDER);
            scoreNumCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            scoreNumCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            scoreNumCell.setPadding(8);
            scoreBox.addCell(scoreNumCell);

            PdfPCell scoreLabelCell = new PdfPCell();
            scoreLabelCell.setBorder(Rectangle.NO_BORDER);
            scoreLabelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            scoreLabelCell.addElement(new Paragraph("종합 점수",
                    new Font(bfRegular, 10, Font.NORMAL, new Color(0x55, 0x55, 0x55))));
            scoreLabelCell.addElement(new Paragraph("/ 100점",
                    new Font(bfRegular, 9, Font.NORMAL, new Color(0x99, 0x99, 0x99))));
            scoreLabelCell.addElement(new Paragraph(
                    "신뢰도: " + model.getOrDefault("trustLevel", "HIGH"), fontSmall));
            scoreBox.addCell(scoreLabelCell);
            document.add(scoreBox);

            // ── Metrics Table ──────────────────────────────────────────────────
            Paragraph metricsTitle = new Paragraph("세부 지표", fontH2);
            metricsTitle.setSpacingAfter(8);
            document.add(metricsTitle);

            @SuppressWarnings("unchecked")
            Map<String, Object> metricsMap = (Map<String, Object>) model.get("metrics");
            if (metricsMap != null) {
                PdfPTable table = new PdfPTable(3);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{2.5f, 1f, 4f});
                table.setSpacingAfter(20);

                addHeaderCell(table, "지표",  bfBold);
                addHeaderCell(table, "점수",  bfBold);
                addHeaderCell(table, "설명",  bfBold);

                String[][] rows = {
                    {"활동성 (Activity)",          "activity"},
                    {"기술 다양성 (Diversity)",     "diversity"},
                    {"협업도 (Collaboration)",      "collab"},
                    {"지속성 (Persistence)",        "persist"}
                };
                boolean alt = false;
                for (String[] row : rows) {
                    Color bg = alt ? new Color(0xf4, 0xf8, 0xff) : Color.WHITE;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) metricsMap.get(row[1]);
                    int score    = m != null ? (int) m.get("score") : 0;
                    String desc  = m != null ? String.valueOf(m.getOrDefault("desc", "")) : "";
                    addDataCell(table, row[0], bfRegular, 10, Color.BLACK,        bg);
                    addDataCell(table, score + "점", bfBold, 11, scoreColor(score), bg);
                    addDataCell(table, desc,   bfRegular,  9, new Color(0x44, 0x44, 0x44), bg);
                    alt = !alt;
                }
                document.add(table);
            }

            // ── Strengths ──────────────────────────────────────────────────────
            @SuppressWarnings("unchecked")
            List<String> strengths = (List<String>) model.get("strengths");
            if (strengths != null && !strengths.isEmpty()) {
                Paragraph strengthsTitle = new Paragraph("강점", fontH2);
                strengthsTitle.setSpacingAfter(6);
                document.add(strengthsTitle);
                for (String s : strengths) {
                    Paragraph p = new Paragraph("✓  " + s, fontBody);
                    p.setIndentationLeft(10);
                    p.setSpacingAfter(3);
                    document.add(p);
                }
                document.add(new Paragraph(" "));
            }

            // ── Improvements ──────────────────────────────────────────────────
            @SuppressWarnings("unchecked")
            List<String> improvements = (List<String>) model.get("improvements");
            if (improvements != null && !improvements.isEmpty()) {
                Paragraph improveTitle = new Paragraph("개선 제안", fontH2);
                improveTitle.setSpacingAfter(6);
                document.add(improveTitle);
                int idx = 1;
                for (String imp : improvements) {
                    Paragraph p = new Paragraph(idx++ + ".  " + imp, fontBody);
                    p.setIndentationLeft(10);
                    p.setSpacingAfter(3);
                    document.add(p);
                }
                document.add(new Paragraph(" "));
            }

            // ── Footer ─────────────────────────────────────────────────────────
            document.add(new Chunk(new LineSeparator(1, 100, new Color(0xcc, 0xcc, 0xcc),
                    Element.ALIGN_CENTER, -2)));
            Paragraph footer = new Paragraph(
                    "GitHub Activity Insight  |  Rule v" + model.getOrDefault("ruleVersion", "1.0")
                    + "  |  Request ID: " + model.getOrDefault("requestId", ""),
                    fontSmall);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(4);
            document.add(footer);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF 생성 실패: " + e.getMessage(), e);
        }
    }

    public String getFilename(String githubId, LocalDateTime createdAt) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        return String.format("report_%s_%s.pdf", githubId, createdAt.format(formatter));
    }

    private BaseFont loadBaseFont(String path) throws DocumentException, java.io.IOException {
        if (new File(path).exists()) {
            return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        }
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }

    private void addHeaderCell(PdfPTable table, String text, BaseFont bf) {
        Font f = new Font(bf, 10, Font.NORMAL, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, f));
        cell.setBackgroundColor(new Color(0x1f, 0x6f, 0xeb));
        cell.setPadding(6);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text, BaseFont bf, int size,
                              Color textColor, Color bgColor) {
        Font f = new Font(bf, size, Font.NORMAL, textColor);
        PdfPCell cell = new PdfPCell(new Phrase(text, f));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(5);
        cell.setBorderColor(new Color(0xe0, 0xe0, 0xe0));
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private Color scoreColor(int score) {
        if (score >= 70) return new Color(0x16, 0xa3, 0x4a);
        if (score >= 40) return new Color(0xd9, 0x7f, 0x06);
        return new Color(0xdc, 0x26, 0x26);
    }
}

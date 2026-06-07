package com.zsj.meetingagent.resume;

import com.zsj.meetingagent.common.exception.BusinessException;
import com.zsj.meetingagent.resume.parser.PdfResumeTextExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PDF 简历文本提取测试。
 * 验证文本型 PDF 能进入后续出题链路，同时对没有文字层的 PDF 给出明确的 OCR 提示。
 */
class PdfResumeTextExtractorTest {

    private final PdfResumeTextExtractor extractor = new PdfResumeTextExtractor();

    @Test
    void shouldExtractTextFromTextBasedPdf() throws Exception {
        byte[] pdfBytes = createPdf("Campus marketplace, Spring Boot, MySQL and Redis.");

        String result = extractor.extract(pdfBytes);

        assertThat(result)
                .contains("Campus marketplace")
                .contains("Spring Boot")
                .contains("Redis");
    }

    @Test
    void shouldRejectPdfWithoutTextLayer() throws Exception {
        byte[] pdfBytes = createBlankPdf();

        assertThatThrownBy(() -> extractor.extract(pdfBytes))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException exception = (BusinessException) error;
                    assertThat(exception.code()).isEqualTo("R0406");
                    assertThat(exception.getMessage()).contains("OCR");
                });
    }

    private byte[] createPdf(String text) throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(72, 720);
                contentStream.showText(text);
                contentStream.endText();
            }
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createBlankPdf() throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}

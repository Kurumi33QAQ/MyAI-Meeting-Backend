package com.zsj.meetingagent.resume.parser;

import com.zsj.meetingagent.common.exception.BusinessException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * PDF 简历文本提取器。
 * 使用 PDFBox 读取文本型 PDF；扫描件没有文字层时明确提示需要 OCR，避免用占位文本误导后续出题。
 */
@Component
public class PdfResumeTextExtractor {

    public String extract(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new BusinessException("R0401", "PDF 简历内容不能为空");
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = normalize(stripper.getText(document));
            if (!StringUtils.hasText(text)) {
                throw new BusinessException(
                        "R0406",
                        "未能从 PDF 中提取文字，请上传带文字层的 PDF；扫描版简历需要后续 OCR 能力支持"
                );
            }
            return text;
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException("R0405", "PDF 简历解析失败，请确认文件未损坏且未加密");
        }
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u0000', ' ')
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" +", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}

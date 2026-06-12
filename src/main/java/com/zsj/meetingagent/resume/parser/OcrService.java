package com.zsj.meetingagent.resume.parser;

/**
 * OCR 文本识别服务。
 * 文本型 PDF 仍走 PDFBox；只有扫描件没有文字层时才调用 OCR。
 */
public interface OcrService {

    String recognizePdf(byte[] pdfBytes);
}

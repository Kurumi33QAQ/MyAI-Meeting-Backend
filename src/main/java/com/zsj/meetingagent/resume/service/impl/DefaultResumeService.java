package com.zsj.meetingagent.resume.service.impl;

import com.zsj.meetingagent.common.exception.BusinessException;
import com.zsj.meetingagent.resume.dto.ResumeTextRequest;
import com.zsj.meetingagent.resume.entity.UploadedFile;
import com.zsj.meetingagent.resume.mapper.UploadedFileMapper;
import com.zsj.meetingagent.resume.parser.PdfResumeTextExtractor;
import com.zsj.meetingagent.resume.parser.ResumeTextParser;
import com.zsj.meetingagent.resume.service.ResumeService;
import com.zsj.meetingagent.resume.vo.ResumePreviewResponse;
import com.zsj.meetingagent.resume.vo.ResumeResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 默认简历服务实现。
 * 当前把简历正文直接存入 MySQL，后续 RAG 阶段会把正文切分为结构化 chunk 并建立向量索引。
 */
@Service
public class DefaultResumeService implements ResumeService {

    private static final int NOT_DELETED = 0;

    private final UploadedFileMapper uploadedFileMapper;
    private final ResumeTextParser resumeTextParser;
    private final PdfResumeTextExtractor pdfResumeTextExtractor;

    public DefaultResumeService(
            UploadedFileMapper uploadedFileMapper,
            ResumeTextParser resumeTextParser,
            PdfResumeTextExtractor pdfResumeTextExtractor
    ) {
        this.uploadedFileMapper = uploadedFileMapper;
        this.resumeTextParser = resumeTextParser;
        this.pdfResumeTextExtractor = pdfResumeTextExtractor;
    }

    @Override
    public ResumeResponse uploadText(String username, ResumeTextRequest request) {
        String fileName = StringUtils.hasText(request.fileName()) ? request.fileName().trim() : "resume.txt";
        byte[] bytes = request.content().getBytes(StandardCharsets.UTF_8);
        return saveResume(username, fileName, "text/plain", (long) bytes.length, bytes, request.content());
    }

    @Override
    public ResumeResponse uploadFile(String username, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("R0401", "简历文件不能为空");
        }
        try {
            byte[] bytes = file.getBytes();
            if (isPdfFile(file)) {
                /*
                 * PDF 原始字节用于前端预览，PDFBox 提取出的正文用于结构化 chunk、RAG 和面试出题。
                 * 扫描件没有文字层时会明确报错，避免把占位正文送给大模型。
                 */
                String content = pdfResumeTextExtractor.extract(bytes);
                return saveResume(username, file.getOriginalFilename(), file.getContentType(), file.getSize(), bytes, content);
            }
            /*
             * 当前文件上传能力定位为“文本简历上传”，按 UTF-8 读取正文。
             * PDF/Word 会交给后续专门的文档解析器处理，避免在简历主流程中混入复杂格式解析。
             */
            String content = new String(bytes, StandardCharsets.UTF_8);
            return saveResume(username, file.getOriginalFilename(), file.getContentType(), file.getSize(), bytes, content);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("R0501", "简历文件读取失败，请先上传文本格式简历");
        }
    }

    private boolean isPdfFile(MultipartFile file) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        return "application/pdf".equalsIgnoreCase(contentType)
                || (fileName != null && fileName.toLowerCase().endsWith(".pdf"));
    }

    @Override
    public ResumeResponse getResume(String username, String resumeId) {
        return uploadedFileMapper.findByFileIdAndUsername(resumeId, username)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException("R0404", "简历不存在或无权访问"));
    }

    @Override
    public ResumePreviewResponse getResumePreview(String username, String resumeId) {
        UploadedFile file = uploadedFileMapper.findByFileIdAndUsername(resumeId, username)
                .orElseThrow(() -> new BusinessException("R0404", "简历不存在或无权访问"));
        if (isPdfBytes(file.fileBytes())) {
            return new ResumePreviewResponse(pdfFileName(file.fileName()), "application/pdf", file.fileBytes());
        }
        /*
         * 历史数据或文本简历没有可直接预览的 PDF 字节时，生成一份合法 PDF 兜底。
         * 这能保证前端“查看简历”按钮可用，同时明确告诉用户这不是原始简历扫描件。
         */
        return new ResumePreviewResponse(
                pdfFileName(file.fileName()),
                "application/pdf",
                buildFallbackPdf(file)
        );
    }

    private ResumeResponse saveResume(String username, String fileName, String contentType, Long fileSize, byte[] fileBytes, String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("R0402", "简历内容不能为空");
        }
        Instant now = Instant.now();
        UploadedFile file = new UploadedFile(
                null,
                UUID.randomUUID().toString(),
                username,
                StringUtils.hasText(fileName) ? fileName : "resume.txt",
                StringUtils.hasText(contentType) ? contentType : "text/plain",
                fileSize == null ? 0L : fileSize,
                "RESUME",
                fileBytes == null ? new byte[0] : fileBytes,
                content.trim(),
                resumeTextParser.buildSummary(content),
                now,
                now,
                NOT_DELETED
        );
        uploadedFileMapper.insert(file);
        return toResponse(file);
    }

    private ResumeResponse toResponse(UploadedFile file) {
        return new ResumeResponse(
                file.fileId(),
                file.fileName(),
                file.contentType(),
                file.fileSize(),
                file.documentType(),
                file.summary(),
                file.createdAt(),
                file.updatedAt()
        );
    }

    private boolean isPdfBytes(byte[] bytes) {
        return bytes != null
                && bytes.length >= 5
                && bytes[0] == '%'
                && bytes[1] == 'P'
                && bytes[2] == 'D'
                && bytes[3] == 'F'
                && bytes[4] == '-';
    }

    private String pdfFileName(String fileName) {
        String normalized = StringUtils.hasText(fileName) ? fileName.trim() : "resume.pdf";
        if (normalized.toLowerCase().endsWith(".pdf")) {
            return normalized;
        }
        int dotIndex = normalized.lastIndexOf('.');
        String baseName = dotIndex > 0 ? normalized.substring(0, dotIndex) : normalized;
        return baseName + ".pdf";
    }

    private byte[] buildFallbackPdf(UploadedFile file) {
        String title = "MyAI Meeting Resume Preview";
        String note = "This PDF is generated by backend because original PDF bytes are not available.";
        String name = "File: " + ascii(file.fileName(), 90);
        String summary = "Summary: " + ascii(file.summary(), 100);
        String text = "Content: " + ascii(file.textContent(), 110);
        return buildSimplePdf(List.of(title, note, name, summary, text));
    }

    private String ascii(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "N/A";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        StringBuilder builder = new StringBuilder();
        normalized.codePoints()
                .limit(maxLength)
                .forEach(codePoint -> builder.append(codePoint >= 32 && codePoint <= 126 ? (char) codePoint : '?'));
        return builder.toString();
    }

    private byte[] buildSimplePdf(List<String> lines) {
        StringBuilder content = new StringBuilder("BT\n/F1 12 Tf\n72 760 Td\n");
        for (String line : lines) {
            content.append('(')
                    .append(escapePdfText(line))
                    .append(") Tj\n0 -22 Td\n");
        }
        content.append("ET\n");

        List<String> objects = new ArrayList<>();
        objects.add("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        objects.add("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        objects.add("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n");
        objects.add("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");
        objects.add("5 0 obj\n<< /Length " + content.toString().getBytes(StandardCharsets.US_ASCII).length + " >>\nstream\n" + content + "endstream\nendobj\n");

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (String object : objects) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.US_ASCII).length);
            pdf.append(object);
        }
        int xrefOffset = pdf.toString().getBytes(StandardCharsets.US_ASCII).length;
        pdf.append("xref\n0 ").append(objects.size() + 1).append('\n');
        pdf.append("0000000000 65535 f \n");
        for (Integer offset : offsets) {
            pdf.append(String.format("%010d 00000 n \n", offset));
        }
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n");
        pdf.append("startxref\n").append(xrefOffset).append("\n%%EOF\n");
        return pdf.toString().getBytes(Charset.forName("US-ASCII"));
    }

    private String escapePdfText(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}

package com.zsj.meetingagent.resume.service.impl;

import com.zsj.meetingagent.common.exception.BusinessException;
import com.zsj.meetingagent.resume.dto.ResumeTextRequest;
import com.zsj.meetingagent.resume.entity.UploadedFile;
import com.zsj.meetingagent.resume.mapper.UploadedFileMapper;
import com.zsj.meetingagent.resume.parser.ResumeTextParser;
import com.zsj.meetingagent.resume.service.ResumeService;
import com.zsj.meetingagent.resume.vo.ResumeResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
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

    public DefaultResumeService(UploadedFileMapper uploadedFileMapper, ResumeTextParser resumeTextParser) {
        this.uploadedFileMapper = uploadedFileMapper;
        this.resumeTextParser = resumeTextParser;
    }

    @Override
    public ResumeResponse uploadText(String username, ResumeTextRequest request) {
        String fileName = StringUtils.hasText(request.fileName()) ? request.fileName().trim() : "resume.txt";
        return saveResume(username, fileName, "text/plain", (long) request.content().getBytes(StandardCharsets.UTF_8).length, request.content());
    }

    @Override
    public ResumeResponse uploadFile(String username, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("R0401", "简历文件不能为空");
        }
        try {
            if (isPdfFile(file)) {
                /*
                 * 旧前端当前只允许上传 PDF。阶段 6 还没有引入 PDF 解析器，
                 * 因此这里保存一个可用于面试流程的占位正文，真实结构化解析放到 RAG 文档解析模块。
                 */
                String fallbackContent = "用户上传了一份 PDF 简历，文件名：" + file.getOriginalFilename()
                        + "。当前后端已记录文件元信息，详细 PDF 解析将在 RAG 文档解析能力中增强。";
                return saveResume(username, file.getOriginalFilename(), file.getContentType(), file.getSize(), fallbackContent);
            }
            /*
             * 当前文件上传能力定位为“文本简历上传”，按 UTF-8 读取正文。
             * PDF/Word 会交给后续专门的文档解析器处理，避免在简历主流程中混入复杂格式解析。
             */
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            return saveResume(username, file.getOriginalFilename(), file.getContentType(), file.getSize(), content);
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

    private ResumeResponse saveResume(String username, String fileName, String contentType, Long fileSize, String content) {
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
}

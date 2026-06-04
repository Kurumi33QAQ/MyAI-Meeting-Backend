package com.zsj.meetingagent.resume.vo;

import java.time.Instant;

/**
 * 简历响应对象。
 * 前端和面试模块只需要 resumeId、摘要和文件元信息，不直接暴露完整简历正文。
 */
public record ResumeResponse(
        String resumeId,
        String fileName,
        String contentType,
        Long fileSize,
        String documentType,
        String summary,
        Instant createdAt,
        Instant updatedAt
) {
}

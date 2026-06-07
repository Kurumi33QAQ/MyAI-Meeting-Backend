package com.zsj.meetingagent.resume.entity;

import java.time.Instant;

/**
 * 上传文件领域对象。
 * 当前用于保存简历文本和文件元信息，对应 MySQL uploaded_file 表。
 */
public record UploadedFile(
        Long id,
        String fileId,
        String username,
        String fileName,
        String contentType,
        Long fileSize,
        String documentType,
        byte[] fileBytes,
        String textContent,
        String summary,
        Instant createdAt,
        Instant updatedAt,
        int deleted
) {
}

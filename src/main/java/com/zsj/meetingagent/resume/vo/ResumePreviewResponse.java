package com.zsj.meetingagent.resume.vo;

/**
 * 简历预览响应对象。
 * 保存后端准备给浏览器预览的文件字节和展示用文件名。
 */
public record ResumePreviewResponse(
        String fileName,
        String contentType,
        byte[] bytes
) {
}

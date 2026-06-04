package com.zsj.meetingagent.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 文本简历录入请求。
 * 当前稳定支持直接录入简历正文，PDF/Word 等复杂解析会在 RAG 文档解析能力中增强。
 */
public record ResumeTextRequest(
        @Size(max = 120, message = "长度不能超过 120 个字符")
        String fileName,

        @NotBlank(message = "不能为空")
        @Size(max = 20000, message = "长度不能超过 20000 个字符")
        String content
) {
}

package com.zsj.meetingagent.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 证据检索请求。
 * query 是用户问题或业务意图，documentTypes 可限制只查简历、JD 或题库等特定来源。
 */
public record RetrieveEvidenceRequest(
        @NotBlank(message = "检索问题不能为空")
        @Size(max = 1000, message = "检索问题长度不能超过 1000 个字符")
        String query,

        List<String> documentTypes,

        Integer topK,

        Integer finalK
) {
}

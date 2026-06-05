package com.zsj.meetingagent.interview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建模拟面试会话请求。
 * resumeId 关联已上传简历，岗位名称和 JD 用于生成更贴近目标岗位的问题。
 */
public record CreateInterviewSessionRequest(
        @NotBlank(message = "不能为空")
        @Size(max = 80, message = "长度不能超过 80 个字符")
        String resumeId,

        @NotBlank(message = "不能为空")
        @Size(max = 120, message = "长度不能超过 120 个字符")
        String jobTitle,

        @Size(max = 120, message = "长度不能超过 120 个字符")
        String companyName,

        @Size(max = 5000, message = "长度不能超过 5000 个字符")
        String jobDescription,

        @Min(value = 1, message = "不能小于 1")
        @Max(value = 10, message = "不能大于 10")
        Integer questionCount
) {
}

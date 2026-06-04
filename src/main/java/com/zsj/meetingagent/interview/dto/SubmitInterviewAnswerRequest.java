package com.zsj.meetingagent.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 提交面试回答请求。
 * questionId 指定当前回答哪一题，answer 是用户的自然语言回答内容。
 */
public record SubmitInterviewAnswerRequest(
        @NotBlank(message = "不能为空")
        @Size(max = 80, message = "长度不能超过 80 个字符")
        String questionId,

        @NotBlank(message = "不能为空")
        @Size(max = 5000, message = "长度不能超过 5000 个字符")
        String answer
) {
}

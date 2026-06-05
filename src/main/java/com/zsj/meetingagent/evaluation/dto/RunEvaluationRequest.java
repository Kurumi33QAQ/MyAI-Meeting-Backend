package com.zsj.meetingagent.evaluation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 启动评测请求。
 * datasetPath 默认读取 classpath:evaluation/eval_cases.json，maxCases 可用于快速抽样验证。
 */
public record RunEvaluationRequest(
        @Size(max = 300, message = "测试集路径长度不能超过 300 个字符")
        String datasetPath,

        @Min(value = 1, message = "评测样本数量至少为 1")
        @Max(value = 200, message = "单次评测样本数量不能超过 200")
        Integer maxCases,

        Boolean writeReport
) {
}

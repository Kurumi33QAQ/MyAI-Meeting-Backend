package com.zsj.meetingagent.ai.controller;

import com.zsj.meetingagent.ai.service.AiModelService;
import com.zsj.meetingagent.ai.vo.AiModelOptionResponse;
import com.zsj.meetingagent.common.result.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 模型选项接口。
 * 先提供最小可用的模型列表，后续可扩展为数据库中的模型配置管理。
 */
@RestController
@RequestMapping("/api/ai")
public class AiModelController {

    private final AiModelService aiModelService;

    public AiModelController(AiModelService aiModelService) {
        this.aiModelService = aiModelService;
    }

    @GetMapping("/models")
    public ApiResponse<List<AiModelOptionResponse>> listModels() {
        return ApiResponse.success(aiModelService.listModels());
    }
}

package com.zsj.meetingagent.ai.service;

import com.zsj.meetingagent.ai.config.AiModelProperties;
import com.zsj.meetingagent.ai.vo.AiModelOptionResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 模型配置查询服务。
 * 当前只返回默认模型，后续可从 MySQL 的 ai_model_config 表读取多模型配置。
 */
@Service
@EnableConfigurationProperties(AiModelProperties.class)
public class AiModelService {

    private final AiModelProperties aiModelProperties;

    public AiModelService(AiModelProperties aiModelProperties) {
        this.aiModelProperties = aiModelProperties;
    }

    public List<AiModelOptionResponse> listModels() {
        return List.of(new AiModelOptionResponse(
                aiModelProperties.getDefaultModel(),
                "默认对话模型",
                aiModelProperties.getProvider(),
                aiModelProperties.isMockEnabled()
        ));
    }
}

package com.zsj.meetingagent.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 模型配置。
 * 这里保存本项目自己的模型调用默认值，避免业务代码直接读取零散环境变量。
 */
@ConfigurationProperties(prefix = "app.ai")
public class AiModelProperties {

    private String provider = "openai-compatible";

    private String defaultModel = "gpt-4o-mini";

    private double defaultTemperature = 0.7;

    private boolean mockEnabled = true;

    private String defaultSystemPrompt = "你是一个中文友好的 Java 后端学习助手，回答要清晰、准确，并优先解释设计原因。";

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public double getDefaultTemperature() {
        return defaultTemperature;
    }

    public void setDefaultTemperature(double defaultTemperature) {
        this.defaultTemperature = defaultTemperature;
    }

    public boolean isMockEnabled() {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }

    public String getDefaultSystemPrompt() {
        return defaultSystemPrompt;
    }

    public void setDefaultSystemPrompt(String defaultSystemPrompt) {
        this.defaultSystemPrompt = defaultSystemPrompt;
    }
}
